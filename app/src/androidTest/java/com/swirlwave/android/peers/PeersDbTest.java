package com.swirlwave.android.peers;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class PeersDbTest {
    Context mMockContext;

    @Before
    public void setUp() {
        mMockContext = new RenamingDelegatingContext(InstrumentationRegistry.getInstrumentation().getTargetContext(), "test_");
    }

    @Test
    public void when_inserting_then_peer_should_beStoredWithAllValuesAndDbIdReturned() throws Exception {
        UUID uuid = UUID.randomUUID();
        Peer peer = new Peer("test", uuid, "some-public-key", "+4798360000", "some-onion-address");
        long dbId = PeersDb.insert(mMockContext, peer);
        assertNotEquals(-1, dbId);
    }
}
