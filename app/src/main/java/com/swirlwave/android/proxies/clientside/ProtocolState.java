package com.swirlwave.android.proxies.clientside;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ProtocolState {
    private final Selector mSelector;
    private final SocketChannel mClientSocketChannel;
    private final SocketChannel mOnionProxySocketChannel;
    private final ByteBuffer mOnionProxyResponseBuffer = ByteBuffer.allocate(8);
    private final ByteBuffer mOnionWriteBuffer = ByteBuffer.allocate(16384);
    private final ByteBuffer mClientWriteBuffer = ByteBuffer.allocate(16384);
    private ProtocolStateCode mCurrentState;

    public SocketChannel getClientSocketChannel() {
        return mClientSocketChannel;
    }

    public SocketChannel getOnionProxySocketChannel() {
        return mOnionProxySocketChannel;
    }

    public ProtocolState(Selector selector, SocketChannel clientSocketChannel, SocketChannel onionProxySocketChannel) {
        mSelector = selector;
        mClientSocketChannel = clientSocketChannel;
        mOnionProxySocketChannel = onionProxySocketChannel;
        mCurrentState = ProtocolStateCode.CONNECT_ONION_PROXY;
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
        mCurrentState = ProtocolStateCode.WRITE_ONION_PROXY_CONNECTION_REQUEST;
    }

    public void writeOnionProxy(SelectionKey selectionKey) throws IOException {
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

    public void readOnionProxy(SelectionKey selectionKey) throws Exception {
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
        mOnionProxySocketChannel.write(mOnionWriteBuffer);

        if (!mOnionWriteBuffer.hasRemaining()) {
            mOnionWriteBuffer.clear();
            selectionKey.interestOps(SelectionKey.OP_READ);
            mCurrentState = ProtocolStateCode.READ_ONION_PROXY_CONNECTION_REQUEST_RESPONSE;
        }
    }

    private void readOnionProxyConnectionRequestResponse() throws Exception {
        int numRead = mOnionProxySocketChannel.read(mOnionProxyResponseBuffer);

        throwOnSocketClosedCode(numRead);

        if (mOnionProxyResponseBuffer.position() == 8) {
            mOnionProxyResponseBuffer.flip();

            byte firstByte = mOnionProxyResponseBuffer.get();
            byte secondByte = mOnionProxyResponseBuffer.get();

            if (firstByte == (byte)0x00 && secondByte == (byte)0x5a) {
                registerClientReadSelector();
                mCurrentState = ProtocolStateCode.PROXYING;
            } else {
                mCurrentState = ProtocolStateCode.ONION_PROXY_CONNECTION_REQUEST_REJECTED;
                throw new Exception(String.format("Onion proxy connection request rejected. Response: 0x%02X 0x%02X", firstByte, secondByte));
            }
        }
    }

    private void throwOnSocketClosedCode(int numRead) throws Exception {
        if (numRead == -1)
            throw new Exception("A result from a read operation in the client proxy indicated that the socket was closed by peer.");
    }

    private void registerClientReadSelector() throws ClosedChannelException {
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.FROM_CLIENT, this);
        mClientSocketChannel.register(mSelector, SelectionKey.OP_READ, attachment);
    }

    private void readClientPrepareOnionWrite() throws Exception {
        readChannelPrepareWrite(mClientSocketChannel, mOnionProxySocketChannel, mOnionWriteBuffer);
    }

    private void readOnionPrepareClientWrite() throws Exception {
        readChannelPrepareWrite(mOnionProxySocketChannel, mClientSocketChannel, mClientWriteBuffer);
    }

    private void writeBufferToClient(SelectionKey selectionKey) throws IOException {
        writeBufferToChannel(mClientWriteBuffer, mClientSocketChannel, selectionKey);
    }

    private void writeBufferToOnionProxy(SelectionKey selectionKey) throws IOException {
        writeBufferToChannel(mOnionWriteBuffer, mOnionProxySocketChannel, selectionKey);
    }

    private void readChannelPrepareWrite(SocketChannel inChannel, SocketChannel outChannel, ByteBuffer buffer) throws Exception {
        if (!buffer.isReadOnly()) {
            int numRead = inChannel.read(buffer);

            throwOnSocketClosedCode(numRead);

            SelectionKey outChannelSelectionKey = outChannel.keyFor(mSelector);
            outChannelSelectionKey.interestOps(outChannelSelectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private void writeBufferToChannel(ByteBuffer buffer, SocketChannel channel, SelectionKey selectionKey) throws IOException {
        if (!buffer.isReadOnly()) {
            buffer.flip();
        }

        channel.write(buffer);

        if (!buffer.hasRemaining()) {
            buffer.clear();
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }
}
