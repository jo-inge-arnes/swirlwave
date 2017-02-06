package com.swirlwave.android.serversideproxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that accumulates chunks of bytes, and assembles them into messages.
 */
public class MessageManager {
    private Message mUnfinishedMessage;

    public Message getUnfinishedMessage() {
        return mUnfinishedMessage;
    }

    public List<Message> processBytes(ByteBuffer byteBuffer) {
        List<Message> completedMessages = new ArrayList<>();

        while(byteBuffer.remaining() > 0) {
            Message message;

            if (mUnfinishedMessage == null) {
                message = new Message();

                message.setType(byteBuffer.get());
                message.increaseBytesProcessed(1);

                if (byteBuffer.remaining() >= 4) {
                    message.setLength(byteBuffer.getInt());
                    message.increaseBytesProcessed(4);
                    readValueBytes(byteBuffer, message);
                } else if (byteBuffer.remaining() > 0) {
                    int bytesToRead = byteBuffer.remaining();
                    byte[] lengthBytes = message.getLengthBytes();
                    byteBuffer.get(lengthBytes, 0, bytesToRead);
                    message.increaseBytesProcessed(bytesToRead);
                }
            } else {
                message = mUnfinishedMessage;
                continueReadingLengthBytes(byteBuffer, message);
                readValueBytes(byteBuffer, message);
            }

            if (message.complete()) {
                completedMessages.add(message);
                mUnfinishedMessage = null;
            } else {
                mUnfinishedMessage = message;
            }
        }

        return completedMessages;
    }

    private void continueReadingLengthBytes(ByteBuffer byteBuffer, Message message) {
        int bytesAlreadyRead = message.getInputBytesProcessed();
        if (bytesAlreadyRead < 5) {
            // Read length bytes
            // Note that at least 1 byte must have been read at this point (the type)
            int missingLengthBytes = 4 - (message.getInputBytesProcessed() - 1);
            int lengthBytesArrayOffset = 4 - missingLengthBytes;
            int bytesToRead;

            if (byteBuffer.remaining() >= missingLengthBytes) {
                // We can read the rest of the length bytes now
                bytesToRead = missingLengthBytes;
                byteBuffer.get(message.getLengthBytes(), lengthBytesArrayOffset, missingLengthBytes);
                ByteBuffer lengthBuffer = ByteBuffer.wrap(message.getLengthBytes());
                message.setLength(lengthBuffer.getInt());
            } else {
                // We still don't have enough bytes to complete the length integer.
                bytesToRead = byteBuffer.remaining();
                byteBuffer.get(message.getLengthBytes(), lengthBytesArrayOffset, bytesToRead);
            }

            message.increaseBytesProcessed(bytesToRead);
        }
    }

    private void readValueBytes(ByteBuffer byteBuffer, Message message) {
        if (byteBuffer.remaining() > 0) {
            int numBytesToRead = byteBuffer.remaining() > message.getLength() ? message.getLength() : byteBuffer.remaining();
            byteBuffer.get(message.getValue(), 0, numBytesToRead);
            message.increaseBytesProcessed(numBytesToRead);
        }
    }
}
