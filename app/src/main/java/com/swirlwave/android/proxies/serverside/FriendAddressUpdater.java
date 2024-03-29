package com.swirlwave.android.proxies.serverside;

import android.content.Context;
import android.util.Log;

import com.msopentech.thali.toronionproxy.Utilities;
import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import java.net.Socket;
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
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);

            mAddress = validateAddressFormat(mAddress);

            if (mAddress == null) {
                Log.e(mContext.getString(R.string.service_name), "Invalid address format received from friend");
                return;
            }

            updateFriendStatusValues();
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), "Couldn't update friend " + e.toString());
        }
    }

    private synchronized void updateFriendStatusValues() {
        Peer friend = PeersDb.selectByUuid(mContext, mFriendUuid);

        boolean friendValuesChanged = false;

        if (!friend.getOnlineStatus()) {
            friend.setOnlineStatus(mContext, true);
            friendValuesChanged = true;
        }

        if (!friend.getAddress().equals(mAddress)) {
            friend.setAddress(mContext, mAddress);
            friendValuesChanged = true;
        }

        if (friendValuesChanged) {
            PeersDb.update(mContext, friend);
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
