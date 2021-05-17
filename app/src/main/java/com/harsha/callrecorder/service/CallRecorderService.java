package com.harsha.callrecorder.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.harsha.callrecorder.R;
import com.harsha.callrecorder.config.AppConfig;
import com.harsha.callrecorder.config.CallRecorderConfig;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.object.IncomingCallObject;
import com.harsha.callrecorder.object.OutgoingCallObject;
import com.harsha.callrecorder.util.LogUtil;

import java.io.File;
import java.util.Date;

import io.realm.Realm;

public class CallRecorderService extends Service {
    private static final String TAG = CallRecorderService.class.getSimpleName();

    public static boolean sIsServiceRunning = false;

    private Realm mRealm = null;

    private NotificationManager mNotificationManager = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel mNotificationChannel = null;

    private static final int FOREGROUND_NOTIFICATION_ID = 2;

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private TelephonyManager mTelephonyManager = null;

    @RequiresPermission(Manifest.permission.VIBRATE)
    private Vibrator mVibrator = null;

    @RequiresPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)
    private AudioManager mAudioManager = null;

    // ----

    private SharedPreferences mPreferenceManagerSharedPreferences = null;

    private boolean mIsIncoming = false;
    private boolean mIsOutgoing = false;

    private String mPhoneStateIncomingNumber = null;

    // ----

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private MediaRecorder mMediaRecorder = null;

    private boolean mVibrate = true, mTurnOnSpeaker = false, mMaxUpVolume = true;

    private int mVoiceCallStreamVolume = -1;

    private IncomingCallObject mIncomingCallObject = null;
    private OutgoingCallObject mOutgoingCallObject = null;

    /*private final IBinder mBinder = new CallRecorderService.LocalBinder();

    public class LocalBinder extends Binder {
        public CallRecorderService getService() {
            return CallRecorderService.this;
        }
    }*/

    @Override
    public IBinder onBind(Intent intent) {
        /*return mBinder;*/

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        LogUtil.d(TAG, "Service start command");

        if (intent != null) {
            try {
                mPreferenceManagerSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            if (intent.hasExtra(AppEnvr.INTENT_ACTION_INCOMING_CALL)) {
                mIsIncoming = intent.getBooleanExtra(AppEnvr.INTENT_ACTION_INCOMING_CALL, false);
            }
            if (intent.hasExtra(AppEnvr.INTENT_ACTION_OUTGOING_CALL)) {
                mIsOutgoing = intent.getBooleanExtra(AppEnvr.INTENT_ACTION_OUTGOING_CALL, false);
            }

            if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                mPhoneStateIncomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                if (mPhoneStateIncomingNumber != null) {
                    if (!mPhoneStateIncomingNumber.trim().isEmpty()) {
                        LogUtil.i(TAG, "Phone state incoming number: " + mPhoneStateIncomingNumber);
                    }
                }
            }

            if (mIsIncoming || mIsOutgoing) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    /*int audioSource = CallRecorderConfig.CALL_RECORDER_DEFAULT_AUDIO_SOURCE;
                    int outputFormat = CallRecorderConfig.CALL_RECORDER_DEFAULT_OUTPUT_FORMAT;
                    int audioEncoder = CallRecorderConfig.CALL_RECORDER_DEFAULT_AUDIO_ENCODER;

                    beginRecorder(audioSource, outputFormat, audioEncoder);*/

                    beginRecorder(null, null, null);
                } else {
                    try {
                        stopSelf();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    stopSelf();
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }
        }

        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "Service create");

        sIsServiceRunning = true;

        // ----

        // Realm
        try {
            mRealm = Realm.getDefaultInstance();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
        // - Realm

        // ----

        try {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mNotificationManager != null) {
            CharSequence contentTitle = "Recording...", contentText = "Call recording is currently in progress.";

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            try {
                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            endRecorder();
        }

        if (mPhoneStateIncomingNumber != null) {
            mPhoneStateIncomingNumber = null;
        }

        if (mIsOutgoing) {
            mIsOutgoing = false;
        }
        if (mIsIncoming) {
            mIsIncoming = false;
        }

        if (mPreferenceManagerSharedPreferences != null) {
            mPreferenceManagerSharedPreferences = null;
        }

        // ----

        super.onDestroy();
        LogUtil.d(TAG, "Service destroy");

        // ----

        if (mAudioManager != null) {
            mAudioManager = null;
        }

        if (mVibrator != null) {
            mVibrator = null;
        }

        if (mTelephonyManager != null) {
            mTelephonyManager = null;
        }

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

        // ----

        // Realm
        if (mRealm != null) {
            if (!mRealm.isClosed()) {
                try {
                    mRealm.close();
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            mRealm = null;
        }
        // - Realm

        // ----

        sIsServiceRunning = false;
    }

    // ----

    private final MediaRecorder.OnInfoListener mMediaRecorderOnInfoListener = (mr, what, extra) -> {
        LogUtil.d(TAG, "Media recorder info");

        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                LogUtil.i(TAG, "Media recorder info: Unknown");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                LogUtil.i(TAG, "Media recorder info: Max duration reached");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                LogUtil.i(TAG, "Media recorder info: Max filesize reached");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING:
                LogUtil.i(TAG, "Media recorder info: Max filesize approaching");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED:
                LogUtil.i(TAG, "Media recorder info: Next output file started");
                break;
        }

        LogUtil.d(TAG, "Media recorder info extra: " + extra);
    };

    private final MediaRecorder.OnErrorListener mMediaRecorderOnErrorListener = (mr, what, extra) -> {
        LogUtil.d(TAG, "Media recorder error");

        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN:
                LogUtil.w(TAG, "Media recorder error: Unknown");
                break;
            case MediaRecorder.MEDIA_ERROR_SERVER_DIED:
                LogUtil.w(TAG, "Media error: Server died");
                break;
        }

        LogUtil.d(TAG, "Media recorder error extra: " + extra);
    };

    private boolean prepare() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to prepare media recorder...");
            try {
                mMediaRecorder.prepare();

                LogUtil.i(TAG, "Prepare OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to prepare media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            LogUtil.w(TAG, "Cannot prepare media recorder when it is null");
        }

        return false;
    }

    private boolean start() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to start media recorder...");
            try {
                mMediaRecorder.start();

                LogUtil.i(TAG, "Start OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to prepare media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            LogUtil.w(TAG, "Cannot start media recorder when it is null");
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean resume() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to resume media recorder...");
            try {
                mMediaRecorder.resume();

                LogUtil.i(TAG, "Resume OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to resume media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            LogUtil.w(TAG, "Cannot resume media recorder when it is null");
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean pause() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to pause media recorder...");
            try {
                mMediaRecorder.pause();

                LogUtil.i(TAG, "Pause OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to pause media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }  else {
            LogUtil.w(TAG, "Cannot pause media recorder when it is null");
        }

        return false;
    }

    private boolean stop() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to stop media recorder...");
            try {
                mMediaRecorder.stop();

                LogUtil.i(TAG, "Stop OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to stop media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            LogUtil.w(TAG, "Cannot stop media recorder when it is null");
        }

        return false;
    }

    private boolean reset() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to reset media recorder...");
            try {
                mMediaRecorder.reset();

                LogUtil.i(TAG, "Reset OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to reset media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            LogUtil.w(TAG, "Cannot reset media recorder when it is null");
        }

        return false;
    }

    private boolean release() {
        if (mMediaRecorder != null) {
            LogUtil.d(TAG, "Trying to release media recorder...");
            try {
                mMediaRecorder.release();

                LogUtil.i(TAG, "Release OK");

                return true;
            } catch (Exception e) {
                LogUtil.e(TAG, "Exception while trying to release media recorder");

                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            LogUtil.w(TAG, "Cannot release media recorder when it is null");
        }

        return false;
    }

    // ----

    @SuppressLint("HardwareIds")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void beginRecorder(@Nullable Integer audioSource, @Nullable Integer outputFormat, @Nullable Integer audioEncoder) {
        if (mMediaRecorder != null) {
            return;
        }

        // ----

        long beginTimestamp = new Date().getTime(); // or "System.currentTimeMillis()"

        // ----

        if (mPreferenceManagerSharedPreferences != null) {
            if (audioSource == null) {
                audioSource = Integer.valueOf(mPreferenceManagerSharedPreferences.getString(AppEnvr.FM_SP_AUDIO_SOURCE,
                        String.valueOf(CallRecorderConfig.CALL_RECORDER_DEFAULT_AUDIO_SOURCE)));
            }
            if (outputFormat == null) {
                outputFormat = Integer.valueOf(mPreferenceManagerSharedPreferences.getString(AppEnvr.FM_SP_OUTPUT_FORMAT,
                        String.valueOf(CallRecorderConfig.CALL_RECORDER_DEFAULT_OUTPUT_FORMAT)));
            }
            if (audioEncoder == null) {
                audioEncoder = Integer.valueOf(mPreferenceManagerSharedPreferences.getString(AppEnvr.FM_SP_AUDIO_ENCODER,
                        String.valueOf(CallRecorderConfig.CALL_RECORDER_DEFAULT_AUDIO_ENCODER)));
            }

            // ----

            mVibrate = mPreferenceManagerSharedPreferences.getBoolean(AppEnvr.FM_SP_VIBRATE, true); // Enabled by default
            mTurnOnSpeaker = mPreferenceManagerSharedPreferences.getBoolean(AppEnvr.FM_SP_TURN_ON_SPEAKER, false); // Not enabled by default
            mMaxUpVolume = mPreferenceManagerSharedPreferences.getBoolean(AppEnvr.FM_SP_MAX_UP_VOLUME, true); // Enabled by default
        }

        if (mMaxUpVolume) {
            if (mAudioManager != null) {
                try {
                    mVoiceCallStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }
        }

        // ----

        if (audioSource == null) {
            audioSource = CallRecorderConfig.CALL_RECORDER_DEFAULT_AUDIO_SOURCE;
        }
        if (outputFormat == null) {
            outputFormat = CallRecorderConfig.CALL_RECORDER_DEFAULT_OUTPUT_FORMAT;
        }
        if (audioEncoder == null) {
            audioEncoder = CallRecorderConfig.CALL_RECORDER_DEFAULT_AUDIO_ENCODER;
        }

        // ----

        String type = "-";
        if (mIsIncoming) {
            type = "-I--";
        }
        if (mIsOutgoing) {
            type = "--0-";
        }

        // ----

        String valueExternal = getResources().getStringArray(R.array.records_output_location_entry_values)[0];
        String valueInternal = getResources().getStringArray(R.array.records_output_location_entry_values)[1];

        String recordsOutputDirectoryPath = null;

        if (mPreferenceManagerSharedPreferences != null) {
            if (mPreferenceManagerSharedPreferences.contains(AppEnvr.FM_SP_KEY_RECORDS_OUTPUT_LOCATION)) {
                String recordsOutputLocation = mPreferenceManagerSharedPreferences.getString(AppEnvr.FM_SP_KEY_RECORDS_OUTPUT_LOCATION, valueExternal);

                if (recordsOutputLocation.equals(valueExternal)) {
                    recordsOutputDirectoryPath = AppEnvr.sExternalFilesDirPathMemory;
                }

                if (recordsOutputLocation.equals(valueInternal)) {
                    recordsOutputDirectoryPath = AppEnvr.sFilesDirPathMemory;
                }
            }
        }

        if (recordsOutputDirectoryPath == null) {
            recordsOutputDirectoryPath = AppEnvr.sExternalFilesDirPathMemory;
        }

        String outputFilePath = recordsOutputDirectoryPath + File.separator + mPhoneStateIncomingNumber + type + beginTimestamp;

        // ----

        try {
            mMediaRecorder = new MediaRecorder();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mMediaRecorder == null) {
            try {
                stopSelf();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            return;
        }

        mMediaRecorder.setOnInfoListener(mMediaRecorderOnInfoListener);
        mMediaRecorder.setOnErrorListener(mMediaRecorderOnErrorListener);
        mMediaRecorder.setAudioSource(audioSource);
        mMediaRecorder.setOutputFormat(outputFormat);
        mMediaRecorder.setAudioEncoder(audioEncoder);
        mMediaRecorder.setOutputFile(outputFilePath);

        boolean prepare = prepare(), start = start();

        boolean succeed = prepare && start;

        if (!succeed) {
            if (!prepare) {
                LogUtil.w(TAG, "Media recorder (telephony) has prepare exception");
            }

            if (!start) {
                LogUtil.w(TAG, "Media recorder (telephony) has start exception");
            }

            // ----

            for (int otherAudioSource : CallRecorderConfig.getAudioSources()) {
                if (otherAudioSource == audioSource) {
                    continue;
                }

                audioSource = otherAudioSource;

                reset();

                mMediaRecorder.setOnInfoListener(mMediaRecorderOnInfoListener);
                mMediaRecorder.setOnErrorListener(mMediaRecorderOnErrorListener);
                mMediaRecorder.setAudioSource(audioSource);
                mMediaRecorder.setOutputFormat(outputFormat);
                mMediaRecorder.setAudioEncoder(audioEncoder);
                mMediaRecorder.setOutputFile(outputFilePath);

                boolean otherPrepare = prepare();

                boolean otherStart = start();

                if (otherPrepare && otherStart) {
                    succeed = true;

                    break;
                } else {
                    if (!otherPrepare) {
                        LogUtil.w(TAG, "Media recorder (telephony) has other prepare exception");
                    }

                    if (!otherStart) {
                        LogUtil.w(TAG, "Media recorder (telephony) has other start exception");
                    }
                }
            }
        }

        // ----

        if (!succeed) {
            try {
                stopSelf();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }


            return;
        }

        // ----

        // Begin recorder other actions
        if (mVibrate) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                if (mVibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            mVibrator.vibrate(VibrationEffect.createOneShot(AppConfig.BEGIN_RECORDER_VIBE_TIME, VibrationEffect.DEFAULT_AMPLITUDE)); // Vibrate
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    } else {
                        try {
                            mVibrator.vibrate(AppConfig.BEGIN_RECORDER_VIBE_TIME); // Vibrate
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            if (mAudioManager != null) {
                if (mTurnOnSpeaker) {
                    try {
                        if (!mAudioManager.isSpeakerphoneOn()) {
                            mAudioManager.setSpeakerphoneOn(true); // Turn on speaker (on state)
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }

                if (mMaxUpVolume) {
                    if (mVoiceCallStreamVolume != -1) {
                        try {
                            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0); // Max up volume
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        // - Begin recorder other actions

        // ----

        String simSerialNumber = null;

        String simOperator = null;
        String simOperatorName = null;
        String simCountryIso = null;

        String networkOperator = null;
        String networkOperatorName = null;
        String networkCountryIso = null;

        TelephonyManager telephonyManager = null;
        try {
            telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (telephonyManager != null) {
            try {
                simSerialNumber = telephonyManager.getSimSerialNumber();

                LogUtil.i(TAG, "SIM Serial Number: " + simSerialNumber);

                simOperator = telephonyManager.getSimOperator();
                simOperatorName = telephonyManager.getSimOperatorName();
                simCountryIso = telephonyManager.getSimCountryIso();

                LogUtil.i(TAG, "SIM Operator: " + simOperator);
                LogUtil.i(TAG, "SIM Operator Name: " + simOperatorName);
                LogUtil.i(TAG, "SIM Country ISO: " + simCountryIso);

                networkOperator = telephonyManager.getNetworkOperator();
                networkOperatorName = telephonyManager.getNetworkOperatorName();
                networkCountryIso = telephonyManager.getNetworkCountryIso();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }

        // ----

        // Realm
        if (mRealm != null && !mRealm.isClosed()) {
            final String finalSimSerialNumber = simSerialNumber;

            final String finalSimOperator = simOperator;
            final String finalSimOperatorName = simOperatorName;
            final String finalSimCountryIso = simCountryIso;

            final String finalNetworkOperator = networkOperator;
            final String finalNetworkOperatorName = networkOperatorName;
            final String finalNetworkCountryIso = networkCountryIso;

            final int finalAudioSource = audioSource;
            final int finalOutputFormat = outputFormat;
            final int finalAudioEncoder = audioEncoder;

            final String finalOutputFilePath = outputFilePath;

            try {
                if (mIsIncoming) {
                    mRealm.executeTransaction(realm -> {
                        mIncomingCallObject = realm.createObject(IncomingCallObject.class);

                        if (mIncomingCallObject != null) {
                            mIncomingCallObject.setPhoneNumber(mPhoneStateIncomingNumber);
                            mIncomingCallObject.setBeginTimestamp(beginTimestamp);
                            mIncomingCallObject.setSimOperator(finalSimOperator);
                            mIncomingCallObject.setSimSerialNumber(finalSimSerialNumber);
                            mIncomingCallObject.setSimOperatorName(finalSimOperatorName);
                            mIncomingCallObject.setSimCountryIso(finalSimCountryIso);
                            mIncomingCallObject.setNetworkOperator(finalNetworkOperator);
                            mIncomingCallObject.setNetworkOperatorName(finalNetworkOperatorName);
                            mIncomingCallObject.setNetworkCountryIso(finalNetworkCountryIso);
                            mIncomingCallObject.setAudioSource(finalAudioSource);
                            mIncomingCallObject.setOutputFormat(finalOutputFormat);
                            mIncomingCallObject.setAudioEncoder(finalAudioEncoder);
                            mIncomingCallObject.setOutputFile(finalOutputFilePath);
                        }
                    });
                }
                if (mIsOutgoing) {
                    mRealm.executeTransaction(realm -> {
                        mOutgoingCallObject = realm.createObject(OutgoingCallObject.class);

                        if (mOutgoingCallObject != null) {
                            mOutgoingCallObject.setPhoneNumber(mPhoneStateIncomingNumber);
                            mOutgoingCallObject.setBeginTimestamp(beginTimestamp);
                            mOutgoingCallObject.setAudioSource(finalAudioSource);
                            mOutgoingCallObject.setSimSerialNumber(finalSimSerialNumber);
                            mOutgoingCallObject.setSimOperator(finalSimOperator);
                            mOutgoingCallObject.setSimOperatorName(finalSimOperatorName);
                            mOutgoingCallObject.setSimCountryIso(finalSimCountryIso);
                            mOutgoingCallObject.setNetworkOperator(finalNetworkOperator);
                            mOutgoingCallObject.setNetworkOperatorName(finalNetworkOperatorName);
                            mOutgoingCallObject.setNetworkCountryIso(finalNetworkCountryIso);
                            mOutgoingCallObject.setOutputFormat(finalOutputFormat);
                            mOutgoingCallObject.setAudioEncoder(finalAudioEncoder);
                            mOutgoingCallObject.setOutputFile(finalOutputFilePath);
                        }
                    });
                }
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }
        // - Realm
    }

    @SuppressLint("HardwareIds")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void endRecorder() {
        if (mMediaRecorder == null) {
            return;
        }

        // ----

        long endTimestamp = new Date().getTime(); // or "System.currentTimeMillis()"

        // ----

        // End recorder other actions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            if (mAudioManager != null) {
                if (mTurnOnSpeaker) {
                    try {
                        if (mAudioManager.isSpeakerphoneOn()) {
                            mAudioManager.setSpeakerphoneOn(false); // Turn on speaker (off state)
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }

                if (mMaxUpVolume) {
                    if (mVoiceCallStreamVolume != -1) {
                        try {
                            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mVoiceCallStreamVolume, 0); // Max up volume
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (mVibrate) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                if (mVibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            mVibrator.vibrate(VibrationEffect.createOneShot(AppConfig.END_RECORDER_VIBE_TIME, VibrationEffect.DEFAULT_AMPLITUDE)); // Vibrate
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    } else {
                        try {
                            mVibrator.vibrate(AppConfig.END_RECORDER_VIBE_TIME); // Vibrate
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        // - End recorder other actions

        // ----

        boolean stop = stop(), reset = reset(), release = release();

        if (!stop || !reset || !release) {
            if (!stop) {
                LogUtil.w(TAG, "Media recorder (telephony) has stop exception");
            }

            if (!reset) {
                LogUtil.w(TAG, "Media recorder (telephony) has reset exception");
            }

            if (!release) {
                LogUtil.w(TAG, "Media recorder (telephony) has release exception");
            }
        }

        mMediaRecorder = null;

        // ----

        if (mVoiceCallStreamVolume != -1) {
            mVoiceCallStreamVolume = -1;
        }

        if (!mMaxUpVolume) {
            mMaxUpVolume = true;
        }
        if (mTurnOnSpeaker) {
            mTurnOnSpeaker = false;
        }
        if (!mVibrate) {
            mVibrate = true;
        }

        // ----

        // Realm
        if (mRealm != null && !mRealm.isClosed()) {
            try {
                if (mIncomingCallObject != null) {
                    mRealm.executeTransaction(realm -> mIncomingCallObject.setEndTimestamp(endTimestamp));

                    mIncomingCallObject = null;
                }
                if (mOutgoingCallObject != null) {
                    mRealm.executeTransaction(realm -> mOutgoingCallObject.setEndTimestamp(endTimestamp));

                    mOutgoingCallObject = null;
                }
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }
        // - Realm
    }
}
