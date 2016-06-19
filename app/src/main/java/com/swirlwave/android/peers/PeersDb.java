package com.swirlwave.android.peers;

import android.content.ContentValues;
import android.content.Context;

import com.swirlwave.android.database.DatabaseOpenHelper;

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

        return id;
    }
}
