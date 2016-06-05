package com.swirlwave.android.tor;

import android.content.Context;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.swirlwave.android.socketserver.Server;

public class ProxyManager {
    private String mFileStorageLocation = "tor_files";
    private OnionProxyManager mOnionProxyManager;
    private String mOnionAddress;
    private Context mContext;

    public ProxyManager(Context context) {
        mContext = context;
    }

    public String getAddress() {
        return mOnionAddress;
    }

    // TODO: Legg til sjekk for å se om IP-adressen har endret seg siden sist tjenesten var koblet til TOR. Hvis endret: Slett gammel katalog, slik at det blir generert ny onion-adresse. Deretter: Lagre ny IP i basen, koble til og informer venner om ny adresse (forsøk i rekkefølge --> Koble til siste kjente onion-adresse, spørre felles venner om adresse, i verste fall sende en SMS med ny adresse)
    // Hva med å endre mFileStorageLocation etter hvilken IP som er aktiv... Så kan man gjenbruke tidligere brukte ip-adresser (husk å kombinere med nettverksnavn, da)
    public void start() throws Exception {
        stop();

        mOnionProxyManager = new AndroidOnionProxyManager(mContext, mFileStorageLocation);
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
