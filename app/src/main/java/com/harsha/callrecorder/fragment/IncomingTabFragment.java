package com.harsha.callrecorder.fragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.harsha.callrecorder.R;
import com.harsha.callrecorder.adapter.IncomingCallRecyclerViewAdapter;
import com.harsha.callrecorder.adapter.TabLayoutFragmentPagerAdapter;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.object.IncomingCallObject;
import com.harsha.callrecorder.service.MainService;
import com.harsha.callrecorder.util.AppUtil;
import com.harsha.callrecorder.util.LogUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class IncomingTabFragment extends Fragment implements TabLayoutFragmentPagerAdapter.ITabLayoutIconFragmentPagerAdapter {
    private static final String TAG = IncomingTabFragment.class.getSimpleName();

    // Realm
    private Realm mRealm = null;
    private RealmResults<IncomingCallObject> mIncomingCallObjectRealmResults = null;
    // - Realm

    // Google AdMob
    private AdRequest mAdRequest = null;
    // - Google AdMob

    private SharedPreferences mSharedPreferences = null;

    private boolean mRecordIncomingCalls = true; // Incoming calls recording is always enabled by default

    private ScrollView mScrollView = null;

    private LinearLayout mMainLinearLayout = null;
    private SearchView mSearchView = null;
    private RecyclerView mRecyclerView = null;

    // Advertisement box
    private ProgressBar mAdvertisementBoxProgressBar = null;
    private TextView mAdvertisementBoxNotAvailableTextView = null;
    private AdView mAdvertisementBoxAdView = null;
    // - Advertisement box

    public IncomingTabFragment() {
        // Required empty public constructor
    }

    private Context getContextNonNull() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Objects.requireNonNull(getContext());
        } else {
            return getContext();
        }
    }

    // ----

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "Fragment create");

        // Realm
        try {
            mRealm = Realm.getDefaultInstance();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mRealm != null && !mRealm.isClosed()) {
            try {
                mIncomingCallObjectRealmResults = mRealm.where(IncomingCallObject.class)
                        .greaterThan("mEndTimestamp", 0L)
                        .sort("mBeginTimestamp", Sort.DESCENDING)
                        .findAll();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            if (mIncomingCallObjectRealmResults != null) {
                mIncomingCallObjectRealmResults.addChangeListener(incomingCallObjectRealmResults -> {
                    /*if (!incomingCallObjectRealmResults.isEmpty()) {*/
                        if (mRecyclerView != null) {
                            List<IncomingCallObject> incomingCallObjectList = null;

                            if (mRealm != null) {
                                incomingCallObjectList = mRealm.copyFromRealm(incomingCallObjectRealmResults);
                            }

                            if (incomingCallObjectList == null) {
                                incomingCallObjectList = new ArrayList<>(incomingCallObjectRealmResults);
                            }

                            setAdapter(populateAdapter(mRecyclerView.getContext(), incomingCallObjectList));
                        }
                    /*}*/

                    // ----

                    updateLayouts();
                });
            }
        }
        // - Realm

        // Consent information
        ConsentInformation consentInformation = ConsentInformation.getInstance(getContextNonNull());
        // - Consent information

        // Google AdMob
        AdRequest.Builder builder = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
        builder.addTestDevice("8632890FFB195CCA67B145C4A69A06CF"); // TODO: Add your device ID (Android ID) from logcat output

        if (consentInformation != null && consentInformation.isRequestLocationInEeaOrUnknown()) {
            if (consentInformation.getConsentStatus() == ConsentStatus.NON_PERSONALIZED) {
                Bundle extras = new Bundle();
                extras.putString("npa", "1");

                builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
            }
        }

        mAdRequest = builder.build();
        // - Google AdMob

        try {
            mSharedPreferences = getContextNonNull().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mSharedPreferences.contains(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS)) {
            mRecordIncomingCalls = mSharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, mRecordIncomingCalls);
        } else {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, mRecordIncomingCalls);
            editor.apply();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.d(TAG, "Fragment resume");

        if (mRealm != null && !mRealm.isClosed()) {
            try {
                mRealm.refresh();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "Fragment destroy");

        if (mSharedPreferences != null) {
            mSharedPreferences = null;
        }

        // Google AdMob
        if (mAdRequest != null) {
            mAdRequest = null;
        }
        // - Google AdMob

        // Realm
        if (mIncomingCallObjectRealmResults != null) {
            mIncomingCallObjectRealmResults.removeAllChangeListeners();

            mIncomingCallObjectRealmResults = null;
        }

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
    }

    // ----

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incoming_tab, container, false); // Inflate the layout for this fragment

        mScrollView = view.findViewById(R.id.fragment_incoming_tab_scroll_view);

        mMainLinearLayout = view.findViewById(R.id.fragment_incoming_tab_main_linear_layout);
        mSearchView = view.findViewById(R.id.fragment_incoming_tab_search_view);
        mRecyclerView = view.findViewById(R.id.fragment_incoming_tab_recycler_view);

        // ----

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView.getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // ----

        // Advertisement box
        mAdvertisementBoxProgressBar = view.findViewById(R.id.fragment_incoming_tab_advertisement_box_progress_bar);
        mAdvertisementBoxNotAvailableTextView = view.findViewById(R.id.fragment_incoming_tab_advertisement_box_not_available_text_view);
        mAdvertisementBoxAdView = view.findViewById(R.id.fragment_incoming_tab_advertisement_box_ad_view);
        // - Advertisement box

        /*if (mIncomingCallObjectRealmResults != null && !mIncomingCallObjectRealmResults.isEmpty()) {*/
            List<IncomingCallObject> incomingCallObjectList = null;

            if (mRealm != null) {
                incomingCallObjectList = mRealm.copyFromRealm(mIncomingCallObjectRealmResults);
            }

            if (incomingCallObjectList == null) {
                incomingCallObjectList = new ArrayList<>(mIncomingCallObjectRealmResults);
            }

            setAdapter(populateAdapter(mRecyclerView.getContext(), incomingCallObjectList));
        /*}*/

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateLayouts();

        // ----

        /*Drawable baselineCallDrawable = ResourceUtil.getDrawable(view.getContext(), R.drawable.ic_baseline_call_24px);
        Drawable outlineCallDrawable = ResourceUtil.getDrawable(view.getContext(), R.drawable.ic_outline_call_24px);*/

        TextView recordIncomingCallsTextView = view.findViewById(R.id.fragment_incoming_tab_text_view);
        recordIncomingCallsTextView.setText(getString(R.string.record_incoming_calls_param, (mRecordIncomingCalls ? getString(R.string.yes) : getString(R.string.no))));
        recordIncomingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(mRecordIncomingCalls ? R.drawable.ic_baseline_call_24px : R.drawable.ic_outline_call_24px, 0, 0, 0);
        /*recordIncomingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(mRecordIncomingCalls ? baselineCallDrawable : outlineCallDrawable, null, null, null);*/

        SwitchCompat recordIncomingCallsSwitchCompat = view.findViewById(R.id.fragment_incoming_tab_switch_compat);
        recordIncomingCallsSwitchCompat.setChecked(mRecordIncomingCalls);
        recordIncomingCallsSwitchCompat.setOnCheckedChangeListener((compoundButton, b) -> {
            mRecordIncomingCalls = b;

            recordIncomingCallsTextView.setText(getString(R.string.record_incoming_calls_param, (mRecordIncomingCalls ? getString(R.string.yes) : getString(R.string.no))));
            recordIncomingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(mRecordIncomingCalls ? R.drawable.ic_baseline_call_24px : R.drawable.ic_outline_call_24px, 0, 0, 0);
            /*recordIncomingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(b ? baselineCallDrawable : outlineCallDrawable, null, null, null);*/

            if (mSharedPreferences != null) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_INCOMING_CALLS, mRecordIncomingCalls);
                editor.apply();
            }

            if (mRecordIncomingCalls && !MainService.sIsServiceRunning) {
                AppUtil.startMainService(view.getContext());
            }
        });
    }

    // ----

    @Override
    public Fragment getItem() {
        return this;
    }

    @Override
    public CharSequence getPageTitle() {
        return "Incoming";
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_baseline_call_received_24px;
    }

    // ----

    private IncomingCallRecyclerViewAdapter populateAdapter(@NonNull Context context, @NonNull List<IncomingCallObject> incomingCallObjectList) {
        Calendar calendar = Calendar.getInstance();

        int todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR), yesterdayDayOfYear = todayDayOfYear - 1;

        boolean hasToday = false, hasYesterday = false;

        List<IncomingCallObject> list = new ArrayList<>();

        // Today check
        if (!incomingCallObjectList.isEmpty()) {
            calendar.setTime(new Date(incomingCallObjectList.get(0).getBeginTimestamp()));

            if (calendar.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                hasToday = true;
            }

            if (hasToday) {
                list.add(new IncomingCallObject(true, context.getString(R.string.today)));

                for (Iterator<IncomingCallObject> iterator = incomingCallObjectList.iterator(); iterator.hasNext(); ) {
                    IncomingCallObject incomingCallObject = iterator.next();

                    calendar.setTime(new Date(incomingCallObject.getBeginTimestamp()));

                    if (calendar.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                        iterator.remove();

                        list.add(incomingCallObject);
                    } else {
                        break;
                    }
                }

                list.get(list.size() - 1).setIsLastInCategory(true);
            }
        }
        // - Today check

        // Yesterday check
        if (!incomingCallObjectList.isEmpty()) {
            calendar.setTime(new Date(incomingCallObjectList.get(0).getBeginTimestamp()));

            if (calendar.get(Calendar.DAY_OF_YEAR) == yesterdayDayOfYear) {
                hasYesterday = true;
            }

            if (hasYesterday) {
                list.add(new IncomingCallObject(true, context.getString(R.string.yesterday)));

                for (Iterator<IncomingCallObject> iterator = incomingCallObjectList.iterator(); iterator.hasNext(); ) {
                    IncomingCallObject incomingCallObject = iterator.next();

                    calendar.setTime(new Date(incomingCallObject.getBeginTimestamp()));

                    if (calendar.get(Calendar.DAY_OF_YEAR) == yesterdayDayOfYear) {
                        iterator.remove();

                        list.add(incomingCallObject);
                    } else {
                        break;
                    }
                }

                list.get(list.size() - 1).setIsLastInCategory(true);
            }
        }
        // - Yesterday check

        if (!incomingCallObjectList.isEmpty()) {
            /*if (hasToday || hasYesterday) {*/
                list.add(new IncomingCallObject(true, context.getString(R.string.older)));
            /*}*/

            list.addAll(incomingCallObjectList);
        }

        // ----

        try {
            if (ActivityCompat.checkSelfPermission(getContextNonNull(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                return new IncomingCallRecyclerViewAdapter(context, list, true);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        return new IncomingCallRecyclerViewAdapter(context, list);
    }

    private void setAdapter(@NonNull IncomingCallRecyclerViewAdapter incomingCallRecyclerViewAdapter) {
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(incomingCallRecyclerViewAdapter);
            mRecyclerView.setItemViewCacheSize(incomingCallRecyclerViewAdapter.getItemCount());
        }

        if (mSearchView != null) {
            mSearchView.setQuery(null, true);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    Filter filter = incomingCallRecyclerViewAdapter.getFilter();

                    if (filter != null) {
                        filter.filter(s);

                        mSearchView.clearFocus();

                        return true;
                    }

                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    Filter filter = incomingCallRecyclerViewAdapter.getFilter();

                    if (filter != null) {
                        filter.filter(s);

                        return true;
                    }

                    return false;
                }
            });
        }
    }

    private void updateLayouts() {
        if (mRecyclerView != null && mRecyclerView.getAdapter() != null && mRecyclerView.getAdapter().getItemCount() > 0) {
            if (mScrollView != null && mScrollView.getVisibility() != View.GONE) {
                mScrollView.setVisibility(View.GONE);
            }
            if (mMainLinearLayout != null && mMainLinearLayout.getVisibility() != View.VISIBLE) {
                mMainLinearLayout.setVisibility(View.VISIBLE);
            }
        } else {
            if (mMainLinearLayout != null && mMainLinearLayout.getVisibility() != View.GONE) {
                mMainLinearLayout.setVisibility(View.GONE);
            }
            if (mScrollView != null && mScrollView.getVisibility() != View.VISIBLE) {
                mScrollView.setVisibility(View.VISIBLE);
            }

            // Advertisement box
            if (mAdRequest != null) {
                mAdvertisementBoxAdView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        switch (errorCode) {
                            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                                break;
                            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                                break;
                            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                                break;
                            case AdRequest.ERROR_CODE_NO_FILL:
                                break;
                        }

                        // ----

                        if (mAdvertisementBoxProgressBar.getVisibility() != View.GONE) {
                            mAdvertisementBoxProgressBar.setVisibility(View.GONE);
                        }

                        if (mAdvertisementBoxAdView.getVisibility() != View.GONE) {
                            mAdvertisementBoxAdView.setVisibility(View.GONE);
                        }

                        if (mAdvertisementBoxNotAvailableTextView.getVisibility() != View.VISIBLE) {
                            mAdvertisementBoxNotAvailableTextView.setVisibility(View.VISIBLE);
                        }

                        // ----

                        super.onAdFailedToLoad(errorCode);
                    }

                    @Override
                    public void onAdLoaded() {
                        if (mAdvertisementBoxProgressBar.getVisibility() != View.GONE) {
                            mAdvertisementBoxProgressBar.setVisibility(View.GONE);
                        }

                        if (mAdvertisementBoxNotAvailableTextView.getVisibility() != View.GONE) {
                            mAdvertisementBoxNotAvailableTextView.setVisibility(View.GONE);
                        }

                        if (mAdvertisementBoxAdView.getVisibility() != View.VISIBLE) {
                            mAdvertisementBoxAdView.setVisibility(View.VISIBLE);
                        }

                        // ----

                        super.onAdLoaded();
                    }
                });

                mAdvertisementBoxAdView.loadAd(mAdRequest);
            } else {
                if (mAdvertisementBoxProgressBar.getVisibility() != View.GONE) {
                    mAdvertisementBoxProgressBar.setVisibility(View.GONE);
                }

                if (mAdvertisementBoxNotAvailableTextView.getVisibility() != View.VISIBLE) {
                    mAdvertisementBoxNotAvailableTextView.setVisibility(View.VISIBLE);
                }
            }
            // - Advertisement box
        }
    }
}
