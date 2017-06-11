package com.swirlwave.android.proxies;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.settings.LocalSettings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public abstract class ProxyBase implements Runnable {
    public static final String LOCALHOST = "127.0.0.1";
    protected final Context mContext;
    protected final LocalSettings mLocalSettings;
    protected Selector mSelector;
    protected volatile boolean mRunning = true;

    protected ProxyBase(Context context) throws Exception {
        mContext = context;
        mLocalSettings = new LocalSettings(mContext);

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
                            accept(selectionKey);
                        } else if (selectionKey.isConnectable()) {
                            connect(selectionKey);
                        } else if (selectionKey.isWritable()) {
                            write(selectionKey);
                        } else if (selectionKey.isReadable()) {
                            read(selectionKey);
                        }
                    } catch (SocketClosedException sce) {
                        try {
                            closeChannel((SocketChannel)selectionKey.channel());
                            Log.i(mContext.getString(R.string.service_name), sce.toString());

                            if (selectionKey.attachment() != null && selectionKey.attachment() instanceof ChannelAttachment) {
                                ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
                                SocketChannel otherChannel = getOtherChannel(attachment);
                                SelectionKey otherChannelSelectionKey = otherChannel.keyFor(mSelector);

                                if (otherChannelSelectionKey != null) {
                                    boolean hasOpWrite = (otherChannelSelectionKey.interestOps() & SelectionKey.OP_WRITE) != 0;

                                    if (hasOpWrite) {
                                        // Mark state object, so that the other channel can close it self when it is finished writing.
                                        ((ChannelAttachment) selectionKey.attachment()).getProtocolState().setHasClosedChannel(true);
                                        Log.i(mContext.getString(R.string.service_name), "Marked other channel for close");
                                    } else {
                                        closeChannel(otherChannel);
                                        Log.i(mContext.getString(R.string.service_name), "Closed the other channel also");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(mContext.getString(R.string.service_name), "Error closing channel: " + e.toString());
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

    protected abstract Selector bindListeningPorts() throws Exception;

    protected abstract void accept(SelectionKey selectionKey) throws Exception;

    protected abstract void connect(SelectionKey selectionKey) throws Exception;

    protected void write(SelectionKey selectionKey) throws Exception {
        ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
        ProtocolState protocolState = attachment.getProtocolState();

        switch (attachment.getChannelDirection()) {
            case TOWARDS_SERVER:
                protocolState.writeServer(selectionKey);
                break;
            case FROM_CLIENT:
                protocolState.writeClient(selectionKey);
                break;
            default:
                break;
        }
    }

    protected SocketChannel acceptIncomingConnection(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();

        Socket clientSocket = serverChannel.socket().accept();
        SocketChannel clientChannel = clientSocket.getChannel();
        clientChannel.configureBlocking(false);

        return clientChannel;
    }

    protected void read(SelectionKey selectionKey) throws Exception {
        ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
        ProtocolState protocolState = attachment.getProtocolState();

        switch (attachment.getChannelDirection()) {
            case TOWARDS_SERVER:
                protocolState.readServer(selectionKey);
                break;
            case FROM_CLIENT:
                protocolState.readClient(selectionKey);
                break;
            default:
                break;
        }
    }

    @NonNull
    protected ServerSocketChannel bindServerSocketChannel(int listeningPort) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        ServerSocket serverSocket = serverSocketChannel.socket();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(listeningPort);
        serverSocket.bind(inetSocketAddress);

        serverSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        return serverSocketChannel;
    }

    protected void closeChannels(SelectionKey selectionKey) {
        if (selectionKey.attachment() != null && selectionKey.attachment() instanceof ChannelAttachment) {
            ChannelAttachment attachment = (ChannelAttachment) selectionKey.attachment();
            closeChannel(getOtherChannel(attachment));
            selectionKey.attach(null);
        }

        closeChannel((SocketChannel) selectionKey.channel());
    }

    protected void closeChannel(SocketChannel socketChannel) {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException ie) {
                Log.e(mContext.getString(R.string.service_name), "Error closing channel: " + ie);
            }
        }
    }

    protected SocketChannel getOtherChannel(ChannelAttachment attachment) {
        ChannelDirection direction = attachment.getChannelDirection();
        ProtocolState state = attachment.getProtocolState();

        return direction == ChannelDirection.FROM_CLIENT ? state.getServerDirectedChannel() : state.getClientSocketChannel();
    }

    public void terminate() {
        mRunning = false;
    }
}
