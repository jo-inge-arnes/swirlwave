package com.swirlwave.android.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;

import com.swirlwave.android.socketserver.Server;

public class SwirlwaveService extends Service {
    private static volatile boolean mIsRunning;
    private BroadcastReceiver mBroadcastReceiver;
    private SwirlwaveServiceHandler mServiceHandler;
    private static Server mServer;

    public static boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onCreate() {
        mIsRunning = true;
        registerBroadcastReceiver();
        startMessageHandler();
        startServer();
    }

    private void startServer() {
        mServer = new Server(this);
        Thread thread = new Thread(mServer);
        thread.start();
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
        mServer.terminate();
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
