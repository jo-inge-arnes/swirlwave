package com.swirlwave.android.socketserver;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server implements Runnable {
    private Context mContext;
    private ServerSocket mServerSocket;
    private volatile boolean mRunning = true;

    public static final int PORT = 9345;

    public Server(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try (mServerSocket = new ServerSocket(PORT)) {
            while(mRunning) {
                try {
                    Socket socket = mServerSocket.accept();
                    ServerThread thread = new ServerThread(mContext, socket);
                    thread.run();
                } catch(Exception e) {
                    Log.e(mContext.getString(R.string.service_name), e.toString());
                }
            }
        } catch(Exception e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }
    }

    public void terminate() {
        mRunning = false;
        if(mServerSocket != null)
            mServerSocket.close();
    }
}
