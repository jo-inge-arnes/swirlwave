package com.swirlwave.android.proxies;

import android.content.Context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class ProtocolState {
    protected static final int CAPACITY = 1048576 * 4; // 4 MiB
    protected static final byte CONNECTION_MESSAGE_ACCEPTED = (byte) 0x0a;
    protected static final byte CONNECTION_MESSAGE_REJECTED = (byte) 0x0B;
    protected final Context mContext;
    protected final Selector mSelector;
    protected final SocketChannel mClientSocketChannel;
    protected SocketChannel mServerDirectedSocketChannel;
    protected final ByteBuffer mClientWriteBuffer = ByteBuffer.allocate(CAPACITY);
    protected final ByteBuffer mServerDirectedWriteBuffer = ByteBuffer.allocate(CAPACITY);
    private boolean mHasClosedChannel;

    public SocketChannel getClientSocketChannel() {
        return mClientSocketChannel;
    }

    public SocketChannel getServerDirectedChannel() {
        return mServerDirectedSocketChannel;
    }

    public boolean hasClosedChannel() {
        return mHasClosedChannel;
    }

    public void setHasClosedChannel(boolean hasClosedChannel) {
        mHasClosedChannel = hasClosedChannel;
    }

    public void setServerDirectedChannel(SocketChannel serverDirectedSocketChannel) {
        mServerDirectedSocketChannel = serverDirectedSocketChannel;
    }

    public ProtocolState(Context context, Selector selector, SocketChannel clientSocketChannel, SocketChannel serverDirectedSocketChannel) {
        mContext = context;
        mSelector = selector;
        mClientSocketChannel = clientSocketChannel;
        mServerDirectedSocketChannel = serverDirectedSocketChannel;
    }

    public abstract void writeClient(SelectionKey selectionKey) throws Exception;

    public abstract void writeServer(SelectionKey selectionKey) throws Exception;

    public abstract void readClient(SelectionKey selectionKey) throws Exception;

    public abstract void readServer(SelectionKey selectionKey) throws Exception;

    protected void readClientPrepareServerDirectedWrite() throws Exception {
        readChannelPrepareWrite(mClientSocketChannel, mServerDirectedSocketChannel, mServerDirectedWriteBuffer);
    }

    protected void readOnionPrepareClientWrite() throws Exception {
        readChannelPrepareWrite(mServerDirectedSocketChannel, mClientSocketChannel, mClientWriteBuffer);
    }

    protected void writeBufferToClient(SelectionKey selectionKey) throws IOException, SocketClosedException {
        writeBufferToChannel(mClientWriteBuffer, mClientSocketChannel, selectionKey);
    }

    protected void writeBufferToServerDirection(SelectionKey selectionKey) throws IOException, SocketClosedException {
        writeBufferToChannel(mServerDirectedWriteBuffer, mServerDirectedSocketChannel, selectionKey);
    }

    protected void writeBufferToChannel(ByteBuffer buffer, SocketChannel channel, SelectionKey selectionKey) throws IOException, SocketClosedException {
        if (!buffer.isReadOnly()) {
            buffer.flip();
        }

        channel.write(buffer);

        if (!buffer.hasRemaining()) {
            buffer.clear();
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);

            if (mHasClosedChannel) {
                throw new SocketClosedException("Closed the other socket after finishing writing.");
            }
        }
    }

    protected void readChannelPrepareWrite(SocketChannel inChannel, SocketChannel outChannel, ByteBuffer buffer) throws Exception {
        if (!buffer.isReadOnly()) {
            read(inChannel, buffer);
            SelectionKey outChannelSelectionKey = outChannel.keyFor(mSelector);
            outChannelSelectionKey.interestOps(outChannelSelectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    protected void registerClientReadSelector() throws ClosedChannelException {
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.FROM_CLIENT, this);
        mClientSocketChannel.register(mSelector, SelectionKey.OP_READ, attachment);
    }

    protected void read(SocketChannel socketChannel, ByteBuffer buffer) throws Exception {
        int numRead = socketChannel.read(buffer);
        throwOnSocketClosedCode(numRead);
    }

    protected void throwOnSocketClosedCode(int numRead) throws Exception {
        if (numRead == -1)
            throw new SocketClosedException("A result from a read operation in the client proxy indicated that the socket was closed by peer.");
    }
}
