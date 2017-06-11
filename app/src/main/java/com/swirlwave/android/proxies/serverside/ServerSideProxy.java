package com.swirlwave.android.proxies.serverside;

import android.content.Context;

import com.swirlwave.android.proxies.ChannelAttachment;
import com.swirlwave.android.proxies.ChannelDirection;
import com.swirlwave.android.proxies.ProxyBase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerSideProxy extends ProxyBase {
    public static final int LISTENING_PORT = 9345;
    private ServerSocketChannel mServerSocketChannel;

    public ServerSideProxy(Context context) throws Exception {
        super(context);
    }

    @Override
    protected Selector bindListeningPorts() throws IOException {
        mSelector = Selector.open();

        mServerSocketChannel = bindServerSocketChannel(LISTENING_PORT);

        return mSelector;
    }

    @Override
    protected void accept(SelectionKey selectionKey) throws Exception {
        SocketChannel clientChannel = acceptIncomingConnection(selectionKey);

        ServerProtocolState protocolState = new ServerProtocolState(mContext, mSelector, clientChannel);
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.FROM_CLIENT, protocolState);

        clientChannel.register(mSelector, SelectionKey.OP_WRITE, attachment);
    }

    @Override
    protected void connect(SelectionKey selectionKey) throws Exception {
        ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
        ServerProtocolState protocolState = (ServerProtocolState) attachment.getProtocolState();
        protocolState.finishServerConnect(selectionKey);
    }
}
