package com.swirlwave.android.tor;

import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.proxies.serverside.ServerSideProxy;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.toast.Toaster;

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

        long onionProxyStartupFinished = 0;
        long onionProxyStartTime = System.currentTimeMillis();
        if (mOnionProxyManager.startWithRepeat(240, 5)) {
            sSocksPort = mOnionProxyManager.getIPv4LocalHostSocksPort();
            sOnionAddress = mOnionProxyManager.publishHiddenService(
                    HIDDEN_SERVICE_PORT, ServerSideProxy.LISTENING_PORT);

            onionProxyStartupFinished = System.currentTimeMillis();

            LocalSettings localSettings = new LocalSettings(mContext);
            if (!localSettings.getAddress().equals(sOnionAddress)) {
                localSettings.setAddress(sOnionAddress);
                Integer newVersion = Integer.parseInt(localSettings.getAddressVersion()) + 1;
                localSettings.setAddressVersion(newVersion.toString());
            }
        }

        Toaster.show(mContext, String.format("Started Onion Proxy in %s ms.", onionProxyStartupFinished - onionProxyStartTime));
    }

    public void stop() throws Exception {
        sOnionAddress = "";
        sSocksPort = -1;

        if (mOnionProxyManager != null)
            mOnionProxyManager.stop();
    }
}
