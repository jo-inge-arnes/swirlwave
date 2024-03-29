package com.swirlwave.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.swirlwave.android.R;
import com.swirlwave.android.crypto.AsymmetricEncryption;
import com.swirlwave.android.toast.Toaster;

import java.util.UUID;

/**
 * Information about the installation's settings, such as phone number, uuid, keys
 */
public class LocalSettings {
    private static final String APP_PREFS = "SwirlwavePreferences";
    private static final String APP_PREFS_INITIALIZED = "PreferencesInitialized";
    private static final String APP_ID_MOST_SIGNIFICANT_BITS = "IdMostSignificantBits";
    private static final String APP_ID_LEAST_SIGNIFICANT_BITS = "IdLeastSignificant";
    private static final String APP_PRIVATE_KEY = "Private";
    private static final String APP_PUBLIC_KEY = "Public";
    private static final String APP_PHONE_NUMBER = "PhoneNumber";
    private static final String APP_INSTALLATION_NAME = "InstallationName";
    private static final String APP_ADDRESS = "Address";
    private static final String APP_ADDRESS_VERSION = "AddressVersion";

    private SharedPreferences mSharedPreferences;
    private UUID mUuid;
    private Pair<String, String> mAsymmetricKeys;
    private String mPhoneNumber;
    private String mInstallationName;

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
            editor.putString(APP_PUBLIC_KEY, keys.first);
            editor.putString(APP_PRIVATE_KEY, keys.second);

            editor.putString(APP_PHONE_NUMBER, "");
            editor.putString(APP_INSTALLATION_NAME, "");

            editor.putString(APP_ADDRESS, "");
            editor.putString(APP_ADDRESS_VERSION, "0");

            editor.putBoolean(APP_PREFS_INITIALIZED, true);
            editor.apply();
            
            mUuid = uuid;
            mAsymmetricKeys = keys;
            mPhoneNumber = "";
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

    public void setPhoneNumber(String phoneNumber) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(APP_PHONE_NUMBER, phoneNumber);
        editor.apply();
        mPhoneNumber = phoneNumber;
    }

    public String getInstallationName() {
        if (mInstallationName == null) {
            mInstallationName = mSharedPreferences.getString(APP_INSTALLATION_NAME, "");
        }

        return mInstallationName;
    }

    public void setInstallationName(String installationName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(APP_INSTALLATION_NAME, installationName);
        editor.apply();
        mInstallationName = installationName;
    }

    public String getAddress() {
        return mSharedPreferences.getString(APP_ADDRESS, "");
    }

    public void setAddress(String address) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(APP_ADDRESS, address);
        editor.apply();
    }

    public String getAddressVersion() {
        return mSharedPreferences.getString(APP_ADDRESS_VERSION, "0");
    }

    public void setAddressVersion(String addressVersion) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(APP_ADDRESS_VERSION, addressVersion);
        editor.apply();
    }

    public String getCapabilities() {
        // TODO: Implement capabilites
        return "";
    }

    public static void ensureInstallationNameAndPhoneNumber(Context context) {
        LocalSettings localSettings;

        try {
            localSettings = new LocalSettings(context);
        } catch (Exception e) {
            localSettings = null;
            Log.e(context.getString(R.string.app_name), "Error opening local settings: " + e.getMessage());
            Toaster.show(context, context.getString(R.string.local_settings_unavailable));
        }

        if (localSettings != null) {
            if ("".equals(localSettings.getPhoneNumber()) || "".equals(localSettings.getInstallationName())) {
                localSettings.openLocalSettingsActivity(context);
            }
        }
    }

    public void openLocalSettingsActivity(Context context) {
        Intent intent = new Intent(context, LocalSettingsActivity.class);
        context.startActivity(intent);
    }
}
