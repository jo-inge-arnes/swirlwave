package com.swirlwave.android.proxies.serverside;

import java.io.ByteArrayOutputStream;

public class ConnectionMessageSelectionKeyAttachment {
    private int mMessageLength = -1;
    private int mBytesRead = 0;
    private boolean mNotCompleted = true;

    private ByteArrayOutputStream mByteArrayStream = new ByteArrayOutputStream();
    private int sentRandomNumber;

    public int getMessageLength() {
        return mMessageLength;
    }

    public void setMessageLength(int messageLength) {
        mMessageLength = messageLength;
    }

    public int getBytesRead() {
        return mBytesRead;
    }

    public void setBytesRead(int bytesRead) {
        mBytesRead = bytesRead;
    }

    public ByteArrayOutputStream getByteArrayStream() {
        return mByteArrayStream;
    }

    public void setByteArrayStream(ByteArrayOutputStream byteArrayOutputStream) {
        mByteArrayStream = byteArrayOutputStream;
    }

    public void setCompletedStatus(boolean isCompleted) {
        mNotCompleted = !isCompleted;
    }

    public boolean notCompleted() {
        return mNotCompleted;
    }

    public boolean isCompleted() {
        return !mNotCompleted;
    }

    public int getSentRandomNumber() {
        return sentRandomNumber;
    }

    public void setSentRandomNumber(int sentRandomNumber) {
        this.sentRandomNumber = sentRandomNumber;
    }
}
