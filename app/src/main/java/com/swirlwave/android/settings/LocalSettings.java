package com.swirlwave.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
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
    private static final String APP_PHONE_NUMBER = "PhoneNumber";

    private SharedPreferences mSharedPreferences;
    private UUID mUuid;
    private Pair<String, String> mAsymmetricKeys;
    private String mPhoneNumber;

    public LocalSettings(Context context) throws Exception {
        mSharedPreferences = context.getSharedPreferences(APP_PREFS, Activity.MODE_PRIVATE);
        ensurePreferencesExist(context);
    }

    public void ensurePreferencesExist(Context context) throws Exception {
        if (!mSharedPreferences.getBoolean(APP_PREFS_INITIALIZED, false)) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            
            UUID uuid = UUID.randomUUID();
            editor.putLong(APP_ID_MOST_SIGNIFICANT_BITS, uuid.getMostSignificantBits());
            editor.putLong(APP_ID_LEAST_SIGNIFICANT_BITS, uuid.getLeastSignificantBits());

            Pair<String, String> keys = AsymmetricEncryption.generateKeys();
            editor.putString(keys.first, APP_PUBLIC_KEY);
            editor.putString(keys.second, APP_PRIVATE_KEY);

            TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNumber = telephonyManager.getLine1Number();
            editor.putString(phoneNumber, APP_PHONE_NUMBER);

            editor.putBoolean(APP_PREFS_INITIALIZED, true);
            editor.apply();
            
            mUuid = uuid;
            mAsymmetricKeys = keys;
            mPhoneNumber = phoneNumber;
        }
    }

    public UUID getUuid() {
        if (mUuid == null) {
            mUuid = new UUID(
                mSharedPreferences.getLong(APP_ID_MOST_SIGNIFICANT_BITS, 0L),
                mSharedPreferences.getLong(APP_ID_LEAST_SIGNIFICANT_BITS, 0L));
        }
        
        return mUuid;
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

    public String getPhoneNumber() {
        if (mPhoneNumber == null) {
            mPhoneNumber = mSharedPreferences.getString(APP_PHONE_NUMBER, "");
        }

        return mPhoneNumber;
    }
}
