package com.swirlwave.android.service;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.swirlwave.android.R;
import com.swirlwave.android.proxies.clientside.AddressChangeAnnouncer;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

final class SwirlwaveServiceHandler extends Handler {
    private SwirlwaveService mSwirlwaveService;
    private SwirlwaveNotifications mSwirlwaveNotifications;
    private NetworkConnectivityState mConnectivityState;
    private SwirlwaveOnionProxyManager mSwirlwaveOnionProxyManager;

    public SwirlwaveServiceHandler(SwirlwaveService swirlwaveService, Looper serviceLooper) {
        super(serviceLooper);
        mSwirlwaveService = swirlwaveService;
        mSwirlwaveNotifications = new SwirlwaveNotifications(swirlwaveService);
        mSwirlwaveOnionProxyManager = new SwirlwaveOnionProxyManager(mSwirlwaveService);
        mConnectivityState = new NetworkConnectivityState(mSwirlwaveService);
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
        stopOnionProxy();
        mSwirlwaveService.stopForeground(true);
        mSwirlwaveService.stopSelfResult(startId);
    }

    private void startOnionProxy(String fileFriendlyNetworkName) {
        try {
            mSwirlwaveOnionProxyManager.start(fileFriendlyNetworkName);

            if ("".equals(SwirlwaveOnionProxyManager.getAddress())) {
                Log.e(mSwirlwaveService.getString(R.string.service_name), "Couldn't connect!");
                return;
            } else {
                mSwirlwaveService.startProxies();
            }
        } catch (Exception e) {
            Log.e(mSwirlwaveService.getString(R.string.service_name), e.toString());
        }
    }

    private void stopOnionProxy() {
        try {
            mSwirlwaveOnionProxyManager.stop();
        } catch (Exception e) {
            Log.e(mSwirlwaveService.getString(R.string.service_name), "Error stopping: " +
                    e.getMessage());
        }
    }

    private void handleConnectivityChange() {
        boolean networkStatusChanged = mConnectivityState.refresh();

        if (mConnectivityState.isConnected()) {
            if (networkStatusChanged) {
                mSwirlwaveNotifications.notifyConnecting();
                startOnionProxy(mConnectivityState.getFileFriendlyLocationName());
                mSwirlwaveNotifications.notifyHasConnection(getHasConnectionMessage());

                if (mConnectivityState.locationHasChanged()) {
                    // TODO: Update version of address i local settings
                    Log.i(mSwirlwaveService.getString(R.string.service_name),
                            "New Location: " + mConnectivityState.getFileFriendlyLocationName());
                }

                try {
                    Thread thread = new Thread(new AddressChangeAnnouncer(mSwirlwaveService.getApplicationContext(), 1000, null));
                    thread.start();
                } catch (Exception e) {
                    Log.e(mSwirlwaveService.getString(R.string.service_name), "Couldn't announce new address to friends: " +  e.toString());
                }
            }
        } else {
            mSwirlwaveNotifications.notifyNoConnection();
            stopOnionProxy();
        }
    }

    private String getHasConnectionMessage() {
        return getStartupFinishedMessage();
    }

    private String getStartupFinishedMessage() {
        return "".equals(SwirlwaveOnionProxyManager.getAddress()) ?
                mSwirlwaveService.getString(R.string.unavailable) : SwirlwaveOnionProxyManager.getAddress();
    }

    private boolean hasIntent(Message msg) {
        return msg.obj != null &&
                msg.obj instanceof Intent;
    }

    private String messageAction(Message msg) {
        String action = ((Intent)msg.obj).getAction();
        return action == null ? "" : action;
    }
}
