package com.swirlwave.android.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for asking for permissions at runtime if needed
 */
public class AppPermissions {
    private Activity mActivity;
    private static final int REQUESTING_ALL_PERMISSIONS = 283;
    private static final String[] mRequiredPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    };

    public AppPermissions(Activity activity) {
        mActivity = activity;
    }

    /**
     * Checks all permissions needed for this app. Will ask user if necessary.
     * <p>
     * Permissions needed by the app must be defined both in AndroidManifest.xml and in this class.
     * <p>
     * The activity that calls this method, must implement
     * ActivityCompat.OnRequestPermissionsResultCallback and override onRequestPermissionsResult.
     * From that overriden method, it should call the onRequestPermissionResult method on this
     * class. The reason for this pattern is that only the activity will be called back by the Android API.
     * <p>
     * If AppPermissionsResult.Success is returned, then there is no need to ask the user, which
     * means that the callback will not be invoked.
     *
     * @return Success if all permissions are granted, or MustAskUser if some permissions has to be
     * approved by the user
     * @see <a href="https://developer.android.com/training/permissions/requesting.html">Requesting
     * Permissions at Run Time</a>
     */
    public AppPermissionsResult requestAllPermissions() {
        List<String> permissionsNeedingApproval = new ArrayList<>();

        for (String permission : mRequiredPermissions) {
            if (!alreadyGranted(permission)) {
                permissionsNeedingApproval.add(permission);
            }
        }

        if (permissionsNeedingApproval.size() > 0) {

            // TODO: Maybe use shouldShowRequestPermissionRationale here

            ActivityCompat.requestPermissions(
                    mActivity,
                    permissionsNeedingApproval.toArray(new String[permissionsNeedingApproval.size()]),
                    REQUESTING_ALL_PERMISSIONS);

            return AppPermissionsResult.WillAskUser;
        } else {
            return AppPermissionsResult.Success;
        }
    }

    public boolean alreadyGranted(String permission) {
        return ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * The activity that called requested permissions, has to call this method in its overridden
     * method onRequestPermissionsResult.
     * @param requestCode
     * @param permissions
     * @param grantResults
     * @return Success if all permissions were granted, Refused if permissions were
     * refused by the user, and Interrupted if the request interaction with the user was
     * interrupted/cancelled.
     */
    public AppPermissionsResult onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        AppPermissionsResult result = AppPermissionsResult.Undefined;

        if (permissions == null || permissions.length == 0 || grantResults == null || grantResults.length == 0) {
            result = AppPermissionsResult.Interrupted;
        } else if (requestCode ==REQUESTING_ALL_PERMISSIONS) {
            if (containsRefusedPermissions(grantResults)) {
                result = AppPermissionsResult.Refused;
            } else {
                result = AppPermissionsResult.Success;
            }
        }

        return result;
    }

    private boolean containsRefusedPermissions(int[] grantResults) {
        boolean containsRefusedPermissions = false;

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                containsRefusedPermissions = true;
                break;
            }
        }

        return containsRefusedPermissions;
    }
}
