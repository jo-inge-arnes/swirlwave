package com.swirlwave.android.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;

import com.swirlwave.android.proxies.clientside.ClientSideProxy;
import com.swirlwave.android.proxies.serverside.ServerSideProxy;

public class SwirlwaveService extends Service {
    private static volatile boolean mIsRunning;
    private static ClientSideProxy sClientSideProxy;
    private static ServerSideProxy sServerSideProxy;
    private BroadcastReceiver mBroadcastReceiver;
    private SwirlwaveServiceHandler mServiceHandler;

    public static boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onCreate() {
        mIsRunning = true;
        registerBroadcastReceiver();
        startMessageHandler();
    }

    public void startProxies() throws Exception {
        // This method is actually called by SwirlwaveServiceHandler's startOnionProxy-method,
        // because the proxies shouldn't be allowed to accept connections before onion proxy is
        // ready. Consider refactoring, so that this method is called as a result of an event, or
        // maybe move the responsibility for starting/stopping Swirlwave proxies altogether.

        if (sServerSideProxy == null) {
            sServerSideProxy = new ServerSideProxy(this);
            new Thread(sServerSideProxy).start();
        }

        if (sClientSideProxy == null) {
            sClientSideProxy = new ClientSideProxy(this);
            new Thread(sClientSideProxy).start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = obtainMessage(startId, intent);
        mServiceHandler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mIsRunning = false;
        sServerSideProxy.terminate();
        sClientSideProxy.terminate();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void registerBroadcastReceiver() {
        mBroadcastReceiver = new SwirlwaveBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void startMessageHandler() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceHandler = new SwirlwaveServiceHandler(this, thread.getLooper());
    }

    private Message obtainMessage(int startId, Intent intent) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        return msg;
    }
}
