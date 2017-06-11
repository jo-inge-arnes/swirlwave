package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Pair;

import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ChannelAttachment;
import com.swirlwave.android.proxies.ChannelDirection;
import com.swirlwave.android.proxies.ProxyBase;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientSideProxy extends ProxyBase {
    public static final int START_PORT = 9346;
    public static final int ONION_PROXY_SO_TIMEOUT = 30000;
    private final List<ServerSocketChannel> mServerSocketChannels = new ArrayList<>();
    private final String mPublicKeyString, mPrivateKeyString;

    public ClientSideProxy(Context context) throws Exception {
        super(context);

        Pair<String, String> keys = mLocalSettings.getAsymmetricKeys();
        mPublicKeyString = keys.first;
        mPrivateKeyString = keys.second;
    }

    @Override
    protected Selector bindListeningPorts() throws IOException {
        mSelector = Selector.open();

        ServerSocketChannel serverSocketChannel = bindServerSocketChannel(START_PORT);
        mServerSocketChannels.add(serverSocketChannel);

        return mSelector;
    }

    @Override
    protected void accept(SelectionKey selectionKey) throws Exception {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();

        SocketChannel clientChannel = acceptIncomingConnection(selectionKey);

        int incomingPort = serverSocketChannel.socket().getLocalPort();
        Peer friend = getDestinationFriendFromPort(incomingPort);

        if (friend.getOnlineStatus()) {
            establishOnionProxyConnection(clientChannel, friend);
        } else {
            closeChannel(clientChannel);
        }
    }

    @Override
    protected void connect(SelectionKey selectionKey) throws Exception {
        prepareOnionProxyConnectionRequest(selectionKey);
    }

    private void establishOnionProxyConnection(SocketChannel clientSocketChannel, Peer friend) throws IOException {
        SocketChannel onionProxyChannel = SocketChannel.open();
        onionProxyChannel.configureBlocking(false);

        ClientProtocolState protocolState = new ClientProtocolState(mContext, mSelector, clientSocketChannel, onionProxyChannel, mPrivateKeyString, mLocalSettings, friend);
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.TOWARDS_SERVER, protocolState);
        onionProxyChannel.register(mSelector, SelectionKey.OP_CONNECT, attachment);

        onionProxyChannel.connect(getLocalOnionProxyAddress());
    }

    private InetSocketAddress getLocalOnionProxyAddress() {
        return new InetSocketAddress(LOCALHOST, SwirlwaveOnionProxyManager.getsSocksPort());
    }

    private void prepareOnionProxyConnectionRequest(SelectionKey selectionKey) throws Exception {
        SocketChannel onionProxyChannel = (SocketChannel) selectionKey.channel();
        onionProxyChannel.socket().setSoTimeout(ONION_PROXY_SO_TIMEOUT);

        if (onionProxyChannel.finishConnect()) {
            SelectionKey onionProxySelectionKey = selectionKey;
            ChannelAttachment attachment = (ChannelAttachment) onionProxySelectionKey.attachment();
            ClientProtocolState protocolState = (ClientProtocolState) attachment.getProtocolState();

            protocolState.prepareOnionProxyConnectionRequest(selectionKey, (short) SwirlwaveOnionProxyManager.HIDDEN_SERVICE_PORT);
        } else {
            throw new Exception("Could not finish connect to onion proxy!");
        }
    }

    private Peer getDestinationFriendFromPort(int incomingPort) {
        List<UUID> friendUuids = PeersDb.selectAllFriendUuids(mContext);
        return PeersDb.selectByUuid(mContext, friendUuids.get(0));
    }
}
