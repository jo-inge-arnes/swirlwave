package com.swirlwave.android.proxies;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class ProtocolState {
    protected final Selector mSelector;
    protected final SocketChannel mClientSocketChannel;
    protected final SocketChannel mServerDirectedSocketChannel;

    public SocketChannel getClientSocketChannel() {
        return mClientSocketChannel;
    }

    public SocketChannel getServerDirectedChannel() {
        return mServerDirectedSocketChannel;
    }

    public ProtocolState(Selector selector, SocketChannel clientSocketChannel, SocketChannel serverDirectedSocketChannel) {
        mSelector = selector;
        mClientSocketChannel = clientSocketChannel;
        mServerDirectedSocketChannel = serverDirectedSocketChannel;
    }

    public abstract void writeClient(SelectionKey selectionKey) throws Exception;

    public abstract void writeServer(SelectionKey selectionKey) throws Exception;

    public abstract void readClient(SelectionKey selectionKey) throws Exception;

    public abstract void readServer(SelectionKey selectionKey) throws Exception;

    protected void writeBufferToChannel(ByteBuffer buffer, SocketChannel channel, SelectionKey selectionKey) throws IOException {
        if (!buffer.isReadOnly()) {
            buffer.flip();
        }

        channel.write(buffer);

        if (!buffer.hasRemaining()) {
            buffer.clear();
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    protected void readChannelPrepareWrite(SocketChannel inChannel, SocketChannel outChannel, ByteBuffer buffer) throws Exception {
        if (!buffer.isReadOnly()) {
            int numRead = inChannel.read(buffer);

            throwOnSocketClosedCode(numRead);

            SelectionKey outChannelSelectionKey = outChannel.keyFor(mSelector);
            outChannelSelectionKey.interestOps(outChannelSelectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    protected void throwOnSocketClosedCode(int numRead) throws Exception {
        if (numRead == -1)
            throw new Exception("A result from a read operation in the client proxy indicated that the socket was closed by peer.");
    }
}
