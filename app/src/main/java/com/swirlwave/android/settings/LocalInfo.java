package com.swirlwave.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class LocalInfo {
    private final String APP_PREFS = "SwirlwavePreferences";
    private final String APP_ID = "SwirlwavePreferencesUuid";
    private SharedPreferences mSharedPreferences;

    public LocalInfo(Context context) {
        mSharedPreferences = context.getSharedPreferences(APP_PREFS, Activity.MODE_PRIVATE);
    }

    public void ensurePreferencesExist() {

    }

    public UUID getId() {
        return mSharedPreferences.getString(APP_ID, )
    }
}
