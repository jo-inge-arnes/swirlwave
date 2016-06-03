package com.swirlwave.android.socketserver;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server implements Runnable {
    public final int SO_TIMEOUT = 30000;

    private Context mContext;
    private volatile boolean mRunning = true;

    public static final int PORT = 9345;

    public Server(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setSoTimeout(SO_TIMEOUT); // Limit the blocking time of the accept-call
            while(mRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    ServerThread thread = new ServerThread(mContext, socket);
                    thread.run();
                } catch(SocketTimeoutException ste) {
                    // Normal situation, just continue...
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
    }
}
