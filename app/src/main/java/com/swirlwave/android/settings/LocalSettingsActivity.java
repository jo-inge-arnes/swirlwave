package com.swirlwave.android.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.swirlwave.android.R;

import java.util.Locale;

public class LocalSettingsActivity extends AppCompatActivity {

    private EditText mPhoneEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_settings);
        mPhoneEditText = (EditText) findViewById(R.id.phoneEditText);
    }

    public void proceedButtonClicked(View view) {
        boolean wasSaved = savePhoneNumber();
        if (wasSaved) finish();
    }

    private boolean savePhoneNumber() {
        boolean success;

        try {
            String phoneNumber = getValidatedPhoneNumberText();
            LocalSettings localSettings = new LocalSettings(this);
            localSettings.setPhoneNumber(phoneNumber);
            success = true;
        } catch (PhoneNumberValidationException pnve) {
            mPhoneEditText.setError(pnve.getExplanation());
            success = false;
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "Error opening local settings: " + e.getMessage());
            Toast.makeText(this, R.string.local_settings_unavailable, Toast.LENGTH_LONG).show();
            success = false;
        }

        return success;
    }

    private String getValidatedPhoneNumberText() throws PhoneNumberValidationException {
        String phoneNumberString = mPhoneEditText.getText().toString();

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Locale locale = Locale.getDefault();
        Phonenumber.PhoneNumber phoneNumber;

        try {
            phoneNumber = phoneUtil.parse(phoneNumberString, locale.getCountry());
        } catch (NumberParseException e) {
            throw new PhoneNumberValidationException(
                    getString(R.string.phone_number_invalid_format));
        }

        if (!phoneUtil.isValidNumber(phoneNumber)) {
            throw new PhoneNumberValidationException(
                    getString(R.string.phone_number_invalid_format));
        }

        return phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    private class PhoneNumberValidationException extends Exception {
        private final String mExplanation;

        public String getExplanation() {
            return mExplanation;
        }

        public PhoneNumberValidationException(String explanation) {
            mExplanation = explanation;
        }
    }
}
