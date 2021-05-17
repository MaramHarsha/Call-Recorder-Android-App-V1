package com.harsha.callrecorder.activity;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.harsha.callrecorder.R;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.util.LogUtil;

import java.net.URL;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "Activity create");

        setContentView(R.layout.settings_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // ----

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        /*if (menuItem == null) {
            return false;
        }*/

        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();

                return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    // ----

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Context getContextNonNull() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return Objects.requireNonNull(getContext());
            } else {
                return getContext();
            }
        }

        // Consent information (dialog)
        private Dialog mCannotChangeConsentInformationDialog = null;

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

                if (mCannotChangeConsentInformationDialog != null) {
                    if (!mCannotChangeConsentInformationDialog.isShowing()) {
                        mCannotChangeConsentInformationDialog.show();
                    }
                }
            }
        };
        // - Consent information (dialog)

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference changeConsentInformationPreference = findPreference(AppEnvr.FM_SP_CHANGE_CONSENT_INFORMATION);
            if (changeConsentInformationPreference != null) {
                changeConsentInformationPreference.setOnPreferenceClickListener(preference -> {
                    // Consent information (dialog)
                    mCannotChangeConsentInformationDialog = new AlertDialog.Builder(getContextNonNull())
                            .setTitle("Change consent information")
                            .setMessage("Consent information cannot be changed at the moment. " +
                                    "Check your connection and try again or come back to try again later. " +
                                    "Contact the developer if the problem persist for more than 24 hours.")
                            .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                            .create();

                    // ----

                    ConsentInformation consentInformation = ConsentInformation.getInstance(getContextNonNull());
                    consentInformation.addTestDevice("8632890FFB195CCA67B145C4A69A06CF"); // TODO: Add your device ID (Android ID) from logcat output
                    consentInformation.requestConsentInfoUpdate(new String[]{getString(R.string.admob_publisher_id)}, new ConsentInfoUpdateListener() {
                        @Override
                        public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                            LogUtil.d(TAG, "Consent information: Consent info updated");

                            URL privacyUrl = null;
                            try {
                                privacyUrl = new URL(getString(R.string.privacy_policy_url));
                            } catch (Exception e) {
                                LogUtil.e(TAG, e.getMessage());
                                LogUtil.e(TAG, e.toString());

                                e.printStackTrace();
                            }

                            if (mConsentForm == null) {
                                mConsentForm = new ConsentForm.Builder(getContextNonNull(), privacyUrl)
                                        .withListener(mConsentFormListener)
                                        .withPersonalizedAdsOption()
                                        .withNonPersonalizedAdsOption()
                                        .build();
                            }

                            if (mConsentForm != null) {
                                mConsentForm.load();
                            }
                        }

                        @Override
                        public void onFailedToUpdateConsentInfo(String errorDescription) {
                            LogUtil.d(TAG, "Consent information: Failed to update consent info");

                            LogUtil.i(TAG, "Consent information: Failed to update consent info: " + errorDescription);

                            if (mCannotChangeConsentInformationDialog != null) {
                                if (!mCannotChangeConsentInformationDialog.isShowing()) {
                                    mCannotChangeConsentInformationDialog.show();
                                }
                            }
                        }
                    });
                    // - Consent information (dialog)

                    return false;
                });
            }
        }
    }
}
