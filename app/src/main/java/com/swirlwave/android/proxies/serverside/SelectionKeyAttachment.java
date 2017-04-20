package com.swirlwave.android.proxies.serverside;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SelectionKeyAttachment {
    private boolean mIsClientChannel;
    private SocketChannel mSocketChannel;
    private SelectionKey mSelectionKey;

    public SelectionKeyAttachment(SocketChannel socketChannel, SelectionKey selectionKey, boolean isClientChannel) {
        mSocketChannel = socketChannel;
        mSelectionKey = selectionKey;
        mIsClientChannel = isClientChannel;
    }

    public SocketChannel getSocketChannel() {
        return mSocketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        mSocketChannel = socketChannel;
    }

    public SelectionKey getSelectionKey() {
        return mSelectionKey;
    }

    public void setSelectionKey(SelectionKey selectionKey) {
        mSelectionKey = selectionKey;
    }

    public boolean isClientChannel() {
        return mIsClientChannel;
    }

    public void setClientChannel(boolean isClientChannel) {
        mIsClientChannel = isClientChannel;
    }
}
