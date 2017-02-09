package com.swirlwave.android;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;
import com.swirlwave.android.peers.Peer;
import com.swirlwave.android.peers.PeersDb;
import com.swirlwave.android.peers.PeersFragment;
import com.swirlwave.android.permissions.AppPermissions;
import com.swirlwave.android.permissions.AppPermissionsResult;
import com.swirlwave.android.service.ActionNames;
import com.swirlwave.android.service.SwirlwaveService;
import com.swirlwave.android.settings.LocalSettings;
import com.swirlwave.android.tor.SwirlwaveOnionProxyManager;

import static android.nfc.NdefRecord.createMime;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, ActivityCompat.OnRequestPermissionsResultCallback, NfcAdapter.CreateNdefMessageCallback {
    private AppPermissions mAppPermissions = new AppPermissions(this);
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if all app permissions are granted.
        // Note: If the result isn't success, then the user will be asked and the callback method
        // onRequestPermissionsResult will be called async.
        AppPermissionsResult permissionsResult = mAppPermissions.requestAllPermissions();
        if (permissionsResult == AppPermissionsResult.Success) {
            init(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        AppPermissionsResult appPermissionsResult =
                mAppPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean wasSuccess = false;
        if (appPermissionsResult == AppPermissionsResult.Success) {
            wasSuccess = true;
        } else if(appPermissionsResult == AppPermissionsResult.Refused) {
            Log.e(getString(R.string.app_name), "The user has refused to grant needed permissions");
            Toast.makeText(this, R.string.required_permissions_refused, Toast.LENGTH_LONG).show();
        } else if (appPermissionsResult == AppPermissionsResult.Interrupted) {
            Log.e(getString(R.string.app_name), "Permission granting interaction was interrupted");
            Toast.makeText(this, R.string.permission_granting_interrupted, Toast.LENGTH_LONG).show();
        }

        init(wasSuccess);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Intent intent = new Intent(this, SwirlwaveService.class);
        String actionName = isChecked ? ActionNames.ACTION_INIT_SERVICE : ActionNames.ACTION_SHUT_DOWN_SERVICE;
        intent.setAction(actionName);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            // only one message sent during the beam
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            // record 0 contains the MIME type, record 1 is the AAR, if present
            String peerJson = new String(msg.getRecords()[0].getPayload());

            if (peerJson.equals("")) {
                Log.e(getString(R.string.app_name), "Transmitted peer info was empty");
                showToastOnUiThread(getString(R.string.nfc_empty_peer_info));
            } else {
                Peer peer = new Gson().fromJson(peerJson, Peer.class);

                Peer alreadyExistingPeer = PeersDb.selectByUuid(this, peer.getUuid());
                if (alreadyExistingPeer == null) {
                    PeersDb.insert(this, peer);
                } else {
                    peer.setId(alreadyExistingPeer.getId());
                    PeersDb.update(this, peer);
                }

                refreshPeersFragment();
            }
        }
    }

    private void refreshPeersFragment() {
        PeersFragment peersFragment = (PeersFragment) getFragmentManager().findFragmentById(R.id.peersFragment);
        peersFragment.refresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    private void init(boolean allPermissionsGranted) {
        Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
        serviceSwitch.setEnabled(allPermissionsGranted);
        serviceSwitch.setChecked(SwirlwaveService.isRunning());
        serviceSwitch.setOnCheckedChangeListener(this);

        // Due to a bug in Android, this doesn't always work, and the user has to whitelist manually
        mAppPermissions.requestIgnoreBatteryOptimization(this);

        LocalSettings.ensureInstallationNameAndPhoneNumber(this);

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Register NFC callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String peerJson;
        String address = SwirlwaveOnionProxyManager.getAddress();

        if (address.equals("")) {
            showToastOnUiThread(getString(R.string.must_be_conntected_to_transfer_contact_info));
            peerJson = null;
        } else {
            try {
                LocalSettings localSettings = new LocalSettings(this);
                Peer localPeer = new Peer(localSettings.getInstallationName(),
                        localSettings.getUuid(),
                        localSettings.getAsymmetricKeys().first,
                        localSettings.getPhoneNumber(),
                        address);
                peerJson = new Gson().toJson(localPeer);
            } catch (Exception e) {
                showToastOnUiThread(getString(R.string.something_went_wrong) + ": " + e.getLocalizedMessage());
                peerJson = null;
            }
        }

        NdefMessage msg = null;

        if (peerJson != null) {
            msg = new NdefMessage(new NdefRecord[] {
                            createMime("application/vnd.com.swirlwave.android.beam", peerJson.getBytes()),
                            NdefRecord.createApplicationRecord("com.swirlwave.android") });
        }

        return msg;
    }


    private void showToastOnUiThread(String text) {
        runOnUiThread(new ShowToastRunnable(text));
    }

    private class ShowToastRunnable implements Runnable {
        private String mText;

        public ShowToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_LONG).show();
        }
    }
}
