package com.swirlwave.android.proxies;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;

import java.util.UUID;

public class FriendOnlineStatusUpdater implements Runnable {
    private Context mContext;
    private UUID mFriendId;
    private boolean mOnlineStatus;

    public FriendOnlineStatusUpdater(Context context, UUID friendId, boolean online) {
        mContext = context;
        mFriendId = friendId;
        mOnlineStatus = online;
    }

    @Override
    public void run() {
        try {
            Peer friend = PeersDb.selectByUuid(mContext, mFriendId);
            PeersDb.updateOnlineStatus(mContext, friend, mOnlineStatus);
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), "Couldn't update online status for friend: " + e.toString());
        }
    }
}
