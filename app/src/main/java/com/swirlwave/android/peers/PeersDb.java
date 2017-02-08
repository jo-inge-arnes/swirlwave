package com.swirlwave.android.peers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.database.DatabaseOpenHelper;

import java.util.UUID;

public class PeersDb {
    public static final String TABLE_NAME = "peers";

    public static final String ID_COLUMN = "_id";
    public static final String NAME_COLUMN = "name";
    public static final String UUID_COLUMN = "uuid";
    public static final String PUBLIC_KEY_COLUMN = "public_key";
    public static final String PHONE_NUMBER_COLUMN = "phone_number";
    public static final String LAST_KNOWN_ADDRESS_COLUMN = "last_known_address";

    public static final String CREATE_TABLE = "create table " +
            TABLE_NAME + " (" +
            ID_COLUMN + " integer primary key autoincrement, " +
            NAME_COLUMN + " text not null, " +
            UUID_COLUMN + " text not null, " +
            PUBLIC_KEY_COLUMN + " text not null, " +
            PHONE_NUMBER_COLUMN + " text not null, " +
            LAST_KNOWN_ADDRESS_COLUMN + " text not null);";

    public static final String SELECT_ALL = "select " +
            ID_COLUMN + "," +
            NAME_COLUMN + ", " +
            UUID_COLUMN + ", " +
            PUBLIC_KEY_COLUMN + ", " +
            PHONE_NUMBER_COLUMN + ", " +
            LAST_KNOWN_ADDRESS_COLUMN + " " +
            "from " +
            TABLE_NAME;

    public static final String SELECT_WHERE_UUID = SELECT_ALL +
            " where " +
            UUID_COLUMN +
            " = ?";

    public static long insert(Context context, Peer peer) {
        ContentValues values = new ContentValues();
        values.put(NAME_COLUMN, peer.getName());
        values.put(UUID_COLUMN, peer.getUuid().toString());
        values.put(PUBLIC_KEY_COLUMN, peer.getPublicKey());
        values.put(PHONE_NUMBER_COLUMN, peer.getPhoneNumber());
        values.put(LAST_KNOWN_ADDRESS_COLUMN, peer.getLastKnownAddress());

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
        values.put(UUID_COLUMN, peer.getUuid().toString());
        values.put(PUBLIC_KEY_COLUMN, peer.getPublicKey());
        values.put(PHONE_NUMBER_COLUMN, peer.getPhoneNumber());
        values.put(LAST_KNOWN_ADDRESS_COLUMN, peer.getLastKnownAddress());

        String peerIdString = new Long(peer.getId()).toString();

        return DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .update(TABLE_NAME, values, ID_COLUMN + " = ?", new String[] { peerIdString });
    }

    public static Cursor selectAll(Context context) {
        return DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .rawQuery(SELECT_ALL, null);
    }

    public static Peer selectByUuid(Context context, UUID uuid) {
        Peer peer = null;

        try (Cursor cursor = DatabaseOpenHelper.getInstance(context)
                .getWritableDatabase()
                .rawQuery(SELECT_WHERE_UUID, new String[] { uuid.toString() })) {

            if (cursor.moveToFirst()) {
                peer = new Peer(
                        cursor.getLong(cursor.getColumnIndex(ID_COLUMN)),
                        cursor.getString(cursor.getColumnIndex(NAME_COLUMN)),
                        UUID.fromString(cursor.getString(cursor.getColumnIndex(UUID_COLUMN))),
                        cursor.getString(cursor.getColumnIndex(PUBLIC_KEY_COLUMN)),
                        cursor.getString(cursor.getColumnIndex(PHONE_NUMBER_COLUMN)),
                        cursor.getString(cursor.getColumnIndex(LAST_KNOWN_ADDRESS_COLUMN))
                );
            }
        } catch (Exception e) {
            Log.e(context.getString(R.string.app_name), "Error selecting peer by uuid: " + e.getMessage());
        }

        return peer;
    }
}
