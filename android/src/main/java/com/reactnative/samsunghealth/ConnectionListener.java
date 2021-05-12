package com.reactnative.samsunghealth;

import android.util.Log;
import android.content.DialogInterface;

import com.facebook.react.bridge.Callback;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.facebook.react.bridge.Promise;

public class ConnectionListener implements HealthDataStore.ConnectionListener {
    private Callback mSuccessCallback;
    private Callback mErrorCallback;
    private Promise mPromise;
    private SamsungHealthModule mModule;
    private HealthConnectionErrorResult mConnError;
    private Boolean mRequestPermission;

    private static final String REACT_MODULE = "RNSamsungHealth";

    public Set<PermissionKey> mKeySet;
    public Set<PermissionKey> mStepsKeySet;

    public ConnectionListener(SamsungHealthModule module, Promise promise, Boolean requestPermissions) {
        mModule = module;
        mPromise = promise;
        mKeySet = new HashSet<PermissionKey>();
        mStepsKeySet = new HashSet<PermissionKey>();
        mRequestPermission = requestPermissions;
    }

    public void addReadPermission(String name) {
        mKeySet.add(new PermissionKey("com.samsung.health.weight", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.height", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.body_fat", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.user_profile", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.exercise", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.sleep", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.heart_rate", PermissionType.READ));
        mKeySet.add(new PermissionKey("com.samsung.health.step_count", PermissionType.READ));

        mStepsKeySet.add(new PermissionKey("com.samsung.health.step_count", PermissionType.READ));
    }

    @Override
    public void onConnected() {
        if (mKeySet.isEmpty()) {
            Log.e(REACT_MODULE, "Permission is empty");
            mPromise.reject("Permission is empty");
            return;
        }

        Log.d(REACT_MODULE, "Health data service is connected.");
        HealthPermissionManager pmsManager = new HealthPermissionManager(mModule.getStore());

        try {
            // Check whether the permissions that this application needs are acquired
            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);
            Map<PermissionKey, Boolean> stepsResultMap = pmsManager.isPermissionAcquired(mStepsKeySet);
            Log.d(REACT_MODULE, resultMap + "ResultMap");
            Log.d(REACT_MODULE, stepsResultMap + "StepsResultMap");
            if (stepsResultMap.containsValue(Boolean.FALSE) && mRequestPermission) {
                // Request the permission for reading step counts if it is not acquired
                pmsManager.requestPermissions(mKeySet, mModule.getContext().getCurrentActivity())
                        .setResultListener(new PermissionListener(mModule, mPromise));
            } else {
                // Get the current step count and display it
                Log.d(REACT_MODULE, "COUNT THE STEPS!");
                mPromise.resolve(true);
            }
        } catch (Exception e) {
            Log.e(REACT_MODULE, "CHECK");
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            mPromise.reject("Permission setting fails");
        }
    }

    @Override
    public void onConnectionFailed(HealthConnectionErrorResult error) {
        mConnError = error;
        String message = "Connection with Samsung Health is not available";

        if (error.hasResolution()) {
            switch (error.getErrorCode()) {
            case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                message = "Please install Samsung Health";
                break;
            case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                message = "Please upgrade Samsung Health";
                break;
            case HealthConnectionErrorResult.PLATFORM_DISABLED:
                message = "Please enable Samsung Health";
                break;
            case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                message = "Please agree with Samsung Health policy";
                break;
            default:
                message = "Please make Samsung Health available";
                break;
            }
        }
        mPromise.reject(message);
    }

    @Override
    public void onDisconnected() {
        Log.d(REACT_MODULE, "Health data service is disconnected.");
        // mErrorCallback.invoke("Health data service is disconnected.");
    }
};