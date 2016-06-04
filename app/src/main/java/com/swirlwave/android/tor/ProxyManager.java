package com.swirlwave.android.tor;

import android.content.Context;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.R;
import com.swirlwave.android.socketserver.Server;

public class ProxyManager {
    private String mFileStorageLocation = "tor_files";
    private OnionProxyManager mOnionProxyManager;
    private String mOnionAddress;

    public ProxyManager(Context context) {
        mOnionProxyManager = new AndroidOnionProxyManager(context, mFileStorageLocation);
    }

    public String getAddress() {
        return mOnionAddress;
    }

    // TODO: Legg til sjekk for å se om IP-adressen har endret seg siden sist tjenesten var koblet til TOR. Hvis endret: Slett gammel katalog, slik at det blir generert ny onion-adresse. Deretter: Lagre ny IP i basen, koble til og informer venner om ny adresse (forsøk i rekkefølge --> Koble til siste kjente onion-adresse, spørre felles venner om adresse, i verste fall sende en SMS med ny adresse)
    public void start() throws Exception {
        mOnionAddress = "";

        stop();

        if (mOnionProxyManager.startWithRepeat(240, 5)) {
            mOnionAddress = mOnionProxyManager.publishHiddenService(80, Server.PORT);
        }
    }

    public void stop() throws Exception {
        if(mOnionProxyManager != null) mOnionProxyManager.stop();
    }
}
