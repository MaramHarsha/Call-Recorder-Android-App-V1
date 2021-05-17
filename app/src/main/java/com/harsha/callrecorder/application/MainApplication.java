package com.harsha.callrecorder.application;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.os.Build;

import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.util.LogUtil;

import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();

    private final RealmMigration mRealmMigration = (realm, oldVersion, newVersion) -> {
    };

    @Override
    public void onCreate() {
        // Files directory (internal)
        AppEnvr.sFilesDirMemory = getFilesDir();
        AppEnvr.sFilesDirPathMemory = getFilesDir().getPath();
        // - Files directory (internal)

        // Cache directory (internal)
        AppEnvr.sCacheDirMemory = getCacheDir();
        AppEnvr.sCacheDirPathMemory = getCacheDir().getPath();
        // - Cache directory (internal)

        // Files directory (external)
        try {
            AppEnvr.sExternalFilesDirMemory = getExternalFilesDir(null);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
        try {
            AppEnvr.sExternalFilesDirPathMemory = Objects.requireNonNull(getExternalFilesDir(null)).getPath();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
        // - Files directory (external)

        // Cache directory (external)
        AppEnvr.sExternalCacheDirMemory = getExternalCacheDir();
        AppEnvr.sExternalCacheDirPathMemory = Objects.requireNonNull(getExternalCacheDir()).getPath();
        // - Cache directory (external)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AppEnvr.sProcessName = getProcessName();
        }

        // ----

        super.onCreate();
        LogUtil.d(TAG, "Application create");

        // Realm
        Realm.init(this);

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .migration(mRealmMigration)
                .build();

        LogUtil.d(TAG, "Realm configuration schema version: " + realmConfiguration.getSchemaVersion());

        Realm.setDefaultConfiguration(realmConfiguration);
        // - Realm
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LogUtil.d(TAG, "Application trim memory");

        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                LogUtil.d(TAG, "Application trim memory: Running moderate");

                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                LogUtil.d(TAG, "Application trim memory: Running low");

                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                LogUtil.d(TAG, "Application trim memory: Running critical");

                break;

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                LogUtil.d(TAG, "Application trim memory: UI hidden");

                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                LogUtil.d(TAG, "Application trim memory: Background");

                break;
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                LogUtil.d(TAG, "Application trim memory: Moderate");

                break;
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                LogUtil.d(TAG, "Application trim memory: Complete");

                // Files directory (internal)
                if (AppEnvr.sFilesDirMemory == null) {
                    AppEnvr.sFilesDirMemory = getFilesDir();
                }
                if (AppEnvr.sFilesDirPathMemory == null) {
                    AppEnvr.sFilesDirPathMemory = getFilesDir().getPath();
                }
                // - Files directory (internal)

                // Cache directory (internal)
                if (AppEnvr.sCacheDirMemory == null) {
                    AppEnvr.sCacheDirMemory = getCacheDir();
                }
                if (AppEnvr.sCacheDirPathMemory == null) {
                    AppEnvr.sCacheDirPathMemory = getCacheDir().getPath();
                }
                // - Cache directory (internal)

                // Files directory (external)
                if (AppEnvr.sExternalFilesDirMemory == null) {
                    try {
                        AppEnvr.sExternalFilesDirMemory = getExternalFilesDir(null);
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }
                if (AppEnvr.sExternalFilesDirPathMemory == null) {
                    try {
                        AppEnvr.sExternalFilesDirPathMemory = Objects.requireNonNull(getExternalFilesDir(null)).getPath();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }
                // - Files directory (external)

                // Cache directory (external)
                if (AppEnvr.sExternalCacheDirMemory == null) {
                    AppEnvr.sExternalCacheDirMemory = getExternalCacheDir();
                }
                if (AppEnvr.sExternalCacheDirPathMemory == null) {
                    AppEnvr.sExternalCacheDirPathMemory = Objects.requireNonNull(getExternalCacheDir()).getPath();
                }
                // - Cache directory (external)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (AppEnvr.sProcessName == null) {
                        AppEnvr.sProcessName = getProcessName();
                    }
                }

                break;
        }
    }
}
