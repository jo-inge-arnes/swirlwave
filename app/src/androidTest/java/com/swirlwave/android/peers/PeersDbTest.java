//package com.swirlwave.android.peers;
//
//import android.content.Context;
//import android.support.test.InstrumentationRegistry;
//import android.support.test.runner.AndroidJUnit4;
//import android.test.RenamingDelegatingContext;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.UUID;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotEquals;
//
//@RunWith(AndroidJUnit4.class)
//public class PeersDbTest {
//    Context mMockContext;
//
//    @Before
//    public void setUp() {
//        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
//    }
//
//    @Test
//    public void when_inserting_then_peer_should_beStoredWithAllValuesAndDbIdReturned() throws Exception {
//        UUID uuid = UUID.randomUUID();
//        Peer peer = new Peer("test", uuid, "some-public-key", "+4798360000", "some-onion-address");
//        long dbId = PeersDb.insert(mMockContext, peer);
//        assertNotEquals(-1, dbId);
//    }
//
//    @Test
//    public void when_selecting_by_uuid_then_correct_peer_should_be_retrieved() throws Exception {
//        UUID uuid = UUID.randomUUID();
//        Peer peer = new Peer("test", uuid, "some-public-key", "+4798360000", "some-onion-address");
//        long dbId = PeersDb.insert(mMockContext, peer);
//
//        Peer returnedPeer = PeersDb.selectByUuid(mMockContext, uuid);
//        assertEquals(dbId, returnedPeer.getId());
//    }
//
//    @Test
//    public void when_updating_then_correct_peer_should_be_updated() throws Exception {
//        UUID uuid = UUID.randomUUID();
//        Peer peer = new Peer("test", uuid, "some-public-key", "+4798360000", "some-onion-address");
//        long dbId = PeersDb.insert(mMockContext, peer);
//
//        UUID uuid2 = UUID.randomUUID();
//        Peer peer2 = new Peer("test2", uuid2, "some-public-key", "+4798360000", "some-onion-address");
//        long dbId2 = PeersDb.insert(mMockContext, peer2);
//
//        peer2.setName("name_updated_ok");
//        peer2.setPublicKey("key_updated_ok");
//        peer2.setAddress("address_updated_ok");
//        peer2.setSecondaryChannelAddress("phone_updated_ok");
//        PeersDb.update(mMockContext, peer2);
//
//        Peer selectedPeer = PeersDb.selectByUuid(mMockContext, uuid);
//        assertEquals(dbId, peer.getId());
//        assertEquals(peer.getName(), selectedPeer.getName());
//        assertEquals(peer.getPublicKey(), selectedPeer.getPublicKey());
//        assertEquals(peer.getSecondaryChannelAddress(), selectedPeer.getSecondaryChannelAddress());
//        assertEquals(peer.getAddress(), selectedPeer.getAddress());
//
//        selectedPeer = PeersDb.selectByUuid(mMockContext, uuid2);
//        assertEquals(dbId2, selectedPeer.getId());
//        assertEquals("name_updated_ok", selectedPeer.getName());
//        assertEquals("key_updated_ok", selectedPeer.getPublicKey());
//        assertEquals("phone_updated_ok", selectedPeer.getSecondaryChannelAddress());
//        assertEquals("address_updated_ok", selectedPeer.getAddress());
//    }
//}
