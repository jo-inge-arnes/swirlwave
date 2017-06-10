package com.swirlwave.android.proxies.clientside;

import com.swirlwave.android.proxies.ChannelAttachment;
import com.swirlwave.android.proxies.ChannelDirection;
import com.swirlwave.android.proxies.ProtocolState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ClientProtocolState extends ProtocolState {
    private final ByteBuffer mOnionProxyResponseBuffer = ByteBuffer.allocate(8);
    private final ByteBuffer mOnionWriteBuffer = ByteBuffer.allocate(16384);
    private final ByteBuffer mClientWriteBuffer = ByteBuffer.allocate(16384);
    private ClientProtocolStateCode mCurrentState;

    public ClientProtocolState(Selector selector, SocketChannel clientSocketChannel, SocketChannel onionProxySocketChannel) {
        super(selector, clientSocketChannel, onionProxySocketChannel);
        mCurrentState = ClientProtocolStateCode.CONNECT_ONION_PROXY;
    }

    public void prepareOnionProxyConnectionRequest(SelectionKey selectionKey, String onionAddress, short remoteHiddenServicePort) {
        onionAddress = "wrzlx26y6u3vrx2y.onion";
        remoteHiddenServicePort = 8088;

        mOnionWriteBuffer.clear();
        mOnionWriteBuffer.put((byte) 0x04);
        mOnionWriteBuffer.put((byte) 0x01);
        mOnionWriteBuffer.putShort(remoteHiddenServicePort);
        mOnionWriteBuffer.putInt(0x01);
        mOnionWriteBuffer.put((byte) 0x00);
        mOnionWriteBuffer.put(onionAddress.getBytes());
        mOnionWriteBuffer.put((byte) 0x00);
        mOnionWriteBuffer.flip();

        selectionKey.interestOps(SelectionKey.OP_WRITE);
        mCurrentState = ClientProtocolStateCode.WRITE_ONION_PROXY_CONNECTION_REQUEST;
    }

    public void writeServer(SelectionKey selectionKey) throws IOException {
        switch (mCurrentState) {
            case WRITE_ONION_PROXY_CONNECTION_REQUEST:
                writeOnionProxyConnectionRequest(selectionKey);
                break;
            case PROXYING:
                writeBufferToOnionProxy(selectionKey);
                break;
            default:
                break;
        }
    }

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

    public void writeClient(SelectionKey selectionKey) throws IOException {
        switch (mCurrentState) {
            case PROXYING:
                writeBufferToClient(selectionKey);
                break;
            default:
                break;
        }
    }

    public void readClient(SelectionKey selectionKey) throws Exception {
        switch (mCurrentState) {
            case PROXYING:
                readClientPrepareOnionWrite();
                break;
            default:
                break;
        }
    }

    private void writeOnionProxyConnectionRequest(SelectionKey selectionKey) throws IOException {
        mServerDirectedSocketChannel.write(mOnionWriteBuffer);

        if (!mOnionWriteBuffer.hasRemaining()) {
            mOnionWriteBuffer.clear();
            selectionKey.interestOps(SelectionKey.OP_READ);
            mCurrentState = ClientProtocolStateCode.READ_ONION_PROXY_CONNECTION_REQUEST_RESPONSE;
        }
    }

    private void readOnionProxyConnectionRequestResponse() throws Exception {
        int numRead = mServerDirectedSocketChannel.read(mOnionProxyResponseBuffer);

        throwOnSocketClosedCode(numRead);

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

    private void registerClientReadSelector() throws ClosedChannelException {
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.FROM_CLIENT, this);
        mClientSocketChannel.register(mSelector, SelectionKey.OP_READ, attachment);
    }

    private void readClientPrepareOnionWrite() throws Exception {
        readChannelPrepareWrite(mClientSocketChannel, mServerDirectedSocketChannel, mOnionWriteBuffer);
    }

    private void readOnionPrepareClientWrite() throws Exception {
        readChannelPrepareWrite(mServerDirectedSocketChannel, mClientSocketChannel, mClientWriteBuffer);
    }

    private void writeBufferToClient(SelectionKey selectionKey) throws IOException {
        writeBufferToChannel(mClientWriteBuffer, mClientSocketChannel, selectionKey);
    }

    private void writeBufferToOnionProxy(SelectionKey selectionKey) throws IOException {
        writeBufferToChannel(mOnionWriteBuffer, mServerDirectedSocketChannel, selectionKey);
    }
}
