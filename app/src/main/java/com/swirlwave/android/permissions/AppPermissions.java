package com.swirlwave.android.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AppPermissions implements ActivityCompat.OnRequestPermissionsResultCallback {
    private Activity mActivity;
    private static int mRequestCounter = Integer.MIN_VALUE;
    private Map<Integer, IPermissionsRequest> mRequests = new HashMap<>();

    public AppPermissions(Activity activity) {
        mActivity = activity;
    }

    public boolean readPhoneStateAlreadyGranted() {
        return alreadyGranted(Manifest.permission.READ_PHONE_STATE);
    }

    public boolean readSmsAlreadyGranted() {
        return alreadyGranted(Manifest.permission.READ_SMS);
    }

    public boolean internetAlreadyGranted() {
        return alreadyGranted(Manifest.permission.INTERNET);
    }

    public boolean alreadyGranted(String permission) {
        return ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissions(IPermissionsRequest request) {
        int requestId = rememberRequest(request);

        List<String> needsUserApproval = new ArrayList<>();

        // Check each permission to see if it already has been granted
        for (String permission : request.getRequestedPermissions()) {
            if (!alreadyGranted(permission)) {
                // TODO: Could add check and use of shouldShowRequestPermissionRationale here

                needsUserApproval.add(permission);
            }
        }

        if (needsUserApproval.size() == 0) {
            request = getBackRequest(requestId);
            request.onRequestPermissionsResult(true);
        } else {
            ActivityCompat.requestPermissions(
                    mActivity,
                    needsUserApproval.toArray(new String[needsUserApproval.size()]),
                    requestId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        IPermissionsRequest request = getBackRequest(requestCode);
        boolean wasSuccess = true;

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                wasSuccess = false;
                break;
            }
        }

        request.onRequestPermissionsResult(wasSuccess);
    }


    private int rememberRequest(IPermissionsRequest request) {
        int requestId = nextRequestId();
        mRequests.put(requestId, request);
        return requestId;
    }

    private IPermissionsRequest getBackRequest(int requestId) {
        return mRequests.remove(requestId);
    }

    private int nextRequestId(){
        return mRequestCounter++;
    }
}
