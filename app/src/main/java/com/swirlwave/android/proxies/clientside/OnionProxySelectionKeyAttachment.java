package com.swirlwave.android.proxies.clientside;

import com.swirlwave.android.proxies.SelectionKeyAttachment;

import java.nio.channels.SocketChannel;

public class OnionProxySelectionKeyAttachment extends SelectionKeyAttachment {
    private ClientProxyMode mMode = ClientProxyMode.UNKNOWN;

    private int mOnionProxyResultBytesReceived = 0;
    private byte[] mOnionProxyResult = new byte[2];

    private int mServerProxyRandomBytesReceived = 0;
    private byte[] mServerProxyRandomBytes = new byte[4];

    public OnionProxySelectionKeyAttachment(SocketChannel socketChannel) {
        super(socketChannel, null, true);
    }

    public ClientProxyMode getMode() {
        return mMode;
    }

    public void setMode(ClientProxyMode mode) {
        mMode = mode;
    }

    public int getOnionProxyResultBytesReceived() {
        return mOnionProxyResultBytesReceived;
    }

    public void setOnionProxyResultBytesReceived(int onionProxyResultBytesReceived) {
        mOnionProxyResultBytesReceived = onionProxyResultBytesReceived;
    }

    public byte[] getOnionProxyResult() {
        return mOnionProxyResult;
    }

    public int getServerProxyRandomBytesReceived() {
        return mServerProxyRandomBytesReceived;
    }

    public void setServerProxyRandomBytesReceived(int serverProxyRandomBytesReceived) {
        mServerProxyRandomBytesReceived = serverProxyRandomBytesReceived;
    }

    public byte[] getServerProxyRandomBytes() {
        return mServerProxyRandomBytes;
    }

    @Override
    public boolean acceptingPayload() {
        return mMode == ClientProxyMode.ACCEPTING_PAYLOAD;
    }
}
