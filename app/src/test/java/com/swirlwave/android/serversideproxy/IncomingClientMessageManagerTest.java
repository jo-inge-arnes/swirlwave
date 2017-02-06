package com.swirlwave.android.serversideproxy;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.List;

public class IncomingClientMessageManagerTest {
    @Test
    public void processBytes_one_complete_message() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put((byte)16);
        byteBuffer.putInt(5);
        for (byte b = 0; b < 5; b++) {
            byteBuffer.put(b);
        }
        byteBuffer.flip();

        IncomingClientMessageManager incomingClientMessageManager = new IncomingClientMessageManager();

        List<IncomingClientMessage> messages = incomingClientMessageManager.processBytes(byteBuffer);
        IncomingClientMessage unfinishedMessage = incomingClientMessageManager.getUnfinishedMessage();

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

    @Test
    public void processBytes_two_complete_messages() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(25);

        byteBuffer.put((byte)16);
        byteBuffer.putInt(5);
        for (byte b = 0; b < 5; b++) {
            byteBuffer.put(b);
        }

        byteBuffer.put((byte)17);
        byteBuffer.putInt(10);
        for (byte b = 5; b < 15; b++) {
            byteBuffer.put(b);
        }

        byteBuffer.flip();

        IncomingClientMessageManager incomingClientMessageManager = new IncomingClientMessageManager();

        List<IncomingClientMessage> messages = incomingClientMessageManager.processBytes(byteBuffer);
        IncomingClientMessage unfinishedMessage = incomingClientMessageManager.getUnfinishedMessage();

        assertEquals(2, messages.size());

        assertTrue(messages.get(0).complete());
        assertEquals((byte)16, messages.get(0).getType());
        assertEquals(5, messages.get(0).getLength());
        assertEquals(5, messages.get(0).getValue().length);
        for (byte b = 0; b < 5; b++) {
            assertEquals(b, messages.get(0).getValue()[b]);
        }

        assertTrue(messages.get(1).complete());
        assertEquals((byte)17, messages.get(1).getType());
        assertEquals(10, messages.get(1).getLength());
        assertEquals(10, messages.get(1).getValue().length);
        for (byte b = 0; b < 10; b++) {
            assertEquals((byte)b+5, messages.get(1).getValue()[b]);
        }

        assertNull(unfinishedMessage);
    }

    @Test
    public void processBytes_one_unfinished_message_with_type_and_length() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(7);

        byteBuffer.put((byte)16);
        byteBuffer.putInt(5);
        byteBuffer.put((byte)255);
        byteBuffer.put((byte)254);
        byteBuffer.flip();

        IncomingClientMessageManager incomingClientMessageManager = new IncomingClientMessageManager();

        List<IncomingClientMessage> messages = incomingClientMessageManager.processBytes(byteBuffer);
        IncomingClientMessage unfinishedMessage = incomingClientMessageManager.getUnfinishedMessage();

        assertEquals(0, messages.size());
        assertNotNull(unfinishedMessage);

        assertEquals(16, unfinishedMessage.getType());
        assertEquals(7, unfinishedMessage.getInputBytesProcessed());
        assertNotNull(unfinishedMessage.getValue());
        assertEquals(5, unfinishedMessage.getValue().length);

        assertEquals((byte)255, unfinishedMessage.getValue()[0]);
        assertEquals((byte)254, unfinishedMessage.getValue()[1]);
        for (byte b = 2; b < 5; b++) {
            assertEquals(0, unfinishedMessage.getValue()[b]);
        }
    }

    @Test
    public void processBytes_one_message_in_two_parts() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(7);

        byteBuffer.put((byte)16);
        byteBuffer.putInt(5);
        byteBuffer.put((byte)0xff);
        byteBuffer.put((byte)0xfe);
        byteBuffer.flip();

        IncomingClientMessageManager incomingClientMessageManager = new IncomingClientMessageManager();
        incomingClientMessageManager.processBytes(byteBuffer);

        byteBuffer = ByteBuffer.allocate(3);
        byteBuffer.put((byte)0xfd);
        byteBuffer.put((byte)0xfc);
        byteBuffer.put((byte)0xfb);
        byteBuffer.flip();

        List<IncomingClientMessage> messages = incomingClientMessageManager.processBytes(byteBuffer);
        IncomingClientMessage unfinishedMessage = incomingClientMessageManager.getUnfinishedMessage();

        assertEquals(1, messages.size());
        assertNull(unfinishedMessage);

        assertEquals(16, messages.get(0).getType());
        assertEquals(10, messages.get(0).getInputBytesProcessed());
        assertEquals(5, messages.get(0).getValue().length);
        assertEquals((byte)0xff, messages.get(0).getValue()[0]);
        assertEquals((byte)0xfe, messages.get(0).getValue()[1]);
        assertEquals((byte)0xfd, messages.get(0).getValue()[2]);
        assertEquals((byte)0xfc, messages.get(0).getValue()[3]);
        assertEquals((byte)0xfb, messages.get(0).getValue()[4]);
    }

    @Test
    public void processBytes_one_complete_and_one_unfinished_in_two_parts() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(7);

        byteBuffer.put((byte)16);
        byteBuffer.putInt(5);
        byteBuffer.put((byte)0xff);
        byteBuffer.put((byte)0xfe);
        byteBuffer.flip();

        IncomingClientMessageManager incomingClientMessageManager = new IncomingClientMessageManager();
        incomingClientMessageManager.processBytes(byteBuffer);

        byteBuffer = ByteBuffer.allocate(6);
        byteBuffer.put((byte)0xfd);
        byteBuffer.put((byte)0xfc);
        byteBuffer.put((byte)0xfb);
        byteBuffer.put((byte)0xa0);
        byteBuffer.put((byte)0xa1);
        byteBuffer.put((byte)0xa2);
        byteBuffer.flip();

        List<IncomingClientMessage> messages = incomingClientMessageManager.processBytes(byteBuffer);
        IncomingClientMessage unfinishedMessage = incomingClientMessageManager.getUnfinishedMessage();

        assertEquals(1, messages.size());
        assertNotNull(unfinishedMessage);

        assertEquals(16, messages.get(0).getType());
        assertEquals(10, messages.get(0).getInputBytesProcessed());
        assertEquals(5, messages.get(0).getValue().length);
        assertEquals((byte)0xff, messages.get(0).getValue()[0]);
        assertEquals((byte)0xfe, messages.get(0).getValue()[1]);
        assertEquals((byte)0xfd, messages.get(0).getValue()[2]);
        assertEquals((byte)0xfc, messages.get(0).getValue()[3]);
        assertEquals((byte)0xfb, messages.get(0).getValue()[4]);

        assertEquals(3, unfinishedMessage.getInputBytesProcessed());
        assertNull(unfinishedMessage.getValue());
        assertEquals((byte)0xa0, unfinishedMessage.getType());
        assertEquals(-1, unfinishedMessage.getLength());
        assertEquals((byte)0xa1, unfinishedMessage.getLengthBytes()[0]);
        assertEquals((byte)0xa2, unfinishedMessage.getLengthBytes()[1]);
        assertEquals((byte)0x00, unfinishedMessage.getLengthBytes()[2]);
        assertEquals((byte)0x00, unfinishedMessage.getLengthBytes()[3]);
    }

    @Test
    public void processBytes_three_bytes_first_then_rest_of_message() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(3);

        byteBuffer.put((byte)16);
        byteBuffer.put((byte)0);
        byteBuffer.put((byte)0);
        byteBuffer.flip();

        IncomingClientMessageManager incomingClientMessageManager = new IncomingClientMessageManager();
        incomingClientMessageManager.processBytes(byteBuffer);

        byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.put((byte)0);
        byteBuffer.put((byte)2);
        byteBuffer.put((byte)10);
        byteBuffer.put((byte)11);
        byteBuffer.flip();

        List<IncomingClientMessage> messages = incomingClientMessageManager.processBytes(byteBuffer);
        IncomingClientMessage unfinishedMessage = incomingClientMessageManager.getUnfinishedMessage();

        assertEquals(1, messages.size());
        assertNull(unfinishedMessage);

        assertEquals(16, messages.get(0).getType());
        assertEquals(7, messages.get(0).getInputBytesProcessed());
        assertEquals(2, messages.get(0).getLength());
        assertEquals(2, messages.get(0).getValue().length);
        assertEquals((byte)10, messages.get(0).getValue()[0]);
        assertEquals((byte)11, messages.get(0).getValue()[1]);
    }
}
