package com.swirlwave.android.proxies.serverside;

import com.swirlwave.android.proxies.ChannelAttachment;
import com.swirlwave.android.proxies.ChannelDirection;
import com.swirlwave.android.proxies.ProtocolState;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ServerProtocolState extends ProtocolState {
    private ServerProtocolStateCode mCurrentState;

    public ServerProtocolState(Selector selector, SocketChannel clientSocketChannel, SocketChannel onionProxySocketChannel) {
        super(selector, clientSocketChannel, onionProxySocketChannel);
        mCurrentState = ServerProtocolStateCode.PROXYING;
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
}
