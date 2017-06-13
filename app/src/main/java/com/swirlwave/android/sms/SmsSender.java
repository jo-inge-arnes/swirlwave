package com.swirlwave.android.sms;

import android.content.Context;
import android.telephony.SmsManager;

import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.toast.Toaster;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class SmsSender implements Runnable {
    private static final short SMS_PORT = 6739;

    private Context mContext;
    private UUID mFriendId;


    public SmsSender(Context context, UUID friendId) {
        mContext = context;
        mFriendId = friendId;
    }

    @Override
    public void run() {
        sendSms();
    }

    public synchronized void sendSms() {
        try {
            Peer friend = PeersDb.selectByUuid(mContext, mFriendId);
            String address = friend.getAddress();
            String phone = friend.getSecondaryChannelAddress();

            if (StringUtils.isEmpty(address)) {
                Toaster.show(mContext, String.format(mContext.getString(R.string.friend_address_is_empty), friend.getName()));
            } else if (StringUtils.isNotEmpty(friend.getSecondaryChannelAddress())) {
                Toaster.show(mContext, String.format(mContext.getString(R.string.friend_phone_is_empty), friend.getName()));
            } else if (!friend.getOnlineStatus()) {
                Toaster.show(mContext, String.format(mContext.getString(R.string.friend_already_registered_as_offline), friend.getName()));
            } else {
                PeersDb.updateOnlineStatus(mContext, friend, false);
                Toaster.show(mContext, mContext.getString(R.string.sending_sms_to) + " " + phone);
                String messageText = address.replace(".onion", "");
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendDataMessage(phone, null, SMS_PORT, messageText.getBytes(), null, null);
            }
        } catch (Exception e) {
            Toaster.show(mContext, mContext.getString(R.string.error_sending_sms) + ": " + e.getMessage());
        }
    }
}
