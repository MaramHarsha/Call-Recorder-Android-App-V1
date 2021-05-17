package com.harsha.callrecorder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.harsha.callrecorder.R;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.service.MainService;
import com.harsha.callrecorder.util.AppUtil;
import com.harsha.callrecorder.util.LogUtil;

public class ActionShutdownReceiver extends BroadcastReceiver {
    private static final String TAG = ActionShutdownReceiver.class.getSimpleName();

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
        if (!intentAction.equals(Intent.ACTION_SHUTDOWN)) {
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
        SharedPreferences sharedPreferences = null;
        try {
            sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (sharedPreferences != null) {
            if (!sharedPreferences.contains(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true); // Incoming calls recording is always enabled by default
                editor.apply();
            }
            if (!sharedPreferences.contains(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS)) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true); // Outgoing calls recording is always enabled by default
                editor.apply();
            }

            boolean recordIncomingCalls = sharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true);
            boolean recordOutgoingCalls = sharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true);

            if (recordIncomingCalls || recordOutgoingCalls) {
                // Stop
                if (MainService.sIsServiceRunning) {
                    AppUtil.stopMainService(context);
                }
                // Stop
            }
        }
    }
    // - Receiver relative
}
