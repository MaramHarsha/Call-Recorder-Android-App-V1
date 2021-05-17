package com.harsha.callrecorder.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

public class RequestIgnoreBatteryOptimizationsUtil {
    private static final String TAG = RequestIgnoreBatteryOptimizationsUtil.class.getSimpleName();

    @Nullable
    @RequiresApi(api = Build.VERSION_CODES.M)
    @RequiresPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    @SuppressLint("BatteryLife")
    public static Intent getRequestIgnoreBatteryOptimizationsIntent(@NonNull final Context context) {
        try {
            final String scheme = "package", schemeSpecificPart = context.getPackageName();

            if (schemeSpecificPart != null) {
                final Uri uri = Uri.fromParts(scheme, schemeSpecificPart, null);

                if (uri != null) {
                    if (uri.getScheme() != null && uri.getScheme().equals(scheme)
                            && uri.getSchemeSpecificPart() != null && uri.getSchemeSpecificPart().equals(schemeSpecificPart)) {
                        final Intent requestIgnoreBatteryOptimizationsIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        requestIgnoreBatteryOptimizationsIntent.setData(uri);

                        return requestIgnoreBatteryOptimizationsIntent;
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        return null;
    }
}
