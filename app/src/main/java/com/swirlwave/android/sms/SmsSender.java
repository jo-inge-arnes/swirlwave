package com.swirlwave.android.sms;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SmsSender implements Runnable {
    private static final short SMS_PORT = 6739;

    private Context mContext;
    private String mPhone;
    private String mAddress;

    public SmsSender(Context context, String friendPhone, String address) {
        mContext = context;
        mPhone = friendPhone;
    }

    @Override
    public void run() {
        sendSms();
    }

    public void sendSms() {
        try {
            String messageText = mAddress.replace(".onion", "");
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendDataMessage(mPhone, null, SMS_PORT, messageText.getBytes(), null, null);
        } catch (Exception e) {
            Toast.makeText(mContext, "Error sending SMS " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
