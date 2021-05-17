package com.harsha.callrecorder.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.harsha.callrecorder.service.MainService;

public class AppUtil {
    private static final String TAG = AppUtil.class.getSimpleName();

    // "MainService"
    public static void startMainService(@NonNull final Context context) {
        if (MainService.sIsServiceRunning) {
            LogUtil.w(TAG, "Will not start \"MainService\", it is running");

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Start foreground
            try {
                context.startForegroundService(new Intent(context, MainService.class));
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
            // - Start foreground
        } else {
            // Start
            try {
                context.startService(new Intent(context, MainService.class));
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
            // - Start
        }
    } // Start main service ("MainService")

    public static void stopMainService(@NonNull final Context context) {
        if (!MainService.sIsServiceRunning) {
            LogUtil.w(TAG, "Will not stop \"MainService\", it is not running");

            return;
        }

        // Stop
        try {
            context.stopService(new Intent(context, MainService.class));
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
        // - Stop
    } // Stop main service ("MainService")
    // - "MainService"

    // Wake lock
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public static void acquireWakeLock(@NonNull final PowerManager.WakeLock wakeLock, final long timeout) {
        LogUtil.d(TAG, "Trying to acquire wake lock with timeout...");
        try {
            wakeLock.acquire(timeout);
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while trying to acquire wake lock with timeout");

            // ----

            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        // ----

        try {
            if (wakeLock.isHeld()) {
                // OK
                LogUtil.d(TAG, "Wake lock acquired");
                return;
                // - OK
            } else {
                // Not OK
                LogUtil.w(TAG, "Wake lock not acquired");
                return;
                // - Not OK
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        LogUtil.w(TAG, "Wake lock not acquired");
    } // Acquire wake lock (with timeout)

    @SuppressLint("WakelockTimeout")
    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public static void acquireWakeLock(@NonNull final PowerManager.WakeLock wakeLock) {
        LogUtil.d(TAG, "Trying to acquire wake lock without timeout...");
        try {
            wakeLock.acquire();
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while trying to acquire wake lock without timeout");

            // ----

            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        // ----

        try {
            if (wakeLock.isHeld()) {
                // OK
                LogUtil.d(TAG, "Wake lock acquired");
                return;
                // OK
            } else {
                // Not OK
                LogUtil.w(TAG, "Wake lock not acquired");
                return;
                // - Not OK
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        LogUtil.d(TAG, "Wake lock not acquired");
    } // Acquire wake lock

    @RequiresPermission(Manifest.permission.WAKE_LOCK)
    public static void releaseWakeLock(@NonNull final PowerManager.WakeLock wakeLock) {
        LogUtil.d(TAG, "Trying to release wake lock...");
        try {
            wakeLock.release();
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while trying to release wake lock");

            // ----

            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        // ----

        try {
            if (wakeLock.isHeld()) {
                // Not OK
                LogUtil.w(TAG, "Wake lock not released");
                return;
                // - Not OK
            } else {
                // OK
                LogUtil.d(TAG, "Wake lock released");
                return;
                // - OK
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        LogUtil.d(TAG, "Wake lock not released");
    } // Release wake lock
    // - Wake lock

    // ----

    public static void openPackageInMarket(@NonNull final Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()))); // Open in market
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();

            // ----

            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName()))); // Open in WebView
            } catch (Exception ex) {
                LogUtil.e(TAG, ex.getMessage());
                LogUtil.e(TAG, ex.toString());

                ex.printStackTrace();
            }
        }
    }
}
