package com.swirlwave.android.socketserver;

import android.content.Context;
import android.util.Log;

import com.swirlwave.android.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ServerThread extends Thread {
    private Socket mSocket;
    private Context mContext;

    public ServerThread(Context context, Socket socket) {
        mSocket = socket;
        mContext = context;
    }

    @Override
    public void run() {
        try (
                PrintWriter out = new PrintWriter(mSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        mSocket.getInputStream()));
        ) {
            String inputLine;

            while (!(inputLine = in.readLine()).equals("")) {
                Log.i(mContext.getString(R.string.service_name), inputLine);
            }

            out.println("HTTP/1.1 200 OK");
            out.println("Connection: Closed");
            out.println("");

            mSocket.close();
        } catch (IOException e) {
            Log.e(mContext.getString(R.string.service_name), e.toString());
        }
    }
}
