package com.swirlwave.android.proxies;

/**
 * Class representing a TLV message
 */
public class IncomingMessage {
    private int mInputBytesProcessed;
    private byte mType = -1;
    private byte[] mLengthBytes = new byte[4];
    private int mLength = -1;
    private byte[] mValue;

    public int getInputBytesProcessed() {
        return mInputBytesProcessed;
    }

    public void setInputBytesProcessed(int inputBytesProcessed) {
        this.mInputBytesProcessed = inputBytesProcessed;
    }

    public byte getType() {
        return mType;
    }

    public void setType(byte type) {
        mType = type;
    }

    public int getLength() {
        return mLength;
    }

    public void setLength(int length) {
        mLength = length;
        mValue = new byte[length];
    }

    public byte[] getValue() {
        return mValue;
    }

    public void setValue(byte[] value) {
        mValue = value;
    }

    public byte[] getLengthBytes() {
        return mLengthBytes;
    }

    public int increaseBytesProcessed(int increment) {
        return mInputBytesProcessed += increment;
    }

    public boolean complete() {
        return (mInputBytesProcessed >= 5) && (mInputBytesProcessed - 5 == mLength);
    }
}
