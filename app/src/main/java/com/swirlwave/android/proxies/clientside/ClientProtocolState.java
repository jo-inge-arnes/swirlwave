package com.swirlwave.android.proxies.clientside;

import com.swirlwave.android.proxies.ProtocolState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ClientProtocolState extends ProtocolState {
    private final ByteBuffer mOnionProxyResponseBuffer = ByteBuffer.allocate(8);
    private ClientProtocolStateCode mCurrentState;

    public ClientProtocolState(Selector selector, SocketChannel clientSocketChannel, SocketChannel onionProxySocketChannel) {
        super(selector, clientSocketChannel, onionProxySocketChannel);
        mCurrentState = ClientProtocolStateCode.CONNECT_ONION_PROXY;
    }

    public void prepareOnionProxyConnectionRequest(SelectionKey selectionKey, String onionAddress, short remoteHiddenServicePort) {
        onionAddress = "srgdo4ugkaay6ayq.onion";

        mServerDirectedWriteBuffer.clear();
        mServerDirectedWriteBuffer.put((byte) 0x04);
        mServerDirectedWriteBuffer.put((byte) 0x01);
        mServerDirectedWriteBuffer.putShort(remoteHiddenServicePort);
        mServerDirectedWriteBuffer.putInt(0x01);
        mServerDirectedWriteBuffer.put((byte) 0x00);
        mServerDirectedWriteBuffer.put(onionAddress.getBytes());
        mServerDirectedWriteBuffer.put((byte) 0x00);
        mServerDirectedWriteBuffer.flip();

        selectionKey.interestOps(SelectionKey.OP_WRITE);
        mCurrentState = ClientProtocolStateCode.WRITE_ONION_PROXY_CONNECTION_REQUEST;
    }

    @Override
    public void writeServer(SelectionKey selectionKey) throws IOException {
        switch (mCurrentState) {
            case WRITE_ONION_PROXY_CONNECTION_REQUEST:
                writeOnionProxyConnectionRequest(selectionKey);
                break;
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
            case READ_ONION_PROXY_CONNECTION_REQUEST_RESPONSE:
                readOnionProxyConnectionRequestResponse();
                break;
            case PROXYING:
                readOnionPrepareClientWrite();
                break;
            default:
                break;
        }
    }

    @Override
    public void writeClient(SelectionKey selectionKey) throws IOException {
        switch (mCurrentState) {
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

            if (firstByte == (byte)0x00 && secondByte == (byte)0x5a) {
                registerClientReadSelector();
                mCurrentState = ClientProtocolStateCode.PROXYING;
            } else {
                mCurrentState = ClientProtocolStateCode.ONION_PROXY_CONNECTION_REQUEST_REJECTED;
                throw new Exception(String.format("Onion proxy connection request rejected. Response: 0x%02X 0x%02X", firstByte, secondByte));
            }
        }
    }
}
