package com.swirlwave.android.serversideproxy;

import android.content.Context;
import android.text.Selection;
import android.util.Log;

import com.swirlwave.android.R;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

public class Server implements Runnable {
    public static final int PORT = 9345;
    public static final int LOCAL_SERVER_PORT = 8088;
    private Context mContext;
    private volatile boolean mRunning = true;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);

    public Server(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);

            ServerSocket serverSocket = serverSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(PORT);
            serverSocket.bind(inetSocketAddress);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

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
                            SocketChannel localServerChannel = connectLocalServer(selector);
                            localServerChannel.register(selector, SelectionKey.OP_CONNECT, clientSocketChannel);
                        } else if (selectionKey.isConnectable()) {
                            SocketChannel localServerChannel = (SocketChannel) selectionKey.channel();
                            if (localServerChannel.finishConnect()) {
                                SelectionKey localServerSelectionKey = selectionKey;
                                localServerSelectionKey.interestOps(SelectionKey.OP_READ);

                                SocketChannel incomingClientChannel = (SocketChannel) selectionKey.attachment();
                                SelectionKey incomingClientSelectionKey = incomingClientChannel.register(selector, SelectionKey.OP_READ);

                                localServerSelectionKey.attach(new SelectionKeyAttachment(incomingClientChannel, incomingClientSelectionKey));
                                incomingClientSelectionKey.attach(new SelectionKeyAttachment(localServerChannel, localServerSelectionKey));
                            }
                        } else if (selectionKey.isReadable()) {
                            SocketChannel inChannel = null;
                            SocketChannel outChannel = null;

                            try {
                                inChannel = (SocketChannel) selectionKey.channel();
                                outChannel = ((SelectionKeyAttachment) selectionKey.attachment()).getSocketChannel();

                                boolean ok = processInput(inChannel, outChannel);

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

            serverSocketChannel.close();
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }
    }

    private SocketChannel acceptIncomingSocket(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        Socket socket = serverSocketChannel.socket().accept();
        SocketChannel socketChannel = socket.getChannel();
        socketChannel.configureBlocking(false);
        return socketChannel;
    }

    private SocketChannel connectLocalServer(Selector selector) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        InetSocketAddress localServerAddress = new InetSocketAddress("127.0.0.1", LOCAL_SERVER_PORT);
        socketChannel.connect(localServerAddress);
        return socketChannel;
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

    private boolean processInput(SocketChannel inChannel, SocketChannel outChannel) throws IOException {
        mBuffer.clear();
        int bytesRead = inChannel.read(mBuffer);
        mBuffer.flip();

        // A value of -1 means that the socket has been closed by the peer.
        if (bytesRead == -1) {
            return false;
        }

        if (mBuffer.limit() > 0) {
            while(mBuffer.hasRemaining()) {
                outChannel.write(mBuffer);
            }
        }

        return true;
    }

    public void terminate() {
        mRunning = false;
    }
}
