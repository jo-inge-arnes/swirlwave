package com.swirlwave.android.serversideproxy;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;

public class MessageManagerTest {
    @Test
    public void processBytes_one_complete_message() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put((byte)16);
        byteBuffer.putInt(5);
        for (byte b = 0; b < 5; b++) {
            byteBuffer.put(b);
        }
        byteBuffer.flip();

        MessageManager messageManager = new MessageManager();

        List<Message> messages = messageManager.processBytes(byteBuffer);
        Message unfinishedMessage = messageManager.getUnfinishedMessage();

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).complete());
        assertEquals((byte)16, messages.get(0).getType());
        assertEquals(5, messages.get(0).getLength());
        assertEquals(5, messages.get(0).getValue().length);

        for (byte b = 0; b < 5; b++) {
            assertEquals(b, messages.get(0).getValue()[b]);
        }

        assertNull(unfinishedMessage);
    }
}
