package com.harsha.callrecorder.envr;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;

public class AppEnvr {
    // Logging
    public static final boolean LOG_V = true; // Verbose
    public static final boolean LOG_D = true; // Debug
    public static final boolean LOG_I = true; // Info
    public static final boolean LOG_W = true; // Warn
    public static final boolean LOG_E = true; // Error

    public static final boolean LOG_WTF = true; // What a Terrible Failure
    // - Logging

    // Application relative
    public static File sFilesDirMemory = null, sCacheDirMemory = null;
    public static String sFilesDirPathMemory = null, sCacheDirPathMemory = null;

    public static File sExternalFilesDirMemory = null, sExternalCacheDirMemory = null;
    public static String sExternalFilesDirPathMemory = null, sExternalCacheDirPathMemory = null;

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static String sProcessName = null;
    // - Application relative

    // Shared preferences key(s)
    public static final String SP_KEY_RECORD_INCOMING_CALLS = "record_incoming_calls";
    public static final String SP_KEY_RECORD_OUTGOING_CALLS = "record_outgoing_calls";
    // - Shared preferences key(s)

    // Fragment manager ("Settings" activity) shared preferences key(s)
    public static final String FM_SP_KEY_RECORDS_OUTPUT_LOCATION = "records_output_location";

    public static final String FM_SP_AUDIO_SOURCE = "audio_source";
    public static final String FM_SP_OUTPUT_FORMAT = "output_format";
    public static final String FM_SP_AUDIO_ENCODER = "audio_encoder";

    public static final String FM_SP_VIBRATE = "vibrate";
    public static final String FM_SP_TURN_ON_SPEAKER = "turn_on_speaker";
    public static final String FM_SP_MAX_UP_VOLUME = "max_up_volume";

    // Consent information (dialog)
    public static final String FM_SP_CHANGE_CONSENT_INFORMATION = "change_consent_information";
    // - Consent information (dialog)
    // - Fragment manager ("Settings" activity) shared preferences key(s)

    // In-app custom intent(s)
    public static final String INTENT_ACTION_INCOMING_CALL = "incoming_call";
    public static final String INTENT_ACTION_OUTGOING_CALL = "outgoing_call";
    // - In-app custom intent(s)
}
