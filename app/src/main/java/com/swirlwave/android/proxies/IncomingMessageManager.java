package com.swirlwave.android.proxies;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that accumulates chunks of bytes from client, and assembles them into messages.
 */
public class IncomingMessageManager {
    private IncomingMessage mUnfinishedMessage;

    public IncomingMessage getUnfinishedMessage() {
        return mUnfinishedMessage;
    }

    public List<IncomingMessage> processBytes(ByteBuffer byteBuffer) {
        List<IncomingMessage> completedMessages = new ArrayList<>();

        while(byteBuffer.remaining() > 0) {
            IncomingMessage message;

            if (mUnfinishedMessage == null) {
                message = new IncomingMessage();

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

    private void continueReadingLengthBytes(ByteBuffer byteBuffer, IncomingMessage message) {
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

    private void readValueBytes(ByteBuffer byteBuffer, IncomingMessage message) {
        if (byteBuffer.remaining() > 0) {
            int valueBytesOffset = message.getInputBytesProcessed() - 5;
            int maxLength = message.getLength() - valueBytesOffset;
            int numBytesToRead = byteBuffer.remaining() > maxLength ? maxLength : byteBuffer.remaining();
            byteBuffer.get(message.getValue(), valueBytesOffset, numBytesToRead);
            message.increaseBytesProcessed(numBytesToRead);
        }
    }
}
