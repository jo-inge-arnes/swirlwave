package com.swirlwave.android.proxies.serverside;

import android.content.Context;

import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ChannelAttachment;
import com.swirlwave.android.proxies.ChannelDirection;
import com.swirlwave.android.proxies.ConnectionMessage;
import com.swirlwave.android.proxies.MessageType;
import com.swirlwave.android.proxies.ProtocolState;
import com.swirlwave.android.proxies.ProxyBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class ServerProtocolState extends ProtocolState {
    public static final int LOCAL_SERVER_PORT = 8088;
    private static final Random sRandom = new Random();
    public static final byte CONNECTION_MESSAGE_ACCEPTED = (byte) 0x0a;
    public static final byte CONNECTION_MESSAGE_REJECTED = (byte) 0x0B;
    private final Context mContext;
    private ServerProtocolStateCode mCurrentState;
    private final byte[] mRandomNumberBytes = new byte[4];
    private ByteBuffer mRandomNumberBuffer;
    private final ByteBuffer mConnectionMessageLengthBuffer = ByteBuffer.allocate(4);
    private int mConnectionMessageLength;
    private final ByteArrayOutputStream mConnectionMessageStream = new ByteArrayOutputStream();
    private UUID mFriendId;
    private ConnectionMessage mConnectionMessage;
    private ByteBuffer mConnectionMessageResponseBuffer = ByteBuffer.allocate(1);
    private byte mResponseCode = CONNECTION_MESSAGE_REJECTED;

    public ServerProtocolState(Context context, Selector selector, SocketChannel clientSocketChannel) {
        super(selector, clientSocketChannel, null);

        mContext = context;

        generateRandomNumber();

        mCurrentState = ServerProtocolStateCode.WRITE_RANDOM_NUMBER_TO_CLIENT;
    }

    public void serverConnectFinished(SelectionKey serverSelectionKey) throws Exception {
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.FROM_CLIENT, this);
        mClientSocketChannel.register(mSelector, SelectionKey.OP_READ, attachment);
        serverSelectionKey.interestOps(serverSelectionKey.interestOps() | SelectionKey.OP_READ);
    }

    @Override
    public void writeServer(SelectionKey selectionKey) throws IOException {
        switch (mCurrentState) {
            case PROXYING:
                writeBufferToServerDirection(selectionKey);
                break;
            default:
                break;
        }
    }

    @Override
    public void readServer(SelectionKey selectionKey) throws Exception {
        switch (mCurrentState) {
            case PROXYING:
                readOnionPrepareClientWrite();
                break;
            default:
                break;
        }
    }

    @Override
    public void writeClient(SelectionKey selectionKey) throws Exception {
        switch (mCurrentState) {
            case WRITE_RANDOM_NUMBER_TO_CLIENT:
                writeRandomNumberToClient(selectionKey);
                break;
            case WRITE_CONNECTION_MESSAGE_RESPONSE:
                writeConnectionMessageResponse();
                break;
            case PROXYING:
                writeBufferToClient(selectionKey);
                break;
            default:
                break;
        }
    }

    @Override
    public void readClient(SelectionKey selectionKey) throws Exception {
        switch (mCurrentState) {
            case READ_CONNECTION_MESSAGE_LENGTH:
                readConnectionMessageLength();
                break;
            case READ_CONNECTION_MESSAGE:
                readConnectionMessage(selectionKey);
                break;
            case PROXYING:
                readClientPrepareServerDirectedWrite();
                break;
            default:
                break;
        }
    }

    private void generateRandomNumber() {
        sRandom.nextBytes(mRandomNumberBytes);
        mRandomNumberBuffer = ByteBuffer.wrap(mRandomNumberBytes);
    }

    private void writeRandomNumberToClient(SelectionKey selectionKey) throws IOException {
        mClientSocketChannel.write(mRandomNumberBuffer);

        if (!mRandomNumberBuffer.hasRemaining()) {
            selectionKey.interestOps(SelectionKey.OP_READ);
            mCurrentState = ServerProtocolStateCode.READ_CONNECTION_MESSAGE_LENGTH;
        }
    }

    private void readConnectionMessageLength() throws Exception {
        read(mClientSocketChannel, mConnectionMessageLengthBuffer);

        if (!mConnectionMessageLengthBuffer.hasRemaining()) {
            mConnectionMessageLengthBuffer.flip();
            mConnectionMessageLength = mConnectionMessageLengthBuffer.getInt();
            mCurrentState = ServerProtocolStateCode.READ_CONNECTION_MESSAGE;
        }
    }

    private void readConnectionMessage(SelectionKey selectionKey) throws Exception {
        read(mClientSocketChannel, mServerDirectedWriteBuffer);

        if (mServerDirectedWriteBuffer.position() > 0) {
            mServerDirectedWriteBuffer.flip();

            mConnectionMessageStream.write(mServerDirectedWriteBuffer.array(), 0, mServerDirectedWriteBuffer.limit());

            mServerDirectedWriteBuffer.clear();

            // TODO: Sanity check length to prevent it from being unrealistically long.
            // TODO: Think about timeouts for socket, in case the client uses way too long to send bytes.

            if (mConnectionMessageStream.size() >= mConnectionMessageLength) {
                byte[] connectionMessageBytes = Arrays.copyOfRange(mConnectionMessageStream.toByteArray(), 0, mConnectionMessageLength);

                mFriendId = ConnectionMessage.extractSenderId(connectionMessageBytes);
                Peer friend = PeersDb.selectByUuid(mContext, mFriendId);
                mConnectionMessage = ConnectionMessage.fromByteArray(connectionMessageBytes, friend.getPublicKey());

                if (mConnectionMessage.getRandomNumber() == toInt(mRandomNumberBytes)) {
                    mResponseCode = CONNECTION_MESSAGE_ACCEPTED;
                }

                mConnectionMessageResponseBuffer.put(mResponseCode);
                mConnectionMessageResponseBuffer.flip();

                selectionKey.interestOps(SelectionKey.OP_WRITE);
                mCurrentState = ServerProtocolStateCode.WRITE_CONNECTION_MESSAGE_RESPONSE;
            }
        }
    }

    private void writeConnectionMessageResponse() throws Exception {
        mClientSocketChannel.write(mConnectionMessageResponseBuffer);

        if (!mConnectionMessageResponseBuffer.hasRemaining()) {
            if (mResponseCode == CONNECTION_MESSAGE_ACCEPTED) {
                processConnectionMessage();
            } else {
                throw new Exception("Connection message was refused!");
            }
        }
    }

    private void processConnectionMessage() throws Exception {
        switch (mConnectionMessage.getMessageType()) {
            case ADDRESS_ANNOUNCEMENT:
                processAddressChangeMessage();
                break;
            case APPLICATION_LAYER_CONNECTION:
                connectLocalServer();
                break;
            default:
                throw new Exception("Connection message has unknown type!");
        }
    }

    private void processAddressChangeMessage() {
        String address = new String(mConnectionMessage.getSystemMessage(), StandardCharsets.UTF_8);
        new Thread(new FriendAddressUpdater(mContext, mConnectionMessage.getSenderId(), address)).start();
    }

    private void connectLocalServer() throws IOException {
        mServerDirectedSocketChannel = SocketChannel.open();
        mServerDirectedSocketChannel.configureBlocking(false);

        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.TOWARDS_SERVER, this);
        mServerDirectedSocketChannel.register(mSelector, SelectionKey.OP_CONNECT, attachment);

        mServerDirectedSocketChannel.connect(getLocalServerAddress());
    }

    public void finishServerConnect(SelectionKey selectionKey) throws Exception {
        SocketChannel serverChannel = (SocketChannel) selectionKey.channel();

        if (serverChannel.finishConnect()) {
            serverConnectFinished(selectionKey);
        } else {
            throw new Exception("Could not finish connect to local server!");
        }
    }

    private InetSocketAddress getLocalServerAddress() {
        return new InetSocketAddress(ProxyBase.LOCALHOST, LOCAL_SERVER_PORT);
    }

    private int toInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
}
