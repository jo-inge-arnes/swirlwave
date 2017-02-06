package com.swirlwave.android.serversideproxy;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SelectionKeyAttachment {
    private boolean mIsClientChannel;
    private SocketChannel mSocketChannel;
    private SelectionKey mSelectionKey;
    private IncomingClientMessageManager mIncomingClientMessageManager;

    public SelectionKeyAttachment(SocketChannel socketChannel, SelectionKey selectionKey, boolean isClientChannel, IncomingClientMessageManager incomingClientMessageManager) {
        mSocketChannel = socketChannel;
        mSelectionKey = selectionKey;
        mIsClientChannel = isClientChannel;
        mIncomingClientMessageManager = incomingClientMessageManager;
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

    public IncomingClientMessageManager getIncomingClientMessageManager() {
        return mIncomingClientMessageManager;
    }

    public void setIncomingClientMessageManager(IncomingClientMessageManager incomingClientMessageManager) {
        mIncomingClientMessageManager = incomingClientMessageManager;
    }
}
