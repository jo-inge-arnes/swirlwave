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
    public static final int LOCAL_SERVER_PORT = 8088;
    public static final int SERVER_SO_TIMEOUT = 30000;
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
        connectLocalServer(clientChannel);
    }

    @Override
    protected void connect(SelectionKey selectionKey) throws Exception {
        finishServerConnect(selectionKey);
    }

    private void connectLocalServer(SocketChannel clientSocketChannel) throws IOException {
        SocketChannel serverDirectedSocketChannel = SocketChannel.open();
        serverDirectedSocketChannel.configureBlocking(false);

        ServerProtocolState protocolState = new ServerProtocolState(mSelector, clientSocketChannel, serverDirectedSocketChannel);
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.TOWARDS_SERVER, protocolState);
        serverDirectedSocketChannel.register(mSelector, SelectionKey.OP_CONNECT, attachment);

        serverDirectedSocketChannel.connect(getLocalServerAddress());
    }

    private void finishServerConnect(SelectionKey selectionKey) throws Exception{
        SocketChannel serverChannel = (SocketChannel) selectionKey.channel();
        serverChannel.socket().setSoTimeout(SERVER_SO_TIMEOUT);

        if (serverChannel.finishConnect()) {
            SelectionKey serverSelectionKey = selectionKey;
            ChannelAttachment attachment = (ChannelAttachment) serverSelectionKey.attachment();
            ServerProtocolState protocolState = (ServerProtocolState) attachment.getProtocolState();

            protocolState.serverConnectFinished(selectionKey);
        } else {
            throw new Exception("Could not finish connect to local server!");
        }
    }

    private InetSocketAddress getLocalServerAddress() {
        return new InetSocketAddress(LOCALHOST, LOCAL_SERVER_PORT);
    }
}
