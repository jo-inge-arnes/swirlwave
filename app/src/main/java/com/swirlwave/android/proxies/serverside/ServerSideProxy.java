package com.swirlwave.android.proxies.serverside;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.swirlwave.android.R;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class ServerSideProxy implements Runnable {
    public static final int PORT = 9345;
    public static final int LOCAL_SERVER_PORT = 8088;
    private final Random mRnd = new Random();
    private final Context mContext;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);
    private ServerSocketChannel mServerSocketChannel;
    private Selector mSelector;
    private volatile boolean mRunning = true;

    public ServerSideProxy(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try {
            Selector selector = bindListeningPort();

            while (mRunning) {
                int numChannelsReady = selector.select();

                if (numChannelsReady == 0) {
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    try {
                    } catch (Exception e) {
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

    @NonNull
    private Selector bindListeningPort() throws IOException {
        mSelector = Selector.open();

        mServerSocketChannel = ServerSocketChannel.open();
        mServerSocketChannel.configureBlocking(false);

        ServerSocket serverSocket = mServerSocketChannel.socket();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(PORT);
        serverSocket.bind(inetSocketAddress);

        mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT);

        return mSelector;
    }

    private SocketChannel acceptIncomingSocket(SelectionKey selectionKey) throws IOException {
//        Toaster.show(mContext, "Incoming connection");

        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        Socket socket = serverSocketChannel.socket().accept();
        SocketChannel socketChannel = socket.getChannel();
        socketChannel.configureBlocking(false);
        return socketChannel;
    }

    private SocketChannel connectLocalServer(Selector selector) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        InetSocketAddress localServerAddress = new InetSocketAddress("127.0.0.1", LOCAL_SERVER_PORT);
        socketChannel.connect(localServerAddress);
        return socketChannel;
    }



    public void terminate() {
        mRunning = false;
    }
}
