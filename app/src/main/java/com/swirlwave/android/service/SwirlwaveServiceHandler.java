package com.swirlwave.android.service;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.tor.ProxyManager;

public final class SwirlwaveServiceHandler extends Handler {
    private SwirlwaveService mSwirlwaveService;
    private SwirlwaveNotifications mSwirlwaveNotifications;
    private ProxyManager mProxyManager;

    public SwirlwaveServiceHandler(SwirlwaveService swirlwaveService, Looper serviceLooper) {
        super(serviceLooper);
        mSwirlwaveService = swirlwaveService;
        mSwirlwaveNotifications = new SwirlwaveNotifications(swirlwaveService);
        mProxyManager = new ProxyManager(mSwirlwaveService);
    }

    @Override
    public void handleMessage(Message msg) {
        if(!hasIntent(msg)) return;

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
        mSwirlwaveService.startForeground(
                SwirlwaveNotifications.SERVICE_NOTIFICATION_ID,
                mSwirlwaveNotifications.createStartupNotification());
    }

    private void serviceShutdown(int startId) {
        stopProxy();
        mSwirlwaveService.stopForeground(true);
        mSwirlwaveService.stopSelfResult(startId);
    }

    private void startProxy() {
        try {
            mProxyManager.start();

            if("".equals(mProxyManager.getAddress())) {
                Log.e(mSwirlwaveService.getString(R.string.service_name), "Couldn't connect!");
            }
        } catch (Exception e) {
            Log.e(mSwirlwaveService.getString(R.string.service_name), e.toString());
        }
    }

    private void stopProxy() {
        try {
            mProxyManager.stop();
        } catch(Exception e) {
            Log.e(mSwirlwaveService.getString(R.string.service_name), "Error stopping: " +
                    e.getMessage());
        }
    }

    private void handleConnectivityChange() {
        if(hasInternetConnection()) {
            mSwirlwaveNotifications.notifyConnecting();
            startProxy();
            mSwirlwaveNotifications.notifyHasConnection(getHasConnectionMessage());
        } else {
            mSwirlwaveNotifications.notifyNoConnection();
            stopProxy();
        }
    }

    private String getHasConnectionMessage() {
        return getStartupFinishedMessage();
    }

    private String getStartupFinishedMessage() {
        return "".equals(mProxyManager.getAddress()) ?
                mSwirlwaveService.getString(R.string.unavailable) : mProxyManager.getAddress();
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
