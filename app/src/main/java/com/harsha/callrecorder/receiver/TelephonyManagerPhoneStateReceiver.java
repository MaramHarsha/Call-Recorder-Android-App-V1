package com.harsha.callrecorder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import com.harsha.callrecorder.R;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.service.CallRecorderService;
import com.harsha.callrecorder.util.LogUtil;

public class TelephonyManagerPhoneStateReceiver extends BroadcastReceiver {
    private static final String TAG = TelephonyManagerPhoneStateReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.d(TAG, "Receiver receive");

        if (context == null || intent == null) {
            if (context == null) {
                LogUtil.w(TAG, "Receiver receive: Context lack");
            }

            if (intent == null) {
                LogUtil.w(TAG, "Receiver receive: Intent lack");
            }

            return;
        }

        String intentAction = null;
        try {
            intentAction = intent.getAction();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (intentAction == null) {
            LogUtil.w(TAG, "Receiver receive: Intent action lack");

            return;
        }

        // Relative
        if (!intentAction.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            LogUtil.w(TAG, "Receiver receive: Intent action mismatch");

            return;
        }
        // - Relative

        // ----

        LogUtil.d(TAG, "Receiver receive: OK");

        onReceiveOk(context, intent);
    }

    // Receiver relative
    private void onReceiveOk(@NonNull Context context, @NonNull Intent intent) {
        TelephonyManager telephonyManager = null;
        try {
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        String phoneStateExtraState = null;
        try {
            phoneStateExtraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (phoneStateExtraState != null) {
            if (phoneStateExtraState.equals(TelephonyManager.EXTRA_STATE_IDLE)) { // 0
                LogUtil.i(TAG, "Phone state: Idle");

                if (telephonyManager != null) {
                    if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                        onCallStateChange(context, intent, TelephonyManager.CALL_STATE_IDLE);
                    }
                } else {
                    onCallStateChange(context, intent, TelephonyManager.CALL_STATE_IDLE);
                }
            }

            if (phoneStateExtraState.equals(TelephonyManager.EXTRA_STATE_RINGING)) { // 1
                LogUtil.i(TAG, "Phone state: Ringing");

                if (telephonyManager != null) {
                    if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                        onCallStateChange(context, intent, TelephonyManager.CALL_STATE_RINGING);
                    }
                } else {
                    onCallStateChange(context, intent, TelephonyManager.CALL_STATE_RINGING);
                }
            }

            if (phoneStateExtraState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) { // 2
                LogUtil.i(TAG, "Phone state: Offhook");

                if (telephonyManager != null) {
                    if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
                        onCallStateChange(context, intent, TelephonyManager.CALL_STATE_OFFHOOK);
                    }
                } else {
                    onCallStateChange(context, intent, TelephonyManager.CALL_STATE_OFFHOOK);
                }
            }
        }
    }
    // - Receiver relative

    // ----

    private static boolean sIsIncoming = false;
    private static boolean sIsOutgoing = false;

    private void onCallStateChange(@NonNull Context context, @NonNull Intent intent, int callState) {
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (sharedPreferences != null) {
            switch (callState) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (CallRecorderService.sIsServiceRunning) {
                        stopRecorder(context, intent);
                    }

                    // ----

                    if (sIsIncoming) {
                        sIsIncoming = false;
                    }
                    if (sIsOutgoing) {
                        sIsOutgoing = false;
                    }

                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    if (!sIsOutgoing) {
                        sIsIncoming = true;
                    }

                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (!sIsIncoming) {
                        sIsOutgoing = true;
                    }

                    // ----

                    if (!CallRecorderService.sIsServiceRunning) {
                        if (sIsIncoming) {
                            LogUtil.i(TAG, "Call type: Incoming");

                            if (sharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true)) { // Incoming calls recording is always enabled by default
                                startRecorder(context, intent);
                            }

                            sIsIncoming = false;
                        }

                        if (sIsOutgoing) {
                            LogUtil.i(TAG, "Call type: Outgoing");

                            if (sharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true)) { // Outgoing calls recording is always enabled by default
                                startRecorder(context, intent);
                            }

                            sIsOutgoing = false;
                        }
                    }

                    break;
            }
        }
    }

    private void startRecorder(@NonNull Context context, @NonNull Intent intent) {
        if (CallRecorderService.sIsServiceRunning) {
            return;
        }

        intent.setClass(context, CallRecorderService.class);
        intent.putExtra(AppEnvr.INTENT_ACTION_INCOMING_CALL, sIsIncoming);
        intent.putExtra(AppEnvr.INTENT_ACTION_OUTGOING_CALL, sIsOutgoing);

        try {
            context.startService(intent);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
    } // Start media recording service

    private void stopRecorder(@NonNull Context context, @NonNull Intent intent) {
        if (!CallRecorderService.sIsServiceRunning) {
            return;
        }

        intent.setClass(context, CallRecorderService.class);

        try {
            context.stopService(intent);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
    } // Stop media recording service
}
