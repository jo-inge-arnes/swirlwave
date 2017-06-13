package com.swirlwave.android.tor;

import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.proxies.serverside.ServerSideProxy;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.toast.Toaster;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Random;
import java.util.UUID;

public class SwirlwaveOnionProxyManager {
    public static final int HIDDEN_SERVICE_PORT = 9344;
    public static final int STARTUP_SECONDS_BEFORE_TIME_OUT = 45;
    public static final int STARTUP_NUMBER_OF_RETRIES = 2;
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

        // The following code is only used when not reusing hidden service addresses
        String dirName = mFileStorageLocationPrefix + "swirlwave";
        File dir = mContext.getDir(dirName, Context.MODE_PRIVATE);
        FileUtils.cleanDirectory(dir);

        File parentDir = dir.getParentFile();
        String filterString = String.format("^app_%s(?!swirlwave).*$", mFileStorageLocationPrefix);
        FileFilter filter = new RegexFileFilter(filterString);
        for (File file : parentDir.listFiles(filter)) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
        }

        mOnionProxyManager = new AndroidOnionProxyManager(mContext, dirName);

        // This code is for reusing hidden service addresses
//        mOnionProxyManager = new AndroidOnionProxyManager(
//                mContext,
//                mFileStorageLocationPrefix + fileFriendlyNetworkName);

        long onionProxyStartupFinished = 0;
        long onionProxyStartTime = System.currentTimeMillis();
        if (mOnionProxyManager.startWithRepeat(STARTUP_SECONDS_BEFORE_TIME_OUT, STARTUP_NUMBER_OF_RETRIES)) {
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
