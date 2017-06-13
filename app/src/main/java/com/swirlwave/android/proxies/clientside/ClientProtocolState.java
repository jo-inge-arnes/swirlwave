package com.swirlwave.android.proxies.clientside;

import android.content.Context;

import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ConnectionMessage;
import com.swirlwave.android.proxies.MessageType;
import com.swirlwave.android.proxies.ProtocolState;
import com.swirlwave.android.proxies.SocketClosedException;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.sms.SmsSender;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class ClientProtocolState extends ProtocolState {
    private final ByteBuffer mOnionProxyResponseBuffer = ByteBuffer.allocate(8);
    private final ByteBuffer mRandomNumberBuffer = ByteBuffer.allocate(4);
    private final String mPrivateKeyString;
    private final LocalSettings mLocalSettings;
    private ClientProtocolStateCode mCurrentState;
    private ConnectionMessage mConnectionMessage;
    private byte[] mRandomBytesFromServer = new byte[4];
    private ByteBuffer mConnectionMessageBuffer;
    private final ByteBuffer mConnectionMessageResponseBuffer = ByteBuffer.allocate(1);
    private UUID mFriendId;

    public ClientProtocolState(Context context, Selector selector, SocketChannel clientSocketChannel, SocketChannel onionProxySocketChannel, String privateKeyString, LocalSettings localSettings, Peer friend) {
        super(context, selector, clientSocketChannel, onionProxySocketChannel);

        mPrivateKeyString = privateKeyString;
        mLocalSettings = localSettings;
        mCurrentState = ClientProtocolStateCode.CONNECT_ONION_PROXY;
        mFriendId = friend.getPeerId();
    }

    public void prepareOnionProxyConnectionRequest(SelectionKey selectionKey, short remoteHiddenServicePort) {
        Peer friend = PeersDb.selectByUuid(mContext, mFriendId);

        mServerDirectedWriteBuffer.clear();
        mServerDirectedWriteBuffer.put((byte) 0x04);
        mServerDirectedWriteBuffer.put((byte) 0x01);
        mServerDirectedWriteBuffer.putShort(remoteHiddenServicePort);
        mServerDirectedWriteBuffer.putInt(0x01);
        mServerDirectedWriteBuffer.put((byte) 0x00);
        mServerDirectedWriteBuffer.put(friend.getAddress().getBytes());
        mServerDirectedWriteBuffer.put((byte) 0x00);
        mServerDirectedWriteBuffer.flip();

        selectionKey.interestOps(SelectionKey.OP_WRITE);
        mCurrentState = ClientProtocolStateCode.WRITE_ONION_PROXY_CONNECTION_REQUEST;
    }

    @Override
    public void writeServer(SelectionKey selectionKey) throws IOException, SocketClosedException {
        switch (mCurrentState) {
            case WRITE_ONION_PROXY_CONNECTION_REQUEST:
                writeOnionProxyConnectionRequest(selectionKey);
                break;
            case WRITE_CONNECTION_MESSAGE_TO_SERVER:
                writeConnectionMessage(selectionKey);
                break;
            case PROXYING:
                writeBufferToServerDirection();
                break;
            default:
                break;
        }
    }

    @Override
    public void readServer(SelectionKey selectionKey) throws Exception {
        switch (mCurrentState) {
            case READ_ONION_PROXY_CONNECTION_REQUEST_RESPONSE:
                readOnionProxyConnectionRequestResponse();
                break;
            case READ_RANDOM_NUMBER_FROM_SERVER:
                readRandomNumberFromServer(selectionKey);
                break;
            case READ_CONNECTION_MESSAGE_RESPONSE:
                readConnectionMessageResponse(selectionKey);
                break;
            case PROXYING:
                readFromServerDirectionPrepareClientWrite();
                break;
            default:
                break;
        }
    }

    @Override
    public void writeClient(SelectionKey selectionKey) throws IOException, SocketClosedException {
        switch (mCurrentState) {
            case PROXYING:
                writeBufferToClient();
                break;
            default:
                break;
        }
    }

    @Override
    public void readClient(SelectionKey selectionKey) throws Exception {
        switch (mCurrentState) {
            case PROXYING:
                readClientPrepareServerDirectedWrite();
                break;
            default:
                break;
        }
    }

    private void writeOnionProxyConnectionRequest(SelectionKey selectionKey) throws IOException {
        mServerDirectedSocketChannel.write(mServerDirectedWriteBuffer);

        if (!mServerDirectedWriteBuffer.hasRemaining()) {
            mServerDirectedWriteBuffer.clear();
            selectionKey.interestOps(SelectionKey.OP_READ);
            mCurrentState = ClientProtocolStateCode.READ_ONION_PROXY_CONNECTION_REQUEST_RESPONSE;
        }
    }

    private void readOnionProxyConnectionRequestResponse() throws Exception {
        read(mServerDirectedSocketChannel, mOnionProxyResponseBuffer);

        if (mOnionProxyResponseBuffer.position() == 8) {
            mOnionProxyResponseBuffer.flip();

            byte firstByte = mOnionProxyResponseBuffer.get();
            byte secondByte = mOnionProxyResponseBuffer.get();

            Peer friend = PeersDb.selectByUuid(mContext, mFriendId);

            if (firstByte == (byte)0x00 && secondByte == (byte)0x5a) {
                if (!friend.getOnlineStatus()) {
                    PeersDb.updateOnlineStatus(mContext, friend, true);
                }

                mCurrentState = ClientProtocolStateCode.READ_RANDOM_NUMBER_FROM_SERVER;
            } else {
                mCurrentState = ClientProtocolStateCode.REJECTED_BY_SERVER;
                PeersDb.updateOnlineStatus(mContext, friend, false);
                new Thread(new SmsSender(mContext, friend.getSecondaryChannelAddress(), mLocalSettings.getAddress())).start();
                throw new Exception(String.format("Onion proxy connection request rejected. Response: 0x%02X 0x%02X", firstByte, secondByte));
            }
        }
    }

    private void readRandomNumberFromServer(SelectionKey selectionKey) throws Exception {
        read(mServerDirectedSocketChannel, mRandomNumberBuffer);

        if (mRandomNumberBuffer.position() == 4) {
            mRandomNumberBuffer.flip();
            mRandomNumberBuffer.get(mRandomBytesFromServer);

            prepareConnectionMessage();

            selectionKey.interestOps(SelectionKey.OP_WRITE);
            mCurrentState = ClientProtocolStateCode.WRITE_CONNECTION_MESSAGE_TO_SERVER;
        }
    }

    private void prepareConnectionMessage() throws Exception {
        mConnectionMessage = new ConnectionMessage();

        mConnectionMessage.setSenderId(mLocalSettings.getUuid());
        mConnectionMessage.setRandomNumber(mRandomBytesFromServer);
        mConnectionMessage.setMessageType(MessageType.APPLICATION_LAYER_CONNECTION);
        mConnectionMessage.setDestination(UUID.randomUUID()); // Here the destination "capability" should be set
        mConnectionMessage.setSystemMessage(mLocalSettings.getAddress().getBytes("UTF-8"));

        byte[] connectionMessageBytes = mConnectionMessage.toByteArray(mPrivateKeyString);
        int connectionMessageBytesLength = connectionMessageBytes.length;

        mConnectionMessageBuffer = ByteBuffer.allocate(4 + connectionMessageBytesLength);
        mConnectionMessageBuffer.putInt(connectionMessageBytesLength);
        mConnectionMessageBuffer.put(connectionMessageBytes);
        mConnectionMessageBuffer.flip();
    }

    private void writeConnectionMessage(SelectionKey selectionKey) throws IOException {
        mServerDirectedSocketChannel.write(mConnectionMessageBuffer);

        if (!mConnectionMessageBuffer.hasRemaining()) {
            selectionKey.interestOps(SelectionKey.OP_READ);
            mCurrentState = ClientProtocolStateCode.READ_CONNECTION_MESSAGE_RESPONSE;
        }
    }

    private void readConnectionMessageResponse(SelectionKey selectionKey) throws Exception {
        read(mServerDirectedSocketChannel, mConnectionMessageResponseBuffer);

        if (mConnectionMessageResponseBuffer.position() == 1) {
            mConnectionMessageResponseBuffer.flip();

            byte responseCode = mConnectionMessageResponseBuffer.get();

            if (responseCode == CONNECTION_MESSAGE_ACCEPTED) {
                registerClientReadSelector();
                selectionKey.interestOps(SelectionKey.OP_READ);
                mCurrentState = ClientProtocolStateCode.PROXYING;
            } else {
                throw new Exception("Connection message was rejected!");
            }
        }
    }
}