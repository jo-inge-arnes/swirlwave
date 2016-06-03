package com.swirlwave.android.service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.R;
import com.swirlwave.android.socketserver.Server;

public final class SwirlwaveServiceHandler extends Handler {
    private static final String HIDDEN_SERVICE_ALREADY_REGISTERED_MSG_PREFIX = "Sorry, only one";
    private SwirlwaveService mSwirlwaveService;
    private SwirlwaveNotifications mSwirlwaveNotifications;
    private String mFileStorageLocation = "torfiles";
    private OnionProxyManager mOnionProxyManager;
    private String mOnionAddress;

    public SwirlwaveServiceHandler(SwirlwaveService swirlwaveService, Looper serviceLooper) {
        super(serviceLooper);
        mSwirlwaveService = swirlwaveService;
        mSwirlwaveNotifications = new SwirlwaveNotifications(swirlwaveService);
        mOnionProxyManager = new AndroidOnionProxyManager(mSwirlwaveService, mFileStorageLocation);
    }

    @Override
    public void handleMessage(Message msg) {
        if(!hasIntent(msg))
            return;

        switch(messageAction(msg)) {
            case ActionNames.ACTION_INIT_SERVICE:
                serviceInit();
                break;
            case ActionNames.ACTION_SHUT_DOWN_SERVICE:
                serviceShutdown(msg.arg1);
                break;
            case ActionNames.ACTION_CONNECTIVITY_CHANGE:
                handleConnectivityChange();
                break;
            default:
                break;
        }
    }

    private void serviceInit() {
        mSwirlwaveService.startForeground(SwirlwaveNotifications.SERVICE_NOTIFICATION_ID,
                mSwirlwaveNotifications.createStartupNotification());
    }

    private void serviceShutdown(int startId) {
        stopOnion();
        mSwirlwaveService.stopForeground(true);
        mSwirlwaveService.stopSelfResult(startId);
    }

    private String startOnion() {
        String onionAddress = "";

        try {
            stopOnion();
            if (mOnionProxyManager.startWithRepeat(240, 5)) {
                onionAddress = mOnionProxyManager.publishHiddenService(80, Server.PORT);
            } else {
                Log.e(mSwirlwaveService.getString(R.string.service_name), "Couldn't connect!");
            }
        } catch (Exception e) {
            Log.e(mSwirlwaveService.getString(R.string.service_name), e.toString());
        }

        return onionAddress;
    }

    private void stopOnion() {
        try {
            if(mOnionProxyManager != null) mOnionProxyManager.stop();
        } catch(Exception e) {
            Log.e(mSwirlwaveService.getString(R.string.service_name), "Error stopping: " +
                    e.getMessage());
        }
    }

    private void handleConnectivityChange() {
        if(hasInternetConnection()) {
            mSwirlwaveNotifications.notifyConnecting();
            mOnionAddress = startOnion();
            mSwirlwaveNotifications.notifyHasConnection(getHasConnectionMessage());
        } else {
            mSwirlwaveNotifications.notifyNoConnection();
            stopOnion();
        }
    }

    private String getHasConnectionMessage() {
        return getStartupFinishedMessage();
    }

    private String getStartupFinishedMessage() {
        return "".equals(mOnionAddress) ?
                mSwirlwaveService.getString(R.string.unavailable) : mOnionAddress;
    }

    private boolean hasIntent(Message msg) {
        return msg.obj != null &&
                msg.obj instanceof Intent;
    }

    private String messageAction(Message msg) {
        String action = ((Intent)msg.obj).getAction();
        return action == null ? "" : action;
    }

    private boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager)mSwirlwaveService
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
