package com.swirlwave.android.proxies;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class ProtocolState {
    protected static final int CAPACITY = 16384;
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

    protected void readFromServerDirectionPrepareClientWrite() throws Exception {
        readChannelPrepareWrite(mServerDirectedSocketChannel, mClientSocketChannel, mClientWriteBuffer);
    }

    protected void writeBufferToClient() throws IOException, SocketClosedException {
        writeChannel(mClientWriteBuffer, mClientSocketChannel, mServerDirectedSocketChannel);
    }

    protected void writeBufferToServerDirection() throws IOException, SocketClosedException {
        writeChannel(mServerDirectedWriteBuffer, mServerDirectedSocketChannel, mClientSocketChannel);
    }

    private void readChannelPrepareWrite(SocketChannel inChannel, SocketChannel outChannel, ByteBuffer outChannelBuffer) throws Exception {
        outChannelBuffer.clear();

        int numRead = read(inChannel, outChannelBuffer);

        if (numRead > 0) {
            outChannelBuffer.flip();

            try {
                SelectionKey inSelectionKey = inChannel.keyFor(mSelector);
                inSelectionKey.interestOps(inSelectionKey.interestOps() & ~SelectionKey.OP_READ);
            } catch (Exception e) {
                Log.e(mContext.getString(R.string.service_name), "Error changing selection key for in channel in readChannel: " + e.toString());
            }

            try {
                SelectionKey outSelectionKey = outChannel.keyFor(mSelector);
                outSelectionKey.interestOps(outSelectionKey.interestOps() | SelectionKey.OP_WRITE);
            } catch (Exception e) {
                Log.e(mContext.getString(R.string.service_name), "Error changing selection key for out channel in readChannel: " + e.toString());
            }
        }
    }

    private void writeChannel(ByteBuffer outChannelBuffer, SocketChannel outChannel, SocketChannel inChannel) throws IOException, SocketClosedException {
        int numWritten = outChannel.write(outChannelBuffer);

        if (numWritten > 0 && outChannelBuffer.limit() == outChannelBuffer.position()) {
            try {
                SelectionKey outSelectionKey = outChannel.keyFor(mSelector);
                outSelectionKey.interestOps(outSelectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            } catch (Exception e) {
                Log.e(mContext.getString(R.string.service_name), "Error changing selection key for out channel in writeChannel: " + e.toString());
            }

            if (mHasClosedChannel) {
                throw new SocketClosedException("Closed the other socket after finishing writing.");
            }

            try {
                SelectionKey inSelectionKey = inChannel.keyFor(mSelector);
                inSelectionKey.interestOps(inSelectionKey.interestOps() | SelectionKey.OP_READ);
            } catch (Exception e) {
                Log.e(mContext.getString(R.string.service_name), "Error changing selection key for in channel in writeChannel: " + e.toString());
            }
        }
    }

    protected void registerClientReadSelector() throws ClosedChannelException {
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.FROM_CLIENT, this);
        mClientSocketChannel.register(mSelector, SelectionKey.OP_READ, attachment);
    }

    protected int read(SocketChannel socketChannel, ByteBuffer buffer) throws Exception {
        int numRead = -1;

        try {
            numRead = socketChannel.read(buffer);
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), "Error reading from socket channel: " + e.toString());
        }

        throwOnSocketClosedCode(numRead);

        return numRead;
    }

    protected void throwOnSocketClosedCode(int numRead) throws Exception {
        if (numRead == -1)
            throw new SocketClosedException("A result from a read operation in the client proxy indicated that the socket was closed by peer.");
    }
}
