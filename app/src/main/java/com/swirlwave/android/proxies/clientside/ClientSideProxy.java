package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ClientSideProxy implements Runnable {
    public static final int START_PORT = 9346;
    public static final String LOCALHOST = "127.0.0.1";
    public static final int ONION_PROXY_SO_TIMEOUT = 30000;
    private Context mContext;
    private LocalSettings mLocalSettings;
    private List<ServerSocketChannel> mServerSocketChannels = new ArrayList<>();
    private Selector mSelector;
    private volatile boolean mRunning = true;
    private String mPublicKeyString, mPrivateKeyString;

    public ClientSideProxy(Context context) throws Exception {
        mContext = context;
        mLocalSettings = new LocalSettings(mContext);
        Pair<String, String> keys = mLocalSettings.getAsymmetricKeys();
        mPublicKeyString = keys.first;
        mPrivateKeyString = keys.second;
    }

    @Override
    public void run() {
        try {
            Selector selector = bindListeningPorts();

            while (mRunning) {
                int numChannelsReady = selector.select();

                if (numChannelsReady == 0)
                    continue;

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    try {
                        if (selectionKey.isAcceptable()) {
                            SocketChannel clientChannel = acceptIncomingConnection(selectionKey);
                            establishOnionProxyConnection(selector, clientChannel);
                        } else if (selectionKey.isConnectable()) {
                            prepareOnionProxyConnectionRequest(selectionKey, getFirstAndBestFriend().getAddress());
                        } else if (selectionKey.isWritable()) {
                            write(selectionKey);
                        } else if (selectionKey.isReadable()) {
                            read(selectionKey);
                        }
                    } catch (Exception e) {
                        closeChannels(selectionKey);
                        Log.e(mContext.getString(R.string.service_name), e.toString());
                    }

                    iterator.remove();
                }

                keys.clear();
            }
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }
    }

    private Selector bindListeningPorts() throws IOException {
        mSelector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        ServerSocket serverSocket = serverSocketChannel.socket();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(START_PORT);
        serverSocket.bind(inetSocketAddress);

        serverSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        mServerSocketChannels.add(serverSocketChannel);

        return mSelector;
    }

    private SocketChannel acceptIncomingConnection(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();

        Socket clientSocket = serverChannel.socket().accept();
        SocketChannel clientChannel = clientSocket.getChannel();
        clientChannel.configureBlocking(false);

        return clientChannel;
    }

    private void establishOnionProxyConnection(Selector selector, SocketChannel clientSocketChannel) throws IOException {
        SocketChannel onionProxyChannel = SocketChannel.open();
        onionProxyChannel.configureBlocking(false);

        ProtocolState protocolState = new ProtocolState(selector, clientSocketChannel, onionProxyChannel);
        ChannelAttachment attachment = new ChannelAttachment(ChannelDirection.TO_ONION_PROXY, protocolState);
        onionProxyChannel.register(selector, SelectionKey.OP_CONNECT, attachment);

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
            ProtocolState protocolState = ((ChannelAttachment) onionProxySelectionKey.attachment()).getProtocolState();

            protocolState.prepareOnionProxyConnectionRequest(selectionKey, onionAddress, (short) SwirlwaveOnionProxyManager.HIDDEN_SERVICE_PORT);
        } else {
            throw new Exception("Could not finish connect to onion proxy!");
        }
    }

    private Peer getFirstAndBestFriend() {
        List<UUID> friendUuids = PeersDb.selectAllFriendUuids(mContext);
        return PeersDb.selectByUuid(mContext, friendUuids.get(0));
    }

    private void write(SelectionKey selectionKey) throws IOException {
        ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
        ProtocolState protocolState = attachment.getProtocolState();

        switch (attachment.getChannelDirection()) {
            case TO_ONION_PROXY:
                protocolState.writeOnionProxy(selectionKey);
                break;
            case FROM_CLIENT:
                protocolState.writeClient(selectionKey);
                break;
            default:
                break;
        }
    }

    private void read(SelectionKey selectionKey) throws Exception {
        ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
        ProtocolState protocolState = attachment.getProtocolState();

        switch (attachment.getChannelDirection()) {
            case TO_ONION_PROXY:
                protocolState.readOnionProxy(selectionKey);
                break;
            case FROM_CLIENT:
                protocolState.readClient(selectionKey);
                break;
            default:
                break;
        }
    }

    private void closeChannels(SelectionKey selectionKey) {
        if (selectionKey.attachment() != null) {
            ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
            closeChannel(getOtherChannel(attachment));
            selectionKey.attach(null);
        }

        closeChannel((SocketChannel)selectionKey.channel());
    }

    private SocketChannel getOtherChannel(ChannelAttachment attachment) {
        ChannelDirection direction = attachment.getChannelDirection();
        ProtocolState state = attachment.getProtocolState();

        return direction == ChannelDirection.FROM_CLIENT ? state.getOnionProxySocketChannel() : state.getClientSocketChannel();
    }

    private void closeChannel(SocketChannel socketChannel) {
        Socket socket = socketChannel.socket();

        try {
            // Closing the socket, even if channel will be closed.
            // Reason: To close a little bit sooner. The cancel on the channel will not close the socket until the next select.
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ie) {
            Log.e(mContext.getString(R.string.service_name), "Error closing socket" + socket + ": " + ie);
        }

        try {
            socketChannel.close();
        } catch (IOException ie) {
            Log.e(mContext.getString(R.string.service_name), "Error closing channel: " + ie);
        }
    }

    public void terminate() {
        mRunning = false;
    }
}
