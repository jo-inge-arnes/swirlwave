package com.swirlwave.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import com.swirlwave.android.crypto.AsymmetricEncryption;

import java.util.UUID;

public class LocalSettings {
    private static final String APP_PREFS = "SwirlwavePreferences";
    private static final String APP_PREFS_INITIALIZED = "PreferencesInitialized";
    private static final String APP_ID_MOST_SIGNIFICANT_BITS = "IdMostSignificantBits";
    private static final String APP_ID_LEAST_SIGNIFICANT_BITS = "IdLeastSignificant";
    private static final String APP_PRIVATE_KEY = "Private";
    private static final String APP_PUBLIC_KEY = "Public";

    private SharedPreferences mSharedPreferences;
    private UUID mId;
    private Pair<String, String> mAsymmetricKeys;

    public LocalSettings(Context context) throws Exception {
        mSharedPreferences = context.getSharedPreferences(APP_PREFS, Activity.MODE_PRIVATE);
        ensurePreferencesExist();
    }

    public void ensurePreferencesExist() throws Exception {
        if (!mSharedPreferences.getBoolean(APP_PREFS_INITIALIZED, false)) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            
            UUID uuid = UUID.randomUUID();
            editor.putLong(APP_ID_MOST_SIGNIFICANT_BITS, uuid.getMostSignificantBits());
            editor.putLong(APP_ID_LEAST_SIGNIFICANT_BITS, uuid.getLeastSignificantBits());

            Pair<String, String> keys = AsymmetricEncryption.generateKeys();
            editor.putString(keys.first, APP_PUBLIC_KEY);
            editor.putString(keys.second, APP_PRIVATE_KEY);

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

    public Pair<String, String> getAsymmetricKeys() {
        if (mAsymmetricKeys == null) {
            mAsymmetricKeys = new Pair<>(
                    mSharedPreferences.getString(APP_PUBLIC_KEY, ""),
                    mSharedPreferences.getString(APP_PRIVATE_KEY, "")
            );
        }

        return mAsymmetricKeys;
    }
}
