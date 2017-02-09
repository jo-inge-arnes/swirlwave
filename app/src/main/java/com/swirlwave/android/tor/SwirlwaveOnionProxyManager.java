package com.swirlwave.android.tor;

import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.proxies.serverside.ServerSideProxy;

public class SwirlwaveOnionProxyManager {
    private String mFileStorageLocationPrefix = "tor_files_";
    private OnionProxyManager mOnionProxyManager;
    private static String sOnionAddress = "";
    private Context mContext;

    public SwirlwaveOnionProxyManager(Context context) {
        mContext = context;
    }

    public static String getAddress() {
        return sOnionAddress;
    }

    public void start(String fileFriendlyNetworkName) throws Exception {
        stop();

        mOnionProxyManager = new AndroidOnionProxyManager(
                mContext,
                mFileStorageLocationPrefix + fileFriendlyNetworkName);

        if (mOnionProxyManager.startWithRepeat(240, 5))
            sOnionAddress = mOnionProxyManager.publishHiddenService(80, ServerSideProxy.PORT);
    }

    public void stop() throws Exception {
        sOnionAddress = "";

        if (mOnionProxyManager != null)
            mOnionProxyManager.stop();
    }
}
