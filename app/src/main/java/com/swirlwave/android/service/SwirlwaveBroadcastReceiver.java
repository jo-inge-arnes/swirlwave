package com.swirlwave.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

class SwirlwaveBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")){
            Intent serviceIntent = new Intent(context, SwirlwaveService.class);
            serviceIntent.setAction(ActionNames.ACTION_CONNECTIVITY_CHANGE);
            context.startService(serviceIntent);
        }
    }
}
