package com.swirlwave.android.proxies.serverside;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ConnectionMessage;
import com.swirlwave.android.proxies.FriendOnlineStatusUpdater;
import com.swirlwave.android.proxies.SelectionKeyAttachment;
import com.swirlwave.android.toast.Toaster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class ServerSideProxy implements Runnable {
    public static final int PORT = 9345;
    public static final int LOCAL_SERVER_PORT = 8088;
    private static ServerSocketChannel sServerSocketChannel;
    private static final Random mRnd = new Random();
    private Context mContext;
    private volatile boolean mRunning = true;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);
    private static Selector sSelector;
    private static final ReentrantLock mSelectorLock = new ReentrantLock();

    public ServerSideProxy(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try {
            Selector selector = getServerSocketChannel();

            while (mRunning) {
                mSelectorLock.lock();
                mSelectorLock.unlock();
                int numChannelsReady = selector.select(500);

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
                            ConnectionMessageSelectionKeyAttachment connectionMessageSelectionKeyAttachment = new ConnectionMessageSelectionKeyAttachment();

                            boolean ok = sendRandomNumber(clientSocketChannel, connectionMessageSelectionKeyAttachment);

                            if (ok) {
                                register(clientSocketChannel, selector, SelectionKey.OP_READ, connectionMessageSelectionKeyAttachment);
                            } else {
                                closeChannel(clientSocketChannel);
                            }
                        } else if (selectionKey.isConnectable()) {
                            SocketChannel localServerChannel = (SocketChannel) selectionKey.channel();
                            if (localServerChannel.finishConnect()) {
                                SelectionKey localServerSelectionKey = selectionKey;
                                localServerSelectionKey.interestOps(SelectionKey.OP_READ);

                                SocketChannel incomingClientChannel = (SocketChannel) selectionKey.attachment();
                                SelectionKey incomingClientSelectionKey = register(incomingClientChannel, selector, SelectionKey.OP_READ, null);

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
                                } else if (attachmentObject instanceof ConnectionMessageSelectionKeyAttachment) {
                                    ConnectionMessageSelectionKeyAttachment connectionMessageSelectionKeyAttachment = (ConnectionMessageSelectionKeyAttachment) attachmentObject;

                                    boolean ok = processSystemMessage(inChannel, connectionMessageSelectionKeyAttachment);

                                    if (ok) {
                                        if (connectionMessageSelectionKeyAttachment.isCompleted()) {
                                            // Deregister reading (OP_READ) from client socket until local server is connected
                                            selectionKey.attach(null);
                                            selectionKey.cancel();

                                            ConnectionMessage message = sendSystemMessageResponse(inChannel, connectionMessageSelectionKeyAttachment);

                                            if (message != null) {
                                                switch (message.getMessageType()) {
                                                    case APPLICATION_LAYER_CONNECTION: {
                                                        SocketChannel localServerChannel = connectLocalServer(selector);
                                                        register(localServerChannel, selector, SelectionKey.OP_CONNECT, inChannel);
                                                        new Thread(new FriendOnlineStatusUpdater(mContext, message.getSenderId(), true)).start();
                                                    }
                                                    break;

                                                    case ADDRESS_ANNOUNCEMENT: {
                                                        closeChannel(inChannel);
                                                        String address = new String(message.getSystemMessage(), StandardCharsets.UTF_8);
                                                        new Thread(new FriendAddressUpdater(mContext, message.getSenderId(), address)).start();
                                                    }
                                                    break;

                                                    default: {
                                                        closeChannel(inChannel);
                                                    }
                                                    break;
                                                }
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
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }
    }

    private SelectionKey register(SelectableChannel channel, Selector selector, int op, Object attachment) throws ClosedChannelException {
        SelectionKey selectionKey = null;

        mSelectorLock.lock();
        try {
            selector.wakeup();

            if (attachment == null) {
                selectionKey = channel.register(selector, op);
            } else {
                selectionKey = channel.register(selector, op, attachment);
            }
        } finally {
            mSelectorLock.unlock();
        }

        return selectionKey;
    }

    @NonNull
    private synchronized Selector getServerSocketChannel() throws IOException {
        if (sServerSocketChannel == null || !sServerSocketChannel.isOpen() || sServerSocketChannel.socket().isClosed() || !sServerSocketChannel.socket().isBound()) {
            sSelector = Selector.open();
            sServerSocketChannel = ServerSocketChannel.open();
            sServerSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = sServerSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(PORT);
            serverSocket.bind(inetSocketAddress);
            register(sServerSocketChannel, sSelector, SelectionKey.OP_ACCEPT, null);
        }

        return sSelector;
    }

    private boolean sendRandomNumber(SocketChannel clientSocketChannel, ConnectionMessageSelectionKeyAttachment connectionMessageSelectionKeyAttachment) {
        byte[] randomBytes = new byte[4];
        mRnd.nextBytes(randomBytes);
        ByteBuffer buffer = ByteBuffer.wrap(randomBytes);

        try {
            connectionMessageSelectionKeyAttachment.setSentRandomNumber(ConnectionMessage.bytesToInt(randomBytes));

            while (buffer.hasRemaining()) {
                clientSocketChannel.write(buffer);
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean processSystemMessage(SocketChannel inChannel, ConnectionMessageSelectionKeyAttachment systemMessageAttachment) throws IOException {
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

    private ConnectionMessage sendSystemMessageResponse(SocketChannel clientSocketChannel, ConnectionMessageSelectionKeyAttachment connectionMessageSelectionKeyAttachment) {
        ConnectionMessage message;
        byte responseCode = (byte)0x0b;

        try {
            byte[] bytes = connectionMessageSelectionKeyAttachment.getByteArrayStream().toByteArray();
            UUID senderId = ConnectionMessage.extractSenderId(bytes);
            Peer sender = PeersDb.selectByUuid(mContext, senderId);

            message = ConnectionMessage.fromByteArray(bytes, sender.getPublicKey());

            if (message.getRandomNumber() == connectionMessageSelectionKeyAttachment.getSentRandomNumber()) {
                responseCode = (byte)0x0a;
            } else {
                message = null;
            }
        } catch (Exception e) {
            return null;
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[] {(byte)responseCode});
            int written = clientSocketChannel.write(buffer);
//            Toaster.show(mContext, "Wrote response code! " + (byte)responseCode);
        } catch (IOException ie) {
            return null;
        }

        return message;
    }

    private SocketChannel acceptIncomingSocket(SelectionKey selectionKey) throws IOException {
//        Toaster.show(mContext, "Incoming connection");

        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        Socket socket = serverSocketChannel.socket().accept();
        SocketChannel socketChannel = socket.getChannel();
        socketChannel.configureBlocking(false);
        return socketChannel;
    }

    private SocketChannel connectLocalServer(Selector selector) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        register(socketChannel, selector, SelectionKey.OP_CONNECT, null);
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
