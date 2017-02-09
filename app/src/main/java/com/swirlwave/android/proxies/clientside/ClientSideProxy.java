package com.swirlwave.android.proxies.clientside;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;

import java.nio.ByteBuffer;

public class ClientSideProxy implements Runnable {
    public static final int PORT = 9345;
    private Context mContext;
    private volatile boolean mRunning = true;
    private final ByteBuffer mBuffer = ByteBuffer.allocate(16384);

    public ClientSideProxy(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try {
            while (mRunning) {

            }
        } catch (Exception e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }
    }

    public void terminate() {
        mRunning = false;
    }
}
