package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ChannelAttachment;
import com.swirlwave.android.proxies.ChannelDirection;
import com.swirlwave.android.proxies.ProtocolState;
import com.swirlwave.android.proxies.ProxyBase;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientSideProxy extends ProxyBase {
    public static final int START_PORT = 9346;
    public static final String LOCALHOST = "127.0.0.1";
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
    protected void accept(SelectionKey selectionKey) throws Exception {
        SocketChannel clientChannel = acceptIncomingConnection(selectionKey);
        establishOnionProxyConnection(clientChannel);
    }

    @Override
    protected void connect(SelectionKey selectionKey) throws Exception {
        prepareOnionProxyConnectionRequest(selectionKey, getFirstAndBestFriend().getAddress());
    }

    @Override
    protected Selector bindListeningPorts() throws IOException {
        mSelector = Selector.open();

        ServerSocketChannel serverSocketChannel = bindServerSocketChannel(START_PORT);
        mServerSocketChannels.add(serverSocketChannel);

        return mSelector;
    }

    private void establishOnionProxyConnection(SocketChannel clientSocketChannel) throws IOException {
        SocketChannel onionProxyChannel = SocketChannel.open();
        onionProxyChannel.configureBlocking(false);

        ClientProtocolState protocolState = new ClientProtocolState(mSelector, clientSocketChannel, onionProxyChannel);
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.TOWARDS_SERVER, protocolState);
        onionProxyChannel.register(mSelector, SelectionKey.OP_CONNECT, attachment);

        onionProxyChannel.connect(getLocalOnionProxyAddress());
    }

    private InetSocketAddress getLocalOnionProxyAddress() {
        return new InetSocketAddress(LOCALHOST, SwirlwaveOnionProxyManager.getsSocksPort());
    }

    private void prepareOnionProxyConnectionRequest(SelectionKey selectionKey, String onionAddress) throws Exception {
        SocketChannel onionProxyChannel = (SocketChannel) selectionKey.channel();
        onionProxyChannel.socket().setSoTimeout(ONION_PROXY_SO_TIMEOUT);

        if (onionProxyChannel.finishConnect()) {
            SelectionKey onionProxySelectionKey = selectionKey;
            ChannelAttachment attachment = (ChannelAttachment) onionProxySelectionKey.attachment();
            ClientProtocolState protocolState = (ClientProtocolState) attachment.getProtocolState();

            protocolState.prepareOnionProxyConnectionRequest(selectionKey, onionAddress, (short) SwirlwaveOnionProxyManager.HIDDEN_SERVICE_PORT);
        } else {
            throw new Exception("Could not finish connect to onion proxy!");
        }
    }

    private Peer getFirstAndBestFriend() {
        List<UUID> friendUuids = PeersDb.selectAllFriendUuids(mContext);
        return PeersDb.selectByUuid(mContext, friendUuids.get(0));
    }
}
