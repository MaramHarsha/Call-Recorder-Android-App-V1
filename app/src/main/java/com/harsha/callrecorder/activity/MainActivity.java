package com.harsha.callrecorder.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.android.material.tabs.TabLayout;
import com.harsha.callrecorder.R;
import com.harsha.callrecorder.adapter.TabLayoutFragmentPagerAdapter;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.fragment.IncomingTabFragment;
import com.harsha.callrecorder.fragment.OutgoingTabFragment;
import com.harsha.callrecorder.service.MainService;
import com.harsha.callrecorder.util.AppUtil;
import com.harsha.callrecorder.util.LogUtil;
import com.harsha.callrecorder.util.RequestIgnoreBatteryOptimizationsUtil;
import com.harsha.callrecorder.util.ResourceUtil;

import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private TabLayout mTabLayout = null;
    private TabLayout.OnTabSelectedListener mOnTabSelectedListener = null;

    private SharedPreferences mSharedPreferences = null;

    // Consent information (dialog)
    private ConsentForm mConsentForm = null;
    private final ConsentFormListener mConsentFormListener = new ConsentFormListener() {
        @Override
        public void onConsentFormLoaded() {
            super.onConsentFormLoaded();
            LogUtil.d(TAG, "Consent form: Contest form loaded");

            if (mConsentForm != null) {
                if (!mConsentForm.isShowing()) {
                    mConsentForm.show();
                }
            }
        }

        @Override
        public void onConsentFormError(String reason) {
            super.onConsentFormError(reason);
            LogUtil.d(TAG, "Consent form: Consent form error");

            LogUtil.i(TAG, "Consent form: Contest form error: " + reason);
        }
    };
    // - Consent information (dialog)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    LogUtil.d(TAG, "Activity create");

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ArrayList<TabLayoutFragmentPagerAdapter.ITabLayoutIconFragmentPagerAdapter> tabLayoutIconFragmentPagerAdapterArrayList = new ArrayList<>();
        tabLayoutIconFragmentPagerAdapterArrayList.add(new IncomingTabFragment()); // "Incoming" tab
        tabLayoutIconFragmentPagerAdapterArrayList.add(new OutgoingTabFragment()); // "Outgoing" tab

        TabLayoutFragmentPagerAdapter tabLayoutFragmentPagerAdapter = new TabLayoutFragmentPagerAdapter(getSupportFragmentManager(), null, tabLayoutIconFragmentPagerAdapterArrayList);

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(tabLayoutFragmentPagerAdapter);

        mTabLayout = findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(viewPager);

        ColorFilter tabIconColorFilter = new PorterDuffColorFilter(ResourceUtil.getColor(this, R.color.tabTextColor), PorterDuff.Mode.SRC_IN);
        ColorFilter tabSelectedIconColorFilter = new PorterDuffColorFilter(ResourceUtil.getColor(this, R.color.tabSelectedTextColor), PorterDuff.Mode.SRC_IN);

        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = null;
            try {
                tab = mTabLayout.getTabAt(i);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            if (tab != null) {
                try {
                    tab.setIcon(tabLayoutIconFragmentPagerAdapterArrayList.get(i).getIcon());

                    Drawable icon = tab.getIcon();

                    if (icon != null) {
                        if (tab.getPosition() == 0) {
                            icon.setColorFilter(tabSelectedIconColorFilter);
                        } else {
                            icon.setColorFilter(tabIconColorFilter);
                        }
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }
        }

        mOnTabSelectedListener = new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }

                super.onTabSelected(tab);
                LogUtil.d(TAG, "Tab select");

                if (tab.getText() != null) {
                    LogUtil.i(TAG, "Tab select: " + tab.getText());
                }

                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(tabSelectedIconColorFilter);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }

                super.onTabUnselected(tab);
                LogUtil.d(TAG, "Tab unselect");

                if (tab.getText() != null) {
                    LogUtil.i(TAG, "Tab unselect: " + tab.getText());
                }

                if (tab.getIcon() != null) {
                    tab.getIcon().setColorFilter(tabIconColorFilter);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab == null) {
                    return;
                }

                super.onTabReselected(tab);
                LogUtil.d(TAG, "Tab reselect");

                if (tab.getText() != null) {
                    LogUtil.i(TAG, "Tab reselect: " + tab.getText());
                }
            }
        };

        mTabLayout.addOnTabSelectedListener(mOnTabSelectedListener);

        // ----

        try {
            mSharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mSharedPreferences != null) {
            if (!mSharedPreferences.contains(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS)) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true); // Incoming calls recording is always enabled by default
                editor.apply();
            }
            if (!mSharedPreferences.contains(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS)) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true); // Outgoing calls recording is always enabled by default
                editor.apply();
            }

            boolean recordIncomingCalls = mSharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, true);
            boolean recordOutgoingCalls = mSharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, true);

            if (recordIncomingCalls || recordOutgoingCalls) {
                // Start
                if (!MainService.sIsServiceRunning) {
                    AppUtil.startMainService(this);
                }
                // - Start
            }
        }

        // ----

        // Consent information (dialog)
        ConsentInformation consentInformation = ConsentInformation.getInstance(this);
        consentInformation.addTestDevice("8632890FFB195CCA67B145C4A69A06CF"); // TODO: Add your device ID (Android ID) from logcat output
        consentInformation.requestConsentInfoUpdate(new String[]{getString(R.string.admob_publisher_id)}, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                LogUtil.d(TAG, "Consent information: Consent info updated");

                switch (consentStatus) {
                    case UNKNOWN: // Unknown
                        LogUtil.i(TAG, "Consent information: Updated: Unknown");

                        URL privacyUrl = null;
                        try {
                            privacyUrl = new URL(getString(R.string.privacy_policy_url));
                        } catch (Exception e) {
                            LogUtil.e(TAG, e.getMessage());
                            LogUtil.e(TAG, e.toString());

                            e.printStackTrace();
                        }

                        if (mConsentForm == null) {
                            mConsentForm = new ConsentForm.Builder(MainActivity.this, privacyUrl)
                                    .withListener(mConsentFormListener)
                                    .withPersonalizedAdsOption()
                                    .withNonPersonalizedAdsOption()
                                    .build();
                        }

                        if (mConsentForm != null) {
                            mConsentForm.load();
                        }

                        break;
                    case NON_PERSONALIZED: // Non-personalized
                        LogUtil.i(TAG, "Consent information: Updated: Non-personalized");

                        break;
                    case PERSONALIZED: // Personalized
                        LogUtil.i(TAG, "Consent information: Updated: Personalized");

                        break;
                }
            }

            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                LogUtil.d(TAG, "Consent information: Failed to update consent info");

                LogUtil.i(TAG, "Consent information: Failed to update consent info: " + errorDescription);
            }
        });
        // - Consent information (dialog)
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.d(TAG, "Activity start");

        // Runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> runtimePermissionsArrayList = new ArrayList<>();
            runtimePermissionsArrayList.add(Manifest.permission.INTERNET);
            runtimePermissionsArrayList.add(Manifest.permission.READ_PHONE_STATE);
            runtimePermissionsArrayList.add(Manifest.permission.CALL_PHONE);
            runtimePermissionsArrayList.add(Manifest.permission.RECORD_AUDIO);
            runtimePermissionsArrayList.add(Manifest.permission.VIBRATE);
            runtimePermissionsArrayList.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
            runtimePermissionsArrayList.add(Manifest.permission.READ_CONTACTS);
            runtimePermissionsArrayList.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
            runtimePermissionsArrayList.add(Manifest.permission.WAKE_LOCK);
            runtimePermissionsArrayList.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runtimePermissionsArrayList.add(Manifest.permission.FOREGROUND_SERVICE);
            }

            if (!runtimePermissionsArrayList.isEmpty()) {
                ArrayList<String> requestRuntimePermissionsArrayList = new ArrayList<>();

                for (String requestRuntimePermission : runtimePermissionsArrayList) {
                    if (checkSelfPermission(requestRuntimePermission) != PackageManager.PERMISSION_GRANTED) { // "if (checkSelfPermission(requestRuntimePermission) == PackageManager.PERMISSION_DENIED)"
                        requestRuntimePermissionsArrayList.add(requestRuntimePermission);
                    }
                }

                if (!requestRuntimePermissionsArrayList.isEmpty()) { // Request runtime permissions
                    requestPermissions(requestRuntimePermissionsArrayList.toArray(new String[0]), 1); // Go with 1 (for all)
                }
            }
        }
        // - Runtime permissions

        // ----

        // 1. Ignore battery optimization settings - Entire application (API 23+ only)
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED) {
                PowerManager powerManager = null;
                try {
                    powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (powerManager != null) {
                    if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        LogUtil.i(TAG, "1. Ignore battery optimization settings - Entire application: Enabled");
                    } else {
                        LogUtil.w(TAG, "1. Ignore battery optimization settings - Entire application: Not enabled");

                        // ----

                        // Relative
                        startActivityForResult(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), 1); // Go with 1
                        // - Relative
                    }
                }
            }
        }*/
        // - 1. Ignore battery optimization settings - Entire application (API 23+ only)*/

        // 2. Request ignore battery optimizations ("1." alternative; with package URI) - Entire application (API 23+ only)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_GRANTED) {
                PowerManager powerManager = null;
                try {
                    powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (powerManager != null) {
                    if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        LogUtil.i(TAG, "2. Request ignore battery optimizations (\"1.\" alternative; with package URI) - Entire application: Enabled");
                    } else {
                        LogUtil.w(TAG, "2. Request ignore battery optimizations (\"1.\" alternative; with package URI) - Entire application: Not enabled");

                        // ----

                        // Relative
                        Intent intent = RequestIgnoreBatteryOptimizationsUtil.getRequestIgnoreBatteryOptimizationsIntent(this);

                        if (intent != null) {
                            startActivityForResult(intent, 2); // Go with 2
                        }
                        // - Relative
                    }
                }
            }
        }
        // - 2. Request ignore battery optimizations ("1." alternative; with package URI) - Entire application (API 23+ only)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "Activity destroy");

        // Consent information (dialog)
        if (mConsentForm != null) {
            mConsentForm = null;
        }
        // - Consent information (dialog)

        // ----

        if (mSharedPreferences != null) {
            mSharedPreferences = null;
        }

        // ----

        if (mTabLayout != null) {
            if (mOnTabSelectedListener != null) {
                mTabLayout.removeOnTabSelectedListener(mOnTabSelectedListener);

                mOnTabSelectedListener = null;
            }

            mTabLayout = null;
        }
    }


    // ----

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu == null) {
            return false;
        }

        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        /*if (menuItem == null) {
            return false;
        }*/

        switch (menuItem.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));

                return true;

            case R.id.action_open_in_market:
                AppUtil.openPackageInMarket(this);

                return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    // ----

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: // "Go with 1 (for all)"
                if (grantResults.length > 0 && grantResults.length == permissions.length) {
                    boolean allGranted = true;

                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false;
                        }
                    }

                    if (allGranted) {
                        LogUtil.i(TAG, "All requested permissions are granted");
                    } else {
                        LogUtil.w(TAG, "Not all requested permissions are granted");

                        // ----

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.runtime_permissions_not_granted_title));
                        builder.setMessage(getString(R.string.runtime_permissions_not_granted_message));
                        builder.setNeutralButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());

                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                }
                break;
        }
    }
}
