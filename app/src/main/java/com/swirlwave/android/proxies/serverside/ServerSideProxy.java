package com.swirlwave.android.proxies.serverside;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.proxies.SelectionKeyAttachment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class ServerSideProxy implements Runnable {
    public static final int PORT = 9345;
    public static final int LOCAL_SERVER_PORT = 8088;
    private static final Random mRnd = new Random();
    private Context mContext;
    private volatile boolean mRunning = true;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);

    public ServerSideProxy(Context context) {
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

                            boolean ok = sendRandomNumber(clientSocketChannel);

                            if (ok) {
                                clientSocketChannel.register(selector, SelectionKey.OP_READ, new SystemMessageSelectionKeyAttachment());
                            } else {
                                closeChannel(clientSocketChannel);
                            }
                        } else if (selectionKey.isConnectable()) {
                            SocketChannel localServerChannel = (SocketChannel) selectionKey.channel();
                            if (localServerChannel.finishConnect()) {
                                SelectionKey localServerSelectionKey = selectionKey;
                                localServerSelectionKey.interestOps(SelectionKey.OP_READ);

                                SocketChannel incomingClientChannel = (SocketChannel) selectionKey.attachment();
                                SelectionKey incomingClientSelectionKey = incomingClientChannel.register(selector, SelectionKey.OP_READ);

                                SelectionKeyAttachment localServerSelectionKeyAttachment = new SelectionKeyAttachment(incomingClientChannel, incomingClientSelectionKey, true);
                                localServerSelectionKey.attach(localServerSelectionKeyAttachment);

                                SelectionKeyAttachment incomingClientSelectionKeyAttachment = new SelectionKeyAttachment(localServerChannel, localServerSelectionKey, false);
                                incomingClientSelectionKey.attach(incomingClientSelectionKeyAttachment);
                            }
                        } else if (selectionKey.isReadable()) {
                            SocketChannel inChannel = null;
                            SocketChannel outChannel = null;

                            try {
                                inChannel = (SocketChannel) selectionKey.channel();

                                Object attachmentObject = selectionKey.attachment();

                                if (attachmentObject instanceof SelectionKeyAttachment) {
                                    SelectionKeyAttachment attachment = (SelectionKeyAttachment) attachmentObject;
                                    outChannel = attachment.getSocketChannel();

                                    boolean ok = processInput(inChannel, outChannel, attachment);

                                    if (!ok) {
                                        closeSocketPairs(selectionKey);
                                    }
                                } else if (attachmentObject instanceof SystemMessageSelectionKeyAttachment) {
                                    SystemMessageSelectionKeyAttachment systemMessageSelectionKeyAttachment = (SystemMessageSelectionKeyAttachment) attachmentObject;

                                    boolean ok = processSystemMessage(inChannel, systemMessageSelectionKeyAttachment);

                                    if (ok) {
                                        if (systemMessageSelectionKeyAttachment.isCompleted()) {
                                            // Deregister reading (OP_READ) from client socket until local server is connected
                                            selectionKey.attach(null);
                                            selectionKey.cancel();

                                            ok = sendSystemMessageResponse(inChannel, systemMessageSelectionKeyAttachment);

                                            if (ok) {
                                                SocketChannel localServerChannel = connectLocalServer(selector);
                                                localServerChannel.register(selector, SelectionKey.OP_CONNECT, inChannel);
                                            } else {
                                                closeChannel(inChannel);
                                            }
                                        }
                                    } else {
                                        closeSocketPairs(selectionKey);
                                    }
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

    private boolean sendRandomNumber(SocketChannel clientSocketChannel) {
        byte[] randomBytes = new byte[4];
        mRnd.nextBytes(randomBytes);
        ByteBuffer buffer = ByteBuffer.wrap(randomBytes);

        try {
            while (buffer.hasRemaining()) {
                clientSocketChannel.write(buffer);
            }
        } catch (IOException ie) {
            return false;
        }

        return true;
    }

    private boolean processSystemMessage(SocketChannel inChannel, SystemMessageSelectionKeyAttachment systemMessageAttachment) throws IOException {
        boolean isOk;

        mBuffer.clear();
        int bytesRead = inChannel.read(mBuffer);
        mBuffer.flip();

        // A value of -1 means that the socket has been closed by the peer.
        if (bytesRead == -1) {
            isOk = false;
        } else if (mBuffer.limit() > 0) {
            isOk = true;

            int systemMessageBytesRead = systemMessageAttachment.getBytesRead();

            while (mBuffer.hasRemaining()) {
                byte nextByte = mBuffer.get();

                if (systemMessageAttachment.notCompleted()) {
                    systemMessageBytesRead++;
                    systemMessageAttachment.getByteArrayStream().write(nextByte);

                    if (systemMessageBytesRead == 4) {
                        byte[] longBytes = systemMessageAttachment.getByteArrayStream().toByteArray();

                        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(longBytes);
                        DataInputStream dataInputStream = new DataInputStream(byteInputStream);
                        int messageLength = dataInputStream.readInt();
                        dataInputStream.close();

                        systemMessageAttachment.setMessageLength(messageLength);
                        systemMessageAttachment.setByteArrayStream(new ByteArrayOutputStream());
                    } else if (systemMessageBytesRead > 4 && systemMessageBytesRead - 4 == systemMessageAttachment.getMessageLength()) {
                        systemMessageAttachment.setCompletedStatus(true);
                    }

                    systemMessageAttachment.setBytesRead(systemMessageBytesRead);
                }
            }
        } else {
            isOk = true;
        }

        return isOk;
    }

    private boolean sendSystemMessageResponse(SocketChannel clientSocketChannel, SystemMessageSelectionKeyAttachment systemMessageSelectionKeyAttachment) {
        byte[] responseCode = { (byte)0x5A };
        ByteBuffer buffer = ByteBuffer.wrap(responseCode);

        try {
            while (buffer.hasRemaining()) {
                clientSocketChannel.write(buffer);
            }
        } catch (IOException ie) {
            return false;
        }

        return true;
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

        Object attachmentObject = selectionKey.attachment();
        if (attachmentObject instanceof  SelectionKeyAttachment) {
            SelectionKeyAttachment selectionKeyAttachment = (SelectionKeyAttachment) attachmentObject;
            SocketChannel outChannel = selectionKeyAttachment.getSocketChannel();
            selectionKeyAttachment.getSelectionKey().attach(null);
            selectionKeyAttachment.getSelectionKey().cancel();
            closeChannel(outChannel);
        }

        selectionKey.attach(null);
        selectionKey.cancel();
        closeChannel(inChannel);
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
                isOk = processDataFromLocalServer(mBuffer, outChannel, attachment);
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

    private boolean processDataFromLocalServer(ByteBuffer inBuffer, SocketChannel outChannel, SelectionKeyAttachment attachment) throws IOException {
        while(inBuffer.hasRemaining()) {
            outChannel.write(inBuffer);
        }

        return true;
    }

    public void terminate() {
        mRunning = false;
    }
}
