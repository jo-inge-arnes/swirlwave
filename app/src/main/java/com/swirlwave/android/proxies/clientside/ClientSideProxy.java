package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.swirlwave.android.R;
import com.swirlwave.android.crypto.AsymmetricEncryption;
import com.swirlwave.android.proxies.ConnectionMessage;
import com.swirlwave.android.proxies.MessageType;
import com.swirlwave.android.proxies.SelectionKeyAttachment;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

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
import java.util.UUID;

public class ClientSideProxy implements Runnable {
    public static final int START_PORT = 9346;
    private Context mContext;
    private LocalSettings mLocalSettings;
    private List<ServerSocketChannel> mServerSocketChannels = new ArrayList<>();
    private volatile boolean mRunning = true;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);
    private String mPublicKeyString, mPrivateKeyString;

    public ClientSideProxy(Context context) throws Exception {
        mContext = context;
        mLocalSettings = new LocalSettings(mContext);
        Pair<String, String> keys = mLocalSettings.getAsymmetricKeys();
        mPublicKeyString = keys.first;
        mPrivateKeyString = keys.second;
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
                            SocketChannel onionProxyChannel = connectOnionProxy(selector);
                            onionProxyChannel.register(selector, SelectionKey.OP_CONNECT, clientSocketChannel);
                        } else if (selectionKey.isConnectable()) {
                            SocketChannel onionProxyChannel = (SocketChannel) selectionKey.channel();
                            if (onionProxyChannel.finishConnect()) {
                                SelectionKey onionProxySelectionKey = selectionKey;
                                onionProxySelectionKey.interestOps(SelectionKey.OP_READ);

                                SocketChannel incomingClientChannel = (SocketChannel) selectionKey.attachment();

                                int clientPort = incomingClientChannel.socket().getPort();

                                // TODO: Implement these methods property
                                String onionAddress = resolveOnionAddress(clientPort);
                                UUID destination = resolveDestination(clientPort);

                                boolean ok = performSocks4aConnectionRequest(onionProxyChannel, onionAddress);

                                if (ok) {
                                    OnionProxySelectionKeyAttachment onionProxySelectionKeyAttachment = new OnionProxySelectionKeyAttachment(incomingClientChannel);
                                    onionProxySelectionKeyAttachment.setMode(ClientProxyMode.AWAITING_ONIONPROXY_RESULT);
                                    onionProxySelectionKeyAttachment.setDestination(destination);
                                    onionProxySelectionKey.attach(onionProxySelectionKeyAttachment);
                                } else {
                                    incomingClientChannel.close();
                                }
                            }
                        } else if (selectionKey.isReadable()) {
                            try {
                                boolean ok = false;

                                SocketChannel inChannel = (SocketChannel) selectionKey.channel();
                                SelectionKeyAttachment attachment = (SelectionKeyAttachment) selectionKey.attachment();

                                if (attachment == null) {

                                } else if (attachment.acceptingPayload()) {
                                    SocketChannel outChannel = attachment.getSocketChannel();
                                    ok = processInput(inChannel, outChannel, attachment);
                                } else if (attachment instanceof OnionProxySelectionKeyAttachment) {
                                    OnionProxySelectionKeyAttachment onionProxySelectionKeyAttachment = (OnionProxySelectionKeyAttachment) attachment;
                                    ClientProxyMode mode = onionProxySelectionKeyAttachment.getMode();
                                    if (mode == ClientProxyMode.AWAITING_ONIONPROXY_RESULT) {
                                        ok = readSocks4aConnectionResponse(inChannel, onionProxySelectionKeyAttachment);
                                    } else if (mode == ClientProxyMode.AWAITING_SERVERPROXY_RANDOM_NUMBER) {
                                        ok = readRandomNumber(inChannel, onionProxySelectionKeyAttachment);
                                    } else if (mode == ClientProxyMode.AWAITING_SERVERPROXY_AUTHENTICATION_RESULT) {
                                        ok = readServerProxyAuthenticationResult(inChannel, onionProxySelectionKeyAttachment);

                                        // Start to read payload from client
                                        if (onionProxySelectionKeyAttachment.getMode() == ClientProxyMode.ACCEPTING_PAYLOAD) {
                                            SocketChannel clientChannel = onionProxySelectionKeyAttachment.getSocketChannel();
                                            SelectionKey clientSelectionKey = clientChannel.register(selector, SelectionKey.OP_READ);
                                            SelectionKeyAttachment clientSelectionKeyAttachment = new SelectionKeyAttachment(inChannel, selectionKey, false);
                                            clientSelectionKey.attach(clientSelectionKeyAttachment);
                                        }
                                    }
                                }

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

    private boolean performSocks4aConnectionRequest(SocketChannel onionProxyChannel, String onionAddress) {
        short remoteHiddenServicePort = (short)SwirlwaveOnionProxyManager.HIDDEN_SERVICE_PORT;

        mBuffer.clear();
        mBuffer.put((byte)0x04);
        mBuffer.put((byte)0x01);
        mBuffer.putShort(remoteHiddenServicePort);
        mBuffer.putInt(0x01);
        mBuffer.put((byte)0x00);
        mBuffer.put(onionAddress.getBytes());
        mBuffer.put((byte)0x00);
        mBuffer.flip();

        try {
            while (mBuffer.hasRemaining()) {
                onionProxyChannel.write(mBuffer);
            }
        } catch (IOException ie) {
            return false;
        }

        return true;
    }

    private boolean readSocks4aConnectionResponse(SocketChannel onionProxyChannel, OnionProxySelectionKeyAttachment onionProxySelectionKeyAttachment) throws IOException {
        boolean isOk;

        mBuffer.clear();
        int bytesRead = onionProxyChannel.read(mBuffer);
        mBuffer.flip();

        // A value of -1 means that the socket has been closed by the peer.
        if (bytesRead == -1) {
            isOk = false;
        } else if (mBuffer.limit() > 0) {
            int alreadyReceived = onionProxySelectionKeyAttachment.getOnionProxyResultBytesReceived();
            byte[] byteArray = onionProxySelectionKeyAttachment.getOnionProxyResult();

            isOk = true;
            while (mBuffer.hasRemaining()) {
                byte nextByte = mBuffer.get();

                if (alreadyReceived < byteArray.length) {
                    byteArray[alreadyReceived] = nextByte;
                    alreadyReceived++;
                    onionProxySelectionKeyAttachment.setOnionProxyResultBytesReceived(alreadyReceived);
                }
            }

            if (isOk && alreadyReceived == byteArray.length) {
                byte[] resultBytes = onionProxySelectionKeyAttachment.getOnionProxyResult();
                if (resultBytes[0] == (byte)0x00 && resultBytes[1] == (byte)0x5a) {
                    onionProxySelectionKeyAttachment.setMode(ClientProxyMode.AWAITING_SERVERPROXY_RANDOM_NUMBER);
                } else {
                    onionProxySelectionKeyAttachment.setMode(ClientProxyMode.INVALID_ONIONPROXY_RESULT);
                    isOk = false;
                }
            }
        } else {
            isOk = true;
        }

        return isOk;
    }

    private boolean readRandomNumber(SocketChannel onionProxyChannel, OnionProxySelectionKeyAttachment onionProxySelectionKeyAttachment) throws IOException {
        boolean isOk;

        mBuffer.clear();
        int bytesRead = onionProxyChannel.read(mBuffer);
        mBuffer.flip();

        // A value of -1 means that the socket has been closed by the peer.
        if (bytesRead == -1) {
            isOk = false;
        } else if (mBuffer.limit() > 0) {
            int alreadyReceived = onionProxySelectionKeyAttachment.getServerProxyRandomBytesReceived();
            byte[] byteArray = onionProxySelectionKeyAttachment.getServerProxyRandomBytes();

            isOk = true;
            while (mBuffer.hasRemaining()) {
                byte nextByte = mBuffer.get();

                if (alreadyReceived < byteArray.length) {
                    byteArray[alreadyReceived] = nextByte;
                    alreadyReceived++;
                    onionProxySelectionKeyAttachment.setServerProxyRandomBytesReceived(alreadyReceived);
                }
            }

            if (isOk && alreadyReceived == byteArray.length) {
                isOk = performServerProxyAuthenticationRequest(onionProxyChannel, onionProxySelectionKeyAttachment);
            }
        } else {
            isOk = true;
        }

        return isOk;
    }

    private boolean performServerProxyAuthenticationRequest(SocketChannel onionProxyChannel, OnionProxySelectionKeyAttachment onionProxySelectionKeyAttachment) {
        byte[] randomBytesFromServer = onionProxySelectionKeyAttachment.getServerProxyRandomBytes();
        UUID destination = onionProxySelectionKeyAttachment.getDestination();

        try {
            byte[] bytes = generateConnectionMessage(randomBytesFromServer, destination);

            if (bytes == null) {
                return false;
            }

            mBuffer.clear();
            mBuffer.putInt(bytes.length);
            mBuffer.put(bytes);
            mBuffer.flip();

            try {
                while (mBuffer.hasRemaining()) {
                    onionProxyChannel.write(mBuffer);
                }
            } catch (IOException ie) {
                return false;
            }

            onionProxySelectionKeyAttachment.setMode(ClientProxyMode.AWAITING_SERVERPROXY_AUTHENTICATION_RESULT);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] generateConnectionMessage(byte[] randomBytesFromServer, UUID destination) throws Exception {
        ConnectionMessage message = new ConnectionMessage();
        message.setSenderId(mLocalSettings.getUuid());
        message.setRandomNumber(randomBytesFromServer);
        message.setMessageType(MessageType.APPLICATION_LAYER_CONNECTION);
        message.setDestination(destination);
        message.setSystemMessage(new byte[] { (byte)0x01, (byte)0x02, (byte)0x03 });
        return message.toByteArray(mPrivateKeyString);
    }

    private boolean readServerProxyAuthenticationResult(SocketChannel onionProxyChannel, OnionProxySelectionKeyAttachment onionProxySelectionKeyAttachment) throws IOException {
        boolean isOk;

        mBuffer.clear();
        int bytesRead = onionProxyChannel.read(mBuffer);
        mBuffer.flip();

        // A value of -1 means that the socket has been closed by the peer.
        if (bytesRead == -1) {
            isOk = false;
        } else if (mBuffer.limit() > 0) {
            boolean responseCodeNotYetRead = true;

            isOk = true;
            while (mBuffer.hasRemaining()) {
                byte nextByte = mBuffer.get();

                if (responseCodeNotYetRead) {
                    responseCodeNotYetRead  = false;

                    if (nextByte == (byte)0x5A) {
                        onionProxySelectionKeyAttachment.setMode(ClientProxyMode.ACCEPTING_PAYLOAD);
                    } else {
                        onionProxySelectionKeyAttachment.setMode(ClientProxyMode.REFUSED_BY_SERVERPROXY);
                        isOk = false;
                    }
                }
            }
        } else {
            isOk = true;
        }

        return isOk;
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

    private String resolveOnionAddress(int port) {
        // TODO: Really resolve the friend's onion address from its port
        return "rp66ar5ev36cix5c.onion";
    }

    private UUID resolveDestination(int clientPort) {
        // TODO: Resolve capability from port, reverse of binding ports
        return UUID.fromString("60b9abad-0638-468d-92aa-f0bf83a10ab6");
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

    private SocketChannel connectOnionProxy(Selector selector) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        int socksPort = SwirlwaveOnionProxyManager.getsSocksPort();
        InetSocketAddress localOnionProxyAddress = new InetSocketAddress("127.0.0.1", socksPort);
        socketChannel.connect(localOnionProxyAddress);
        return socketChannel;
    }

    private void closeSocketPairs(SelectionKey selectionKey) {
        SocketChannel inChannel = (SocketChannel) selectionKey.channel();
        closeChannel(inChannel);

        SelectionKeyAttachment selectionKeyAttachment = (SelectionKeyAttachment) selectionKey.attachment();
        selectionKey.attach(null);
        selectionKey.cancel();

        if (selectionKeyAttachment != null) {
            SocketChannel outChannel = selectionKeyAttachment.getSocketChannel();
            SelectionKey outChannelSelectionKey = selectionKeyAttachment.getSelectionKey();

            if (outChannelSelectionKey != null) {
                outChannelSelectionKey.attach(null);
                outChannelSelectionKey.cancel();
            }

            if (outChannel != null) {
                closeChannel(outChannel);
            }
        }
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
                isOk = processDataFromOnionProxy(mBuffer, outChannel, attachment);
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

    private boolean processDataFromOnionProxy(ByteBuffer inBuffer, SocketChannel outChannel, SelectionKeyAttachment attachment) throws IOException {
        while(inBuffer.hasRemaining()) {
            outChannel.write(inBuffer);
        }

        return true;
    }

    public void terminate() {
        mRunning = false;
    }
}
