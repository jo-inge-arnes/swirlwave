package com.swirlwave.android.tor;

import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.proxies.serverside.ServerSideProxy;

public class SwirlwaveOnionProxyManager {
    public static final int HIDDEN_SERVICE_PORT = 9344;
    private String mFileStorageLocationPrefix = "tor_files_";
    private OnionProxyManager mOnionProxyManager;
    private static String sOnionAddress = "";
    private static int sSocksPort = -1;
    private Context mContext;

    public SwirlwaveOnionProxyManager(Context context) {
        mContext = context;
    }

    public static String getAddress() {
        return sOnionAddress;
    }

    public static int getsSocksPort() {
        return sSocksPort;
    }

    public void start(String fileFriendlyNetworkName) throws Exception {
        stop();

        mOnionProxyManager = new AndroidOnionProxyManager(
                mContext,
                mFileStorageLocationPrefix + fileFriendlyNetworkName);

        if (mOnionProxyManager.startWithRepeat(240, 5)) {
            sSocksPort = mOnionProxyManager.getIPv4LocalHostSocksPort();
            sOnionAddress = mOnionProxyManager.publishHiddenService(HIDDEN_SERVICE_PORT, ServerSideProxy.PORT);
        }
    }

    public void stop() throws Exception {
        sOnionAddress = "";
        sSocksPort = -1;

        if (mOnionProxyManager != null)
            mOnionProxyManager.stop();
    }
}
