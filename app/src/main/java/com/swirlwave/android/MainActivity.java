package com.swirlwave.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.swirlwave.android.permissions.AppPermissions;
import com.swirlwave.android.permissions.AppPermissionsResult;
import com.swirlwave.android.service.ActionNames;
import com.swirlwave.android.service.SwirlwaveService;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private AppPermissions mAppPermissions = new AppPermissions(this);

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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Intent intent = new Intent(this, SwirlwaveService.class);
        String actionName = isChecked ? ActionNames.ACTION_INIT_SERVICE : ActionNames.ACTION_SHUT_DOWN_SERVICE;
        intent.setAction(actionName);
        startService(intent);
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

    private void init(boolean allPermissionsGranted) {
        Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
        serviceSwitch.setEnabled(allPermissionsGranted);
        serviceSwitch.setChecked(SwirlwaveService.isRunning());
        serviceSwitch.setOnCheckedChangeListener(this);

        // TODO: Move this code and the method requestIgnoreBatteryOptimization to AppPermissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestIgnoreBatteryOptimization();
        }
    }

    /**
     * Because of Android 6 (API 23) has a new doze and standby feature, ask for service
     * to keep running even if the app's activity is shut down, and also to keep running even
     * in doze or standby...
     * <p>
     * Note: There's a bug report saying this doesn't work. <a href="https://code.google.com/p/android/issues/detail?id=191195">Issue 191195</a>
     * You have to whitelist the app manually from Settings->Apps->Swirlwave->Battery and allow app to run when screen is off.
     *
     * @see <a href="https://developer.android.com/training/monitoring-device-state/doze-standby.html">Optimize for Doze and Standby</a>
     */
    @TargetApi(23)
    private void requestIgnoreBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }
}
