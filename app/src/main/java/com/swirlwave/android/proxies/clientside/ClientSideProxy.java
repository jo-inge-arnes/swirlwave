package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.proxies.SelectionKeyAttachment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ClientSideProxy implements Runnable {
    public static final int START_PORT = 9346;
    private Context mContext;
    private List<ServerSocketChannel> mServerSocketChannels = new ArrayList<>();
    private volatile boolean mRunning = true;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);

    public ClientSideProxy(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            bindPorts(selector);

            while (mRunning) {
                int numChannelsReady = selector.select();

                if (numChannelsReady == 0) {
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    try {
                        if (selectionKey.isAcceptable()) {
                            SocketChannel clientSocketChannel = acceptIncomingSocket(selectionKey);
                            String onionAddress = resolveOnionAddress(clientSocketChannel.socket().getPort());
                            SocketChannel localServerChannel = connectRemoteServer(selector, onionAddress);
                            localServerChannel.register(selector, SelectionKey.OP_CONNECT, clientSocketChannel);
                        } else if (selectionKey.isConnectable()) {
                            SocketChannel remoteServerChannel = (SocketChannel) selectionKey.channel();
                            if (remoteServerChannel.finishConnect()) {
                                SelectionKey localServerSelectionKey = selectionKey;
                                localServerSelectionKey.interestOps(SelectionKey.OP_READ);

                                SocketChannel incomingClientChannel = (SocketChannel) selectionKey.attachment();
                                SelectionKey incomingClientSelectionKey = incomingClientChannel.register(selector, SelectionKey.OP_READ);

                                SelectionKeyAttachment localServerSelectionKeyAttachment = new SelectionKeyAttachment(incomingClientChannel, incomingClientSelectionKey, true);
                                localServerSelectionKey.attach(localServerSelectionKeyAttachment);

                                SelectionKeyAttachment incomingClientSelectionKeyAttachment = new SelectionKeyAttachment(remoteServerChannel, localServerSelectionKey, false);
                                incomingClientSelectionKey.attach(incomingClientSelectionKeyAttachment);
                            }
                        } else if (selectionKey.isReadable()) {
                            SocketChannel inChannel = null;
                            SocketChannel outChannel = null;

                            try {
                                inChannel = (SocketChannel) selectionKey.channel();
                                SelectionKeyAttachment attachment = (SelectionKeyAttachment) selectionKey.attachment();
                                outChannel = attachment.getSocketChannel();

                                boolean ok = processInput(inChannel, outChannel, attachment);

                                if (!ok) {
                                    closeSocketPairs(selectionKey);
                                }
                            } catch (IOException ie) {
                                closeSocketPairs(selectionKey);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(mContext.getString(R.string.service_name), e.toString());
                    }

                    iterator.remove();
                }

                keys.clear();
            }
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }

        closeChannels();
    }

    private void bindPorts(Selector selector) throws IOException {
        // TODO: Bind to one port for each friend
        for (int i = 0; i < 10; i++) {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(START_PORT + i);
            serverSocket.bind(inetSocketAddress);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            mServerSocketChannels.add(serverSocketChannel);
        }
    }

    private void closeChannels() {
        for (ServerSocketChannel serverSocketChannel : mServerSocketChannels) {
            try {
                serverSocketChannel.close();
            } catch (Exception e) {
                Log.e(mContext.getString(R.string.service_name), e.toString());
            }
        }
    }

    private SocketChannel acceptIncomingSocket(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        Socket socket = serverSocketChannel.socket().accept();
        SocketChannel socketChannel = socket.getChannel();
        socketChannel.configureBlocking(false);
        return socketChannel;
    }

    private SocketChannel connectRemoteServer(Selector selector, String onionAddress) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        InetSocketAddress localServerAddress = new InetSocketAddress(onionAddress, 8080); // !!!!! Should be ServerSideProxy.PORT);
        socketChannel.connect(localServerAddress);
        return socketChannel;
    }

    private String resolveOnionAddress(int port) {
        // TODO: Really resolve the friend's onion address from its port
        return "192.168.10.176";
    }

    private void closeSocketPairs(SelectionKey selectionKey) {
        SocketChannel inChannel = (SocketChannel) selectionKey.channel();

        SelectionKeyAttachment selectionKeyAttachment = (SelectionKeyAttachment) selectionKey.attachment();
        SocketChannel outChannel = selectionKeyAttachment.getSocketChannel();

        selectionKey.attach(null);
        selectionKeyAttachment.getSelectionKey().attach(null);

        selectionKey.cancel();
        selectionKeyAttachment.getSelectionKey().cancel();

        closeChannel(inChannel);
        closeChannel(outChannel);
    }

    private void closeChannel(SocketChannel socketChannel) {
        Socket socket = socketChannel.socket();
        try {
            // Closing the socket, even if channel will be closed.
            // Reason: To close a little bit sooner. The cancel on the channel will not close the socket until the next select.
            socket.close();
        } catch (IOException ie) {
            Log.e(mContext.getString(R.string.service_name), "Error closing socket" + socket + ": " + ie);
        }

        try {
            socketChannel.close();
        } catch (IOException ie) {
            Log.e(mContext.getString(R.string.service_name), "Error closing channel: " + ie);
        }
    }

    private boolean processInput(SocketChannel inChannel, SocketChannel outChannel, SelectionKeyAttachment attachment) throws IOException {
        boolean isOk;

        mBuffer.clear();
        int bytesRead = inChannel.read(mBuffer);
        mBuffer.flip();

        // A value of -1 means that the socket has been closed by the peer.
        if (bytesRead == -1) {
            isOk = false;
        } else if (mBuffer.limit() > 0) {
            if (attachment.isClientChannel()) {
                isOk = processDataFromRemoteServer(mBuffer, outChannel, attachment);
            } else {
                isOk = processDataFromClient(mBuffer, outChannel, attachment);
            }
        } else {
            isOk = true;
        }

        return isOk;
    }

    private boolean processDataFromClient(ByteBuffer inBuffer, SocketChannel outChannel, SelectionKeyAttachment attachment) throws IOException {
        while(inBuffer.hasRemaining()) {
            outChannel.write(inBuffer);
        }

        return true;
    }

    private boolean processDataFromRemoteServer(ByteBuffer inBuffer, SocketChannel outChannel, SelectionKeyAttachment attachment) throws IOException {
        while(inBuffer.hasRemaining()) {
            outChannel.write(inBuffer);
        }

        return true;
    }

    public void terminate() {
        mRunning = false;
    }
}
