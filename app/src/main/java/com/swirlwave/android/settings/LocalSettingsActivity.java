package com.swirlwave.android.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.swirlwave.android.R;

public class LocalSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_settings);
    }

    public void proceedButtonClicked(View view) {
        savePhoneNumber();
        finish();
    }

    private void savePhoneNumber() {
        String phoneNumber = getPhoneNumberEditText();
        try {
            LocalSettings localSettings = new LocalSettings(this);
            localSettings.setPhoneNumber(phoneNumber);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "Error opening local settings: " + e.getMessage());
            Toast.makeText(this, R.string.local_settings_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private String getPhoneNumberEditText() {
        EditText phoneEditText = (EditText) findViewById(R.id.phoneEditText);
        return phoneEditText.getText().toString();
    }
}
