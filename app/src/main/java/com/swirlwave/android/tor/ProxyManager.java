package com.swirlwave.android.tor;

import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.socketserver.Server;

public class ProxyManager {
    private String mFileStorageLocationPrefix = "tor_files_";
    private OnionProxyManager mOnionProxyManager;
    private String mOnionAddress;
    private Context mContext;

    public ProxyManager(Context context) {
        mContext = context;
    }

    public String getAddress() {
        return mOnionAddress;
    }

    public void start(String fileFriendlyNetworkName) throws Exception {
        stop();

        mOnionProxyManager = new AndroidOnionProxyManager(
                mContext,
                mFileStorageLocationPrefix + fileFriendlyNetworkName);
        if (mOnionProxyManager.startWithRepeat(240, 5)) {
            mOnionAddress = mOnionProxyManager.publishHiddenService(80, Server.PORT);
        }
    }

    public void stop() throws Exception {
        mOnionAddress = "";
        if(mOnionProxyManager != null) {
            mOnionProxyManager.stop();
        }
    }
}
