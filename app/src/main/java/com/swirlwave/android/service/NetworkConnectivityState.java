package com.swirlwave.android.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

class NetworkConnectivityState {
    private Context mContext;
    private boolean mConnected;
    private boolean mWifi; // Maybe this should be int mNetworkType instead
    private String mNetworkName = "";
    private String mIp = "";

    public NetworkConnectivityState(Context context) {
        mContext = context;
    }

    public boolean refresh() {
        boolean wasChanged = false;

        ConnectivityManager cm = (ConnectivityManager)(mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE));
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if(mConnected != hasInternetConnection(activeNetwork)) {
            mConnected = !mConnected;
            wasChanged = true;
        }

        if(mConnected) {
            if(mWifi != isWifi(activeNetwork)) {
                mWifi = !mWifi;
                wasChanged = true;
            }

            String freshIp = getFreshIp();
            if(!mIp.equals(freshIp)) {
                mIp = freshIp;
                wasChanged = true;
            }

            String freshNetworkName = getFreshNetworkName(activeNetwork);
            if(!mNetworkName.equals(freshNetworkName)) {
                mNetworkName = freshNetworkName;
                wasChanged = true;
            }
        }

        return wasChanged;
    }

    private String getFreshNetworkName(NetworkInfo activeNetwork) {
        return activeNetwork.getExtraInfo();
    }

    private boolean hasInternetConnection(NetworkInfo activeNetwork) {
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public boolean isConnected() {
        return mConnected;
    }

    private boolean isWifi(NetworkInfo activeNetwork) {
        return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private String getFreshIp() {
        if(mWifi)
            return getWifiIp();
        else
            return getMobileIp();
    }

    private String getWifiIp() {
            try {
                WifiManager wifiManager = (WifiManager) mContext.getSystemService(mContext.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                        (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                        (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
            } catch (Exception ex) {
                return "";
            }
    }

    private String getMobileIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            return "";
        }
        return "";
    }
}

