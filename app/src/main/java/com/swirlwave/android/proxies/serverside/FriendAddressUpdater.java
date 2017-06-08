package com.swirlwave.android.proxies.serverside;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;

import java.util.UUID;

public class FriendAddressUpdater implements Runnable {
    private Context mContext;
    private UUID mFriendUuid;
    private String mAddress;

    public FriendAddressUpdater(Context context, UUID friendUuid, String address) {
        mContext = context;
        mFriendUuid = friendUuid;
        mAddress = address;
    }

    @Override
    public void run() {
        try {
            mAddress = validateAddressFormat(mAddress);

            if (mAddress == null) {
                Log.e(mContext.getString(R.string.service_name), "Invalid address format received from friend");
                return;
            }

            Peer friend = PeersDb.selectByUuid(mContext, mFriendUuid);

            if (!friend.getOnlineStatus()) {
                friend.setOnlineStatus(mContext, true);
                friend.setAddress(mContext, mAddress);
                PeersDb.update(mContext, friend);
            } else if (!friend.getAddress().equals(mAddress)) {
                friend.setAddress(mContext, mAddress);
                PeersDb.update(mContext, friend);
            }
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), "Couldn't update friend " + e.toString());
        }
    }

    public static String validateAddressFormat(String address) {
        if (address == null)
            return null;

        address = address.trim();

        if (!address.endsWith(".onion"))
            return null;

        if (address.length() < 7)
            return null;

        return address;
    }
}
