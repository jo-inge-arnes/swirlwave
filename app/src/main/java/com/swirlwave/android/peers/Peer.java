package com.swirlwave.android.peers;

import android.content.Context;

import com.swirlwave.android.R;
import com.swirlwave.android.toast.Toaster;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Peer {
    public static final long DEFAULT_ID = -1;

    private long mId;
    private String mName;
    private UUID mPeerId;
    private String mPublicKey;
    private String mAddress;
    private int mAddressVersion;
    private boolean mOnlineStatus;
    private Date mLastContactTime;
    private String mSecondaryChannelAddress;
    private List<UUID> mKnownFriends;
    private String mCapabilities;
    private boolean mAwaitingAnswerFromFallbackProtocol;

    public Peer() {
        mId = DEFAULT_ID;
    }

    public Peer(long id) {
        mId = id;
    }

    public Peer(long id, String name, UUID uuid, String publicKey, String address, int addressVersion, boolean onlineStatus, Date lastContactTime, String secondaryChannelAddress, List<UUID> knownFriends, String capabilities, boolean awaitingAnswerFromFallbackProtocol) {
        mId = id;
        mName = name;
        mPeerId = uuid;
        mPublicKey = publicKey;
        mAddress = address;
        mAddressVersion = addressVersion;
        mOnlineStatus = onlineStatus;
        mLastContactTime = lastContactTime;
        mSecondaryChannelAddress = secondaryChannelAddress;
        mKnownFriends = knownFriends;
        mCapabilities = capabilities;
        mAwaitingAnswerFromFallbackProtocol = awaitingAnswerFromFallbackProtocol;
    }

    public Peer(String name, UUID uuid, String publicKey, String address, int addressVersion, boolean onlineStatus, Date lastContactTime, String secondaryChannelAddress, List<UUID> knownFriends, String capabilities, boolean awaitingAnswerFromFallbackProtocol) {
        this(DEFAULT_ID, name, uuid, publicKey, address, addressVersion, onlineStatus, lastContactTime, secondaryChannelAddress, knownFriends, capabilities, awaitingAnswerFromFallbackProtocol);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public UUID getPeerId() {
        return mPeerId;
    }

    public void setPeerId(UUID id) {
        mPeerId = id;
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

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public void setAddress(Context context, String address) {
        if (mAddress != null && !mAddress.equals(address)) {
            StringBuilder sb = new StringBuilder();
            sb.append(getName());
            sb.append(" ");
            sb.append(context.getString(R.string.has_changed_address));

            Toaster.show(context, sb.toString());
        }

        mAddress = address;
    }

    public int getAddressVersion() {
        return mAddressVersion;
    }

    public void setAddressVersion(int addressVersion) {
        mAddressVersion = addressVersion;
    }

    public boolean getOnlineStatus() {
        return mOnlineStatus;
    }

    public void setOnlineStatus(boolean onlineStatus) {
        mOnlineStatus = onlineStatus;
    }

    public void setOnlineStatus(Context context, boolean onlineStatus) {
        mOnlineStatus = onlineStatus;

        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" ");
        if (onlineStatus)
            sb.append(context.getString(R.string.is_online));
        else
            sb.append(context.getString(R.string.is_offline));

        Toaster.show(context, sb.toString());
    }

    public Date getLastContactTime() {
        return mLastContactTime;
    }

    public void setLastContactTime(Date lastContactTime) {
        mLastContactTime = lastContactTime;
    }

    public String getSecondaryChannelAddress() {
        return mSecondaryChannelAddress;
    }

    public void setSecondaryChannelAddress(String secondaryChannelAddress) {
        mSecondaryChannelAddress = secondaryChannelAddress;
    }

    public List<UUID> getKnownFriends() {
        return mKnownFriends;
    }

    public void setKnownFriends(List<UUID> knownFriends) {
        this.mKnownFriends = knownFriends;
    }

    public String getCapabilities() {
        return mCapabilities;
    }

    public void setCapabilities(String capabilities) {
        this.mCapabilities = capabilities;
    }

    public boolean isAwaitingAnswerFromFallbackProtocol() {
        return mAwaitingAnswerFromFallbackProtocol;
    }

    public void setAwaitingAnswerFromFallbackProtocol(boolean awaitingAnswerFromFallbackProtocol) {
        mAwaitingAnswerFromFallbackProtocol = awaitingAnswerFromFallbackProtocol;
    }

    public void setAwaitingAnswerFromFallbackProtocol(Context context, boolean awaitingAnswerFromFallbackProtocol) {
        mAwaitingAnswerFromFallbackProtocol = awaitingAnswerFromFallbackProtocol;

        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" ");
        if (awaitingAnswerFromFallbackProtocol)
            sb.append(context.getString(R.string.is_now_awaiting_answer_for_fallback_protocol));
        else
            sb.append(context.getString(R.string.is_no_longer_awaiting_answer_for_fallback_protocol));

        Toaster.show(context, sb.toString());
    }
}