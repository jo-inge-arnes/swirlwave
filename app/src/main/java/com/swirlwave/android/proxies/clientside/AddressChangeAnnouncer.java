package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.msopentech.thali.toronionproxy.Utilities;
import com.swirlwave.android.R;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ConnectionMessage;
import com.swirlwave.android.proxies.MessageType;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.sms.SmsSender;
import com.swirlwave.android.toast.Toaster;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddressChangeAnnouncer implements Runnable {
    private final Context mContext;
    private final LocalSettings mLocalSettings;
    private final String mPrivateKeyString;
    private final UUID mFriendId;
    private long mDelay;

    public AddressChangeAnnouncer(Context context, long delay, UUID friendId) throws Exception {
        mContext = context;
        mLocalSettings = new LocalSettings(mContext);
        Pair<String, String> keys = mLocalSettings.getAsymmetricKeys();
        mPrivateKeyString = keys.second;
        mDelay = delay;
        mFriendId = friendId;
   }

    @Override
    public void run() {
        try {
            Thread.sleep(mDelay);
            announceAddress(mFriendId);
        } catch (Exception e) {
            Toaster.show(mContext, mContext.getString(R.string.error_while_announcing_address_to_friends));
            Log.e(mContext.getString(R.string.service_name), "Error announcing address to friends: " + e.toString());
        }
    }

    /**
     * This method uses plain sockets without NIO. Friends are announced sequentialially.
     * Maybe rewrite this if neccessary. Runs in its own thread anyway.
     */
    private void announceAddress(UUID friendId) {
        int onionProxyPort = SwirlwaveOnionProxyManager.getsSocksPort();

        // Get current onion-address
        String currentAddress = SwirlwaveOnionProxyManager.getAddress();

        if (currentAddress == null || "".equals(currentAddress)) {
            Toaster.show(mContext, mContext.getString(R.string.will_not_announce_address_to_friend_because_it_is_empty));
            Log.i(mContext.getString(R.string.service_name), "Will not announce address to friends because address is empty!");
            return;
        }

        List<Peer> friends;
        if (friendId == null) {
            friends = PeersDb.selectAllPeers(mContext);
        } else {
            friends = new ArrayList<>();
            friends.add(PeersDb.selectByUuid(mContext, friendId));
        }

        Toaster.show(mContext, mContext.getString(R.string.announcing_address_to_friends));

        // Send message to friends
        for (Peer friend : friends) {
            String friendAddress = friend.getAddress();

            if (friendAddress == null || "".equals(friendAddress)) {
                Toaster.show(mContext, friend.getName() + " " + mContext.getString(R.string.has_an_empty_address));
                Log.e(mContext.getString(R.string.service_name), friend.getName() + " has an empty address.");
                continue;
            }

            try (Socket socket = Utilities.socks4aSocketConnection(friendAddress, SwirlwaveOnionProxyManager.HIDDEN_SERVICE_PORT, "127.0.0.1", onionProxyPort)) {
                // Read random bytes from server
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                int randomNumber = dataInputStream.readInt();

                // Create address change message
                byte[] message = generateAddressAnnouncementMessage(randomNumber, currentAddress);

                // Send address change message
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeInt(message.length);
                dataOutputStream.write(message);

                // Read response code
                byte responseCode = dataInputStream.readByte();

                // Got contact. If the friend was waiting for an answer, the it has been given now.
                if (friend.isAwaitingAnswerFromFallbackProtocol()) {
                    PeersDb.updateAwaitingAnswerFromFallbackProtocol(mContext, friend, false);
                }

                if (responseCode == (byte)0x0a) {
                    Toaster.show(mContext, friend.getName() + " " + mContext.getString(R.string.accepted_address_announcement));
                    Log.i(mContext.getString(R.string.service_name), friend.getName() + " accepted address announcement.");

                    if (!friend.getOnlineStatus()) {
                        PeersDb.updateOnlineStatus(mContext, friend, true);
                        Toaster.show(mContext, friend.getName() + " " + mContext.getString(R.string.is_online));
                        Log.i(mContext.getString(R.string.service_name), friend.getName() + " is now registered as online.");
                    }
                } else {
                    Toaster.show(mContext, mContext.getString(R.string.friend_rejected_address_announcement) + " " + friend.getName());
                    Log.i(mContext.getString(R.string.service_name), "Friend rejected address announcement message " + friend.getName());
                }
            } catch (Exception e) {
                Toaster.show(mContext, mContext.getString(R.string.failed_announcing_address_to) + " " + friend.getName());
                Log.i(mContext.getString(R.string.service_name), "Address could not be announced to friend, " + friend.getName() + ": " + e.toString());

                if (friend.isAwaitingAnswerFromFallbackProtocol()) {
                    try {
                        Toaster.show(mContext, friend.getName() + " " + mContext.getString(R.string.is_awaiting_answer_from_fallback_protocol));
                        Log.i(mContext.getString(R.string.service_name), friend.getName() + " is awaiting answer from fallback protocol.");

                        PeersDb.updateAwaitingAnswerFromFallbackProtocol(mContext, friend, false);

                        new Thread(new SmsSender(mContext, friend.getSecondaryChannelAddress(), SwirlwaveOnionProxyManager.getAddress())).start();
                    } catch (Exception e2) {
                        Toaster.show(mContext, mContext.getString(R.string.failure_answering_fallback_protocol_during_address_announcement_to) + " " + friend.getName());
                        Log.i(mContext.getString(R.string.service_name), "Failure answering fallback protocol to friend during address announcement to " + friend.getName() + ": " + e2.toString());
                    }
                }
            }
        }
    }

    private byte[] generateAddressAnnouncementMessage(int randomBytesFromServer, String address) throws Exception {
        ConnectionMessage message = new ConnectionMessage();
        message.setSenderId(mLocalSettings.getUuid());
        message.setRandomNumber(randomBytesFromServer);
        message.setMessageType(MessageType.ADDRESS_ANNOUNCEMENT);
        message.setDestination(UUID.randomUUID());
        message.setSystemMessage(address.getBytes(StandardCharsets.UTF_8));
        return message.toByteArray(mPrivateKeyString);
    }
}
