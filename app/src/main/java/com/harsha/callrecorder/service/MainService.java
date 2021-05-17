package com.harsha.callrecorder.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.harsha.callrecorder.R;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.receiver.TelephonyManagerPhoneStateReceiver;
import com.harsha.callrecorder.util.AppUtil;
import com.harsha.callrecorder.util.LogUtil;

public class MainService extends Service {
    private static final String TAG = MainService.class.getSimpleName();

    public static boolean sIsServiceRunning = false;

    private NotificationManager mNotificationManager = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel mNotificationChannel = null;

    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private PowerManager mPowerManager = null;
    private PowerManager.WakeLock mWakeLock = null;

    private SharedPreferences mSharedPreferences = null;
    private final SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = (sharedPreferences, s) -> {
        if (sharedPreferences == null || s == null) {
            return;
        }

        // ----

        LogUtil.d(TAG, "Shared preference change listener - Shared preference changed");

        if (s.equals(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS)) {
            if (!sharedPreferences.contains(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true); // Incoming calls recording is always enabled by default
                editor.apply();
            }
        }

        if (s.equals(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS)) {
            if (!sharedPreferences.contains(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true); // Outgoing calls recording is always enabled by default
                editor.apply();
            }
        }

        boolean recordIncomingCalls = sharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true);
        boolean recordOutgoingCalls = sharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true);

        if (!recordIncomingCalls && !recordOutgoingCalls) {
            try {
                stopSelf();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }
    };

    private final TelephonyManagerPhoneStateReceiver mTelephonyManagerPhoneStateReceiver = new TelephonyManagerPhoneStateReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        LogUtil.d(TAG, "Service start command");

        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "Service create");

        sIsServiceRunning = true;

        // ----

        // Foreground notification (start)

        try {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mNotificationManager != null) {
            CharSequence contentTitle = "Running...", contentText = getString(R.string.app_name) + " is active.";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    mNotificationChannel = new NotificationChannel(getString(R.string.service) + "-" + FOREGROUND_NOTIFICATION_ID, getString(R.string.service), NotificationManager.IMPORTANCE_NONE);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (mNotificationChannel != null) {
                    try {
                        mNotificationManager.createNotificationChannel(mNotificationChannel);
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }

                    Icon logoIcon = Icon.createWithResource(this, R.drawable.ic_logo);

                    try {
                        startForeground(FOREGROUND_NOTIFICATION_ID, new Notification.Builder(this, getString(R.string.service) + "-" + FOREGROUND_NOTIFICATION_ID)
                                .setSmallIcon(logoIcon)
                                .setLargeIcon(logoIcon)
                                .setContentTitle(contentTitle)
                                .setContentText(contentText)
                                .build());
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }
            } else {
                Notification.Builder builder = new Notification.Builder(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Icon logoIcon = Icon.createWithResource(this, R.drawable.ic_logo);

                    builder.setSmallIcon(logoIcon);
                    builder.setLargeIcon(logoIcon);
                } else {
                    builder.setSmallIcon(R.drawable.ic_logo);
                }
                builder.setContentTitle(contentTitle);
                builder.setContentText(contentText);
                builder.setOngoing(true);

                try {
                    mNotificationManager.notify(FOREGROUND_NOTIFICATION_ID, builder.build());
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }
        }
        // - Foreground notification (start)

        // ----

        try {
            mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mPowerManager != null) {
            try {
                mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name));
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            if (mWakeLock != null) {
                AppUtil.acquireWakeLock(mWakeLock); // Acquire wake lock
            }
        }

        // ----

        try {
            mSharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mSharedPreferences != null) {
            try {
                mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }

        // ----

        // Receiver(s) - Enable, register
        /*if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {*/
            /*try {
                getPackageManager().setComponentEnabledSetting(new ComponentName(this, TelephonyManagerPhoneStateReceiver.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }*/

            try {
                registerReceiver(mTelephonyManagerPhoneStateReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        /*}*/
        // - Receiver(s) - Enable, register
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "Service destroy");

        // Receiver(s) - Unregister, disable
        /*if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {*/
            try {
                unregisterReceiver(mTelephonyManagerPhoneStateReceiver);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            /*try {
                getPackageManager().setComponentEnabledSetting(new ComponentName(this, TelephonyManagerPhoneStateReceiver.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }*/
        /*}*/
        // - Receiver(s) - Unregister, disable

        // ----

        if (mSharedPreferences != null) {
            try {
                mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            mSharedPreferences = null;
        }

        // ----

        if (mPowerManager != null) {
            if (mWakeLock != null) {
                AppUtil.releaseWakeLock(mWakeLock); // Release wake lock

                mWakeLock = null;
            }

            mPowerManager = null;
        }

        // ----

        // Foreground notification (stop)
        if (mNotificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mNotificationChannel != null) {
                    try {
                        stopForeground(true);
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }

                    try {
                        mNotificationManager.deleteNotificationChannel(mNotificationChannel.getId());
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }

                    mNotificationChannel = null;
                }
            } else {
                try {
                    mNotificationManager.cancel(FOREGROUND_NOTIFICATION_ID);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            mNotificationManager = null;
        }
        // - Foreground notification (stop)

        // ----

        sIsServiceRunning = false;
    }
}
