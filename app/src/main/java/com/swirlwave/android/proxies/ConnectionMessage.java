package com.swirlwave.android.proxies;

import com.swirlwave.android.crypto.AsymmetricEncryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ConnectionMessage {
    private UUID mSenderId;
    private int mRandomNumber;
    private MessageType mMessageType;
    private UUID mDestination;
    private byte[] mSystemMessage;

    public UUID getSenderId() {
        return mSenderId;
    }

    public void setSenderId(UUID senderId) {
        this.mSenderId = senderId;
    }

    public int getRandomNumber() {
        return mRandomNumber;
    }

    public void setRandomNumber(int randomNumber) {
        this.mRandomNumber = randomNumber;
    }

    public void setRandomNumber(byte[] intBytes) throws Exception {
        mRandomNumber = bytesToInt(intBytes);
    }

    public static int bytesToInt(byte[] intBytes) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(intBytes); DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            return dataInputStream.readInt();
        }
    }

    public MessageType getMessageType() {
        return mMessageType;
    }

    public void setMessageType(MessageType messageType) {
        this.mMessageType = messageType;
    }

    public UUID getDestination() {
        return mDestination;
    }

    public void setDestination(UUID destination) {
        this.mDestination = destination;
    }

    public byte[] getSystemMessage() {
        return mSystemMessage;
    }

    public void setSystemMessage(byte[] systemMessage) {
        this.mSystemMessage = systemMessage;
    }

    public static UUID extractSenderId(byte[] bytes) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes); DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            return new UUID(dataInputStream.readLong(), dataInputStream.readLong());
        }
    }

    public static ConnectionMessage fromByteArray(byte[] bytes, String publicKeyString) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes); DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            ConnectionMessage message = new ConnectionMessage();
            message.setSenderId(new UUID(dataInputStream.readLong(), dataInputStream.readLong()));

            byte[] encryptedBytes = new byte[byteArrayInputStream.available()];
            byteArrayInputStream.read(encryptedBytes, 0, byteArrayInputStream.available());
            byte[] contentBytes = AsymmetricEncryption.decryptBytes(encryptedBytes, publicKeyString, false);

            processContentBytes(contentBytes, message);

            return message;
        }
    }

    public byte[] toByteArray(String encryptionKeyString) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeLong(mSenderId.getMostSignificantBits());
            dataOutputStream.writeLong(mSenderId.getLeastSignificantBits());
            byte[] contentBytes = getContentBytes();
            byte[] encryptedBytes = AsymmetricEncryption.encryptBytes(contentBytes, encryptionKeyString, false);
            dataOutputStream.write(encryptedBytes);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] getContentBytes() throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeInt(mRandomNumber);
            dataOutputStream.write((byte)mMessageType.ordinal());
            dataOutputStream.writeLong(mDestination.getMostSignificantBits());
            dataOutputStream.writeLong(mDestination.getLeastSignificantBits());
            if (mSystemMessage != null) {
                dataOutputStream.write(mSystemMessage);
            }

            return byteArrayOutputStream.toByteArray();
        }
    }

    private static void processContentBytes(byte[] bytes, ConnectionMessage message) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes); DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            message.setRandomNumber(dataInputStream.readInt());
            message.setMessageType(MessageType.values()[(int)dataInputStream.readByte()]);
            message.setDestination(new UUID(dataInputStream.readLong(), dataInputStream.readLong()));

            if (byteArrayInputStream.available() > 0) {
                byte[] systemMessageBytes = new byte[byteArrayInputStream.available()];
                byteArrayInputStream.read(systemMessageBytes, 0, byteArrayInputStream.available());
                message.setSystemMessage(systemMessageBytes);
            }
        }
    }
}
