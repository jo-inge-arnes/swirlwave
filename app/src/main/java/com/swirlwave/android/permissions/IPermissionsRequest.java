package com.swirlwave.android.permissions;

/**
 * This interface is defines a callback method invoked by the {@link AppPermissions} method
 * requestPermissions.
 */
public interface IPermissionsRequest {
    /**
     * Gets the permissions that needs to be granted for this request
     * @return The permission names
     * @see android.Manifest.permission
     */
    String[] getRequestedPermissions();

    /**
     * Callback method invoked after permissions have been granted or denied
     * @param success True if all permissions were granted
     */
    void onRequestPermissionsResult(boolean success);
}
