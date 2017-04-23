package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.msopentech.thali.toronionproxy.Utilities;
import com.swirlwave.android.R;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.proxies.ConnectionMessage;
import com.swirlwave.android.proxies.MessageType;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class AddressChangeAnnouncer implements Runnable {
    private final Context mContext;
    private final LocalSettings mLocalSettings;
    private final String mPrivateKeyString;
    private long mDelay;

    public AddressChangeAnnouncer(Context context, long delay) throws Exception {
        mContext = context;
        mLocalSettings = new LocalSettings(mContext);
        Pair<String, String> keys = mLocalSettings.getAsymmetricKeys();
        mPrivateKeyString = keys.second;
        mDelay = delay;
   }

    @Override
    public void run() {
        try {
            Thread.sleep(mDelay);
            announceAddresses();
        } catch (Exception e) {
        }
    }

    /**
     * This method uses plain sockets without NIO. Friends are announced sequentialially.
     * Maybe rewrite this if neccessary. Runs in its own thread anyway.
     */
    public void announceAddresses() {
        int onionProxyPort = SwirlwaveOnionProxyManager.getsSocksPort();

        // Get current onion-address
        String currentAddress = SwirlwaveOnionProxyManager.getAddress();

        // Get addresses of all friends
        List<String> friendAddresses = PeersDb.selectAllFriendAddresses(mContext);

        // Send message to friends
        for (String friendAddress : friendAddresses) {
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
                dataOutputStream.flush();

                // Read response code
                byte responseCode = dataInputStream.readByte();

                if (responseCode != (byte)0x5a) {
                    Log.e(mContext.getString(R.string.service_name), "Friend rejected address change message");
                }
            } catch (Exception e) {
                Log.e(mContext.getString(R.string.service_name), "Address could not be announced to friend: " + e.toString());
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
