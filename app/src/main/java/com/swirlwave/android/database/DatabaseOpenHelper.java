package com.swirlwave.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.swirlwave.android.peers.PeersDb;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static DatabaseOpenHelper sInstance;

    private static final String DATABASE_NAME = "swirlwave.db";
    private static final int DATABASE_VERSION = 1;

    private DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseOpenHelper getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DatabaseOpenHelper(context.getApplicationContext());
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PeersDb.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not yet implemented
    }
}
