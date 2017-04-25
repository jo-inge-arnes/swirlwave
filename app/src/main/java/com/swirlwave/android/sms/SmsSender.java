package com.swirlwave.android.sms;

import android.content.Context;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.swirlwave.android.R;
import com.swirlwave.android.toast.Toaster;

public class SmsSender implements Runnable {
    private static final short SMS_PORT = 6739;

    private Context mContext;
    private String mPhone;
    private String mAddress;

    public SmsSender(Context context, String friendPhone, String address) {
        mContext = context;
        mPhone = friendPhone;
        mAddress = address;
    }

    @Override
    public void run() {
        sendSms();
    }

    public void sendSms() {
        try {
            Toaster.show(mContext, mContext.getString(R.string.sending_sms_to) + " " + mPhone);
            String messageText = mAddress.replace(".onion", "");
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendDataMessage(mPhone, null, SMS_PORT, messageText.getBytes(), null, null);
        } catch (Exception e) {
            Toaster.show(mContext, mContext.getString(R.string.error_sending_sms) + ": " + e.getMessage());
        }
    }
}
