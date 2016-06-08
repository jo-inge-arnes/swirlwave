package com.swirlwave.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class LocalInfo {
    private static final String APP_PREFS = "SwirlwavePreferences";
    private static final String APP_PREFS_INITIALIZED = "PreferencesInitialized";
    private static final String APP_ID_MOST_SIGNIFICANT_BITS = "IdMostSignificantBits";
    private static final String APP_ID_LEAST_SIGNIFICANT_BITS = "IdLeastSignificant";
    
    private SharedPreferences mSharedPreferences;
    private UUID mId;

    public LocalInfo(Context context) {
        mSharedPreferences = context.getSharedPreferences(APP_PREFS, Activity.MODE_PRIVATE);
        ensurePreferencesExist();
    }

    public void ensurePreferencesExist() {
        if (!mSharedPreferences.getBoolean(APP_PREFS_INITIALIZED, false)) {
            SharedPreferences.Editor editor = mySharedPreferences.edit();
            
            UUID uuid = UUID.randomUUID();
            editor.putLong(APP_ID_MOST_SIGNIFICANT_BITS, uuid.getMostSignificantBits());
            editor.putLong(APP_ID_LEAST_SIGNIFICANT_BITS, uuid.getLeastSignificantBits());
            editor.putBoolean(APP_PREFS_INITIALIZED, true);
            editor.apply();
            
            mId = uuid;
        }
    }

    public UUID getId() {
        if (mId == null) {
            mId = new UUID(
                mSharedPreferences.getLong(APP_ID_MOST_SIGNIFICANT_BITS, 0L),
                mSharedPreferences.getLong(APP_ID_LEAST_SIGNIFICANT_BITS, 0L));
        }
        
        return mId;
    }
}
