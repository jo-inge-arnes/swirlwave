package com.swirlwave.android.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.clientside.AddressChangeAnnouncer;
import com.swirlwave.android.proxies.serverside.FriendAddressUpdater;
import com.swirlwave.android.service.SwirlwaveService;
import com.swirlwave.android.toast.Toaster;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

public class SmsReceiver extends BroadcastReceiver {
    public SmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Get the data (SMS data) bound to intent
            Bundle bundle = intent.getExtras();

            SmsMessage[] msgs = null;

            if (bundle != null) {
                // Retrieve the Binary SMS data
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];

                // For every SMS message received (although multipart is not supported with binary)
                for (int i = 0; i < msgs.length; i++) {
                    byte[] data = null;

                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                    Peer friend = PeersDb.selectByPhoneNumber(context, msgs[i].getOriginatingAddress());
                    if (friend != null) {
                        // Return the User Data section minus the
                        // User Data Header (UDH) (if there is any UDH at all)
                        data = msgs[i].getUserData();

                        String str = "";
                        for (int index = 0; index < data.length; index++) {
                            str += Character.toString((char) data[index]);
                        }
                        str += ".onion";

                        str = FriendAddressUpdater.validateAddressFormat(str);

                        if (str != null) {
                            friend.setAddress(context, str);
                            friend.setOnlineStatus(context, true);
                            friend.setAwaitingAnswerFromFallbackProtocol(context, true);
                            PeersDb.update(context, friend);

                            if (SwirlwaveService.isRunning() && !SwirlwaveOnionProxyManager.getAddress().equals("")) {
                                // Send message back to friend if the Swirlwave service is currently running.
                                // Don't have to wait long, assuming that service is already ready.
                                try {
                                    new Thread(new AddressChangeAnnouncer(context, 100, friend.getPeerId())).start();
                                } catch (Exception e) {
                                    Log.e(context.getString(R.string.app_name), "Couldn't send address as part of SMS fallback protocol: " + e.toString());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error while processing received SMS: " + e.toString());
        }
    }
}