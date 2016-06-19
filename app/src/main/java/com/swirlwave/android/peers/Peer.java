package com.swirlwave.android.peers;

import java.util.UUID;

public class Peer {
    public static final long DEFAULT_ID = -1;

    private long mId;
    private String mName;
    private UUID mUuid;
    private String mPublicKey;
    private String mPhoneNumber;
    private String mLastKnownAddress;

    public Peer() {
        mId = DEFAULT_ID;
    }

    public Peer(long id) {
        mId = id;
    }

    public Peer(long id, String name, UUID uuid, String publicKey, String phoneNumber, String lastKnownAddress) {
        mId = id;
        mName = name;
        mUuid = uuid;
        mPublicKey = publicKey;
        mPhoneNumber = phoneNumber;
        mLastKnownAddress = lastKnownAddress;
    }

    public Peer(String name, UUID uuid, String publicKey, String phoneNumber, String lastKnownAddress) {
        this(DEFAULT_ID, name, uuid, publicKey, phoneNumber, lastKnownAddress);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public UUID getUuid() {
        return mUuid;
    }

    public void setUuid(UUID id) {
        mUuid = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    public String getLastKnownAddress() {
        return mLastKnownAddress;
    }

    public void setLastKnownAddress(String lastKnownAddress) {
        mLastKnownAddress = lastKnownAddress;
    }
}
