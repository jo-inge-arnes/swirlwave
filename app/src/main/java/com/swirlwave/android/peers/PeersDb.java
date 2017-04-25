package com.swirlwave.android.peers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.database.DatabaseOpenHelper;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PeersDb {
    public static final String TABLE_NAME = "peers";

    public static final String ID_COLUMN = "_id";
    public static final String NAME_COLUMN = "name";
    public static final String PEER_ID_COLUMN = "peer_id";
    public static final String PUBLIC_KEY_COLUMN = "public_key";
    public static final String ADDRESS_COLUMN = "address";
    public static final String ADDRESS_VERSION_COLUMN = "address_version";
    public static final String ONLINE_STATUS_COLUMN = "online_status";
    public static final String LAST_CONTACT_TIME = "last_contact_time";
    public static final String SECONDARY_CHANNEL_ADDRESS_COLUMN = "secondary_channel_address";
    public static final String KNOWN_FRIENDS_COLUMN = "known_friends";
    public static final String CAPABILITIES_COLUMN = "capabilities";
    public static final String AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN = "awaiting_answer_from_fallback_protocol";

    public static final String CREATE_TABLE = "create table " +
            TABLE_NAME + " (" +
            ID_COLUMN + " integer primary key autoincrement, " +
            NAME_COLUMN + " text not null, " +
            PEER_ID_COLUMN + " text not null, " +
            PUBLIC_KEY_COLUMN + " text not null, " +
            ADDRESS_COLUMN + " text not null, " +
            ADDRESS_VERSION_COLUMN + " text not null, " +
            ONLINE_STATUS_COLUMN + " text not null, " +
            LAST_CONTACT_TIME + " text not null, " +
            SECONDARY_CHANNEL_ADDRESS_COLUMN + " text not null, " +
            KNOWN_FRIENDS_COLUMN + " text not null, " +
            CAPABILITIES_COLUMN + " text not null, " +
            AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN + " text not null);";

    public static final String SELECT_ALL = "select " +
            ID_COLUMN + "," +
            NAME_COLUMN + ", " +
            PEER_ID_COLUMN + ", " +
            PUBLIC_KEY_COLUMN + ", " +
            ADDRESS_COLUMN + ", " +
            ADDRESS_VERSION_COLUMN + ", " +
            ONLINE_STATUS_COLUMN + ", " +
            LAST_CONTACT_TIME + ", " +
            SECONDARY_CHANNEL_ADDRESS_COLUMN + ", " +
            KNOWN_FRIENDS_COLUMN + ", " +
            CAPABILITIES_COLUMN + ", " +
            AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN + " " +
            "from " +
            TABLE_NAME;

    public static final String SELECT_WHERE_PEER_ID = SELECT_ALL +
            " where " +
            PEER_ID_COLUMN +
            " = ?";

    private static final String SELECT_WHERE_SECONDARY_CHANNEL_ADDRESS = SELECT_ALL +
            " where " +
            SECONDARY_CHANNEL_ADDRESS_COLUMN +
            " = ?";

    public static final String SELECT_ALL_ADDRESSES = "select " + ADDRESS_COLUMN + " from " + TABLE_NAME;

    private static final String SELECT_ALL_PEER_IDS = "select " + PEER_ID_COLUMN + " from " + TABLE_NAME;

    private static final String ONLINE = "Online";
    private static final String OFFLINE = "Offline";
    private static final String YES = "Yes";
    private static final String NO = "No";

    public static long insert(Context context, Peer peer) {
        ContentValues values = new ContentValues();
        values.put(NAME_COLUMN, peer.getName());
        values.put(PEER_ID_COLUMN, peer.getPeerId().toString());
        values.put(PUBLIC_KEY_COLUMN, peer.getPublicKey());
        values.put(ADDRESS_COLUMN, peer.getAddress());
        values.put(ADDRESS_VERSION_COLUMN, peer.getAddressVersion());
        values.put(ONLINE_STATUS_COLUMN, peer.getOnlineStatus() ? ONLINE : OFFLINE);
        values.put(LAST_CONTACT_TIME, Long.toString(peer.getLastContactTime().getTime()));
        values.put(SECONDARY_CHANNEL_ADDRESS_COLUMN, peer.getSecondaryChannelAddress());
        values.put(KNOWN_FRIENDS_COLUMN, toString(peer.getKnownFriends()));
        values.put(CAPABILITIES_COLUMN, peer.getCapabilities());
        values.put(AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN, peer.isAwaitingAnswerFromFallbackProtocol() ? YES : NO);

        long id = DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase().insert(TABLE_NAME, null, values);

        peer.setId(id);

//        context.getContentResolver().notifyChange()

        return id;
    }

    public static int update(Context context, Peer peer) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, peer.getId());
        values.put(NAME_COLUMN, peer.getName());
        values.put(PEER_ID_COLUMN, peer.getPeerId().toString());
        values.put(PUBLIC_KEY_COLUMN, peer.getPublicKey());
        values.put(ADDRESS_COLUMN, peer.getAddress());
        values.put(ADDRESS_VERSION_COLUMN, peer.getAddressVersion());
        values.put(ONLINE_STATUS_COLUMN, peer.getOnlineStatus() ? ONLINE : OFFLINE);
        values.put(LAST_CONTACT_TIME, Long.toString(peer.getLastContactTime().getTime()));
        values.put(SECONDARY_CHANNEL_ADDRESS_COLUMN, peer.getSecondaryChannelAddress());
        values.put(KNOWN_FRIENDS_COLUMN, toString(peer.getKnownFriends()));
        values.put(CAPABILITIES_COLUMN, peer.getCapabilities());
        values.put(AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN, peer.isAwaitingAnswerFromFallbackProtocol() ? YES : NO);

        String peerIdString = new Long(peer.getId()).toString();

        return DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .update(TABLE_NAME, values, ID_COLUMN + " = ?", new String[] { peerIdString });
    }

    public static void updateOnlineStatus(Context context, Peer peer, boolean online) {
        String peerIdString = new Long(peer.getId()).toString();
        ContentValues values = new ContentValues();
        values.put(ONLINE_STATUS_COLUMN, online ? ONLINE : OFFLINE);

        DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .update(TABLE_NAME, values, ID_COLUMN + " = ?", new String[] { peerIdString });

        peer.setOnlineStatus(context, online);
    }

    public static void updateAwaitingAnswerFromFallbackProtocol(Context context, Peer peer, boolean awaitingFallbackProtocol) {
        String peerIdString = new Long(peer.getId()).toString();
        ContentValues values = new ContentValues();
        values.put(AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN, awaitingFallbackProtocol ? YES : NO);

        DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .update(TABLE_NAME, values, ID_COLUMN + " = ?", new String[] { peerIdString });

        peer.setAwaitingAnswerFromFallbackProtocol(context, awaitingFallbackProtocol);
    }

    public static Cursor selectAll(Context context) {
        return DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .rawQuery(SELECT_ALL, null);
    }

    public static List<String> selectAllFriendAddresses(Context context) {
        List<String> addresses = new ArrayList<>();

        try (Cursor cursor = DatabaseOpenHelper.getInstance(context).getWritableDatabase().rawQuery(SELECT_ALL_ADDRESSES, null)) {
            while (cursor.moveToNext()) {
                addresses.add(cursor.getString(cursor.getColumnIndex(ADDRESS_COLUMN)));
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error selecting friend addresses: " + e.getMessage());
        }

        return addresses;
    }

    public static Peer selectByUuid(Context context, UUID uuid) {
        Peer peer = null;

        try (Cursor cursor = DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .rawQuery(SELECT_WHERE_PEER_ID, new String[] { uuid.toString() })) {

            if (cursor.moveToFirst()) {
                peer = getPeer(cursor);
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error selecting peer by uuid: " + e.getMessage());
        }

        return peer;
    }

    public static Peer selectByPhoneNumber(Context context, String phoneNumber) {
        Peer peer = null;

        try (Cursor cursor = DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .rawQuery(SELECT_WHERE_SECONDARY_CHANNEL_ADDRESS, new String[] { phoneNumber })) {

            if (cursor.moveToFirst()) {
                peer = getPeer(cursor);
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error selecting peer by phone number: " + e.getMessage());
        }

        return peer;
    }

    public static List<UUID> selectAllFriendUuids(Context context) {
        List<UUID> uuids = new ArrayList<>();

        try (Cursor cursor = DatabaseOpenHelper.getInstance(context).getWritableDatabase().rawQuery(SELECT_ALL_PEER_IDS, null)) {
            while (cursor.moveToNext()) {
                uuids.add(UUID.fromString(cursor.getString(cursor.getColumnIndex(PEER_ID_COLUMN))));
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error selecting friend uuids: " + e.getMessage());
        }

        return uuids;
    }

    public static List<Peer> selectAllPeers(Context context) {
        List<Peer> peers = new ArrayList<>();

        try (Cursor cursor = selectAll(context)) {
            while (cursor.moveToNext()) {
                peers.add(getPeer(cursor));
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error selecting all peers: " + e.getMessage());
        }

        return peers;
    }

    private static String toString(List<UUID> knownFriends) {
        if (knownFriends == null || knownFriends.size() == 0)
            return "";

        return StringUtils.join(knownFriends, ',');
    }

    @NonNull
    private static Peer getPeer(Cursor cursor) {
        return new Peer(
                cursor.getLong(cursor.getColumnIndex(ID_COLUMN)),
                cursor.getString(cursor.getColumnIndex(NAME_COLUMN)),
                UUID.fromString(cursor.getString(cursor.getColumnIndex(PEER_ID_COLUMN))),
                cursor.getString(cursor.getColumnIndex(PUBLIC_KEY_COLUMN)),
                cursor.getString(cursor.getColumnIndex(ADDRESS_COLUMN)),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(ADDRESS_VERSION_COLUMN))),
                cursor.getString(cursor.getColumnIndex(ONLINE_STATUS_COLUMN)).equals(ONLINE),
                new Date(Long.parseLong(cursor.getString(cursor.getColumnIndex(LAST_CONTACT_TIME)))),
                cursor.getString(cursor.getColumnIndex(SECONDARY_CHANNEL_ADDRESS_COLUMN)),
                parseKnownFriends(cursor.getString(cursor.getColumnIndex(KNOWN_FRIENDS_COLUMN))),
                cursor.getString(cursor.getColumnIndex(CAPABILITIES_COLUMN)),
                cursor.getString(cursor.getColumnIndex(AWAITING_ANSWER_FROM_FALLBACK_PROTOCOL_COLUMN)).equals(YES)
        );
    }

    private static List<UUID> parseKnownFriends(String knownFriendsString) {
        List<UUID> uuids = new ArrayList<>();

        if (knownFriendsString == null || knownFriendsString.equals(""))
            return uuids;

        for (String uuidString : StringUtils.split(knownFriendsString, ',')) {
            uuids.add(UUID.fromString(uuidString));
        }

        return uuids;
    }
}
