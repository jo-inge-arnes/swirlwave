package com.swirlwave.android.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

class NetworkConnectivityState {
    private static final int FILE_FRIENDLY_NAME_MAX_LENGTH = 50;
    private static final String FILE_FRIENDLY_NAME_ALLOWED_CHARS = "[^\\dA-Za-z]";
    private Context mContext;
    private boolean mConnected;
    private boolean mWifi; // Maybe this should be "int mNetworkType" instead
    private String mNetworkName = "";
    private String mIp = "";
    private String mFileFriendlyName = "";
    private String mFileFriendlyNamePrevious = "";

    public NetworkConnectivityState(Context context) {
        mContext = context;
    }

    public static String generateFileFriendlyLocationName(String network, String address) {
        String name = String.format("%s%s", address, network)
                .replaceAll(FILE_FRIENDLY_NAME_ALLOWED_CHARS, "")
                .toLowerCase();

        if (name.length() > FILE_FRIENDLY_NAME_MAX_LENGTH)
            name = name.substring(0, FILE_FRIENDLY_NAME_MAX_LENGTH);

        return name;
    }

    public boolean refresh() {
        boolean wasChanged = false;

        mFileFriendlyNamePrevious = mFileFriendlyName;

        ConnectivityManager cm = (ConnectivityManager) (mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE));
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (mConnected != hasInternetConnection(activeNetwork)) {
            mConnected = !mConnected;
            wasChanged = true;
        }

        if (mConnected) {
            if (mWifi != isWifi(activeNetwork)) {
                mWifi = !mWifi;
                wasChanged = true;
            }

            String freshIp = getFreshIp(mWifi);
            if (!mIp.equals(freshIp)) {
                mIp = freshIp;
                wasChanged = true;
            }

            String freshNetworkName = getFreshNetworkName(activeNetwork);
            if (!mNetworkName.equals(freshNetworkName)) {
                mNetworkName = freshNetworkName;
                wasChanged = true;
            }
        }

        if (wasChanged)
            mFileFriendlyName = generateFileFriendlyLocationName(mNetworkName, mIp);

        return wasChanged;
    }

    /**
     * After a refresh, this can be checked to see if the location has changed since last time.
     * @return True if the last call to refresh() caused the location name to change
     */
    public boolean locationHasChanged() {
        return !mFileFriendlyName.equals(mFileFriendlyNamePrevious);
    }

    public boolean isConnected() {
        return mConnected;
    }

    public String getFileFriendlyLocationName() {
        return mFileFriendlyName;
    }

    private boolean hasInternetConnection(NetworkInfo activeNetwork) {
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private String getFreshNetworkName(NetworkInfo activeNetwork) {
        String extraInfo = activeNetwork.getExtraInfo();
        return extraInfo == null ? "" : extraInfo;
    }

    private boolean isWifi(NetworkInfo activeNetwork) {
        return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private String getFreshIp(boolean isWifi) {
        if (isWifi)
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
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
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

