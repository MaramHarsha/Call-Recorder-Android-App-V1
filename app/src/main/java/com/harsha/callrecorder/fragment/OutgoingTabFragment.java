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
import com.harsha.callrecorder.adapter.OutgoingCallRecyclerViewAdapter;
import com.harsha.callrecorder.adapter.TabLayoutFragmentPagerAdapter;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.object.OutgoingCallObject;
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

public class OutgoingTabFragment extends Fragment implements TabLayoutFragmentPagerAdapter.ITabLayoutIconFragmentPagerAdapter {
    private static final String TAG = OutgoingTabFragment.class.getSimpleName();

    // Realm
    private Realm mRealm = null;
    private RealmResults<OutgoingCallObject> mOutgoingCallObjectRealmResults = null;
    // - Realm

    // Google AdMob
    private AdRequest mAdRequest = null;
    // - Google AdMob

    private SharedPreferences mSharedPreferences = null;

    private boolean mRecordOutgoingCalls = true; // Outgoing calls recording is always enabled by default

    private ScrollView mScrollView = null;

    private LinearLayout mMainLinearLayout = null;
    private SearchView mSearchView = null;
    private RecyclerView mRecyclerView = null;

    // Advertisement box
    private ProgressBar mAdvertisementBoxProgressBar = null;
    private TextView mAdvertisementBoxNotAvailableTextView = null;
    private AdView mAdvertisementBoxAdView = null;
    // - Advertisement box

    public OutgoingTabFragment() {
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
                mOutgoingCallObjectRealmResults = mRealm.where(OutgoingCallObject.class)
                        .greaterThan("mEndTimestamp", 0L)
                        .sort("mBeginTimestamp", Sort.DESCENDING)
                        .findAll();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            if (mOutgoingCallObjectRealmResults != null) {
                mOutgoingCallObjectRealmResults.addChangeListener(outgoingCallObjectRealmResults -> {
                    /*if (!outgoingCallObjectRealmResults.isEmpty()) {*/
                        if (mRecyclerView != null) {
                            List<OutgoingCallObject> outgoingCallObjectList = null;

                            if (mRealm != null) {
                                outgoingCallObjectList = mRealm.copyFromRealm(outgoingCallObjectRealmResults);
                            }

                            if (outgoingCallObjectList == null) {
                                outgoingCallObjectList = new ArrayList<>(outgoingCallObjectRealmResults);
                            }

                            setAdapter(populateAdapter(mRecyclerView.getContext(), outgoingCallObjectList));
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

        if (mSharedPreferences.contains(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS)) {
            mRecordOutgoingCalls = mSharedPreferences.getBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, mRecordOutgoingCalls);
        } else {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, mRecordOutgoingCalls);
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
        if (mOutgoingCallObjectRealmResults != null) {
            mOutgoingCallObjectRealmResults.removeAllChangeListeners();

            mOutgoingCallObjectRealmResults = null;
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
        View view = inflater.inflate(R.layout.fragment_outgoing_tab, container, false); // Inflate the layout for this fragment

        mScrollView = view.findViewById(R.id.fragment_outgoing_tab_scroll_view);

        mMainLinearLayout = view.findViewById(R.id.fragment_outgoing_tab_main_linear_layout);
        mSearchView = view.findViewById(R.id.fragment_outgoing_tab_search_view);
        mRecyclerView = view.findViewById(R.id.fragment_outgoing_tab_recycler_view);

        // ----

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView.getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // ----

        // Advertisement box
        mAdvertisementBoxProgressBar = view.findViewById(R.id.fragment_outgoing_tab_advertisement_box_progress_bar);
        mAdvertisementBoxNotAvailableTextView = view.findViewById(R.id.fragment_outgoing_tab_advertisement_box_not_available_text_view);
        mAdvertisementBoxAdView = view.findViewById(R.id.fragment_outgoing_tab_advertisement_box_ad_view);
        // - Advertisement box

        /*if (mOutgoingCallObjectRealmResults != null && !mOutgoingCallObjectRealmResults.isEmpty()) {*/
            List<OutgoingCallObject> outgoingCallObjectList = null;

            if (mRealm != null) {
                outgoingCallObjectList = mRealm.copyFromRealm(mOutgoingCallObjectRealmResults);
            }

            if (outgoingCallObjectList == null) {
                outgoingCallObjectList = new ArrayList<>(mOutgoingCallObjectRealmResults);
            }

            setAdapter(populateAdapter(mRecyclerView.getContext(), outgoingCallObjectList));
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

        TextView recordOutgoingCallsTextView = view.findViewById(R.id.fragment_outgoing_tab_text_view);
        recordOutgoingCallsTextView.setText(getString(R.string.record_outgoing_calls_param, (mRecordOutgoingCalls ? getString(R.string.yes) : getString(R.string.no))));
        recordOutgoingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(mRecordOutgoingCalls ? R.drawable.ic_baseline_call_24px : R.drawable.ic_outline_call_24px, 0, 0, 0);
        /*recordOutgoingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(mRecordOutgoingCalls ? baselineCallDrawable : outlineCallDrawable, null, null, null);*/

        SwitchCompat recordOutgoingCallsSwitchCompat = view.findViewById(R.id.fragment_outgoing_tab_switch_compat);
        recordOutgoingCallsSwitchCompat.setChecked(mRecordOutgoingCalls);
        recordOutgoingCallsSwitchCompat.setOnCheckedChangeListener((compoundButton, b) -> {
            mRecordOutgoingCalls = b;

            recordOutgoingCallsTextView.setText(getString(R.string.record_outgoing_calls_param, (mRecordOutgoingCalls ? getString(R.string.yes) : getString(R.string.no))));
            recordOutgoingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(mRecordOutgoingCalls ? R.drawable.ic_baseline_call_24px : R.drawable.ic_outline_call_24px, 0, 0, 0);
            /*recordOutgoingCallsTextView.setCompoundDrawablesWithIntrinsicBounds(b ? baselineCallDrawable : outlineCallDrawable, null, null, null);*/

            if (mSharedPreferences != null) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(AppEnvr.SP_KEY_RECORD_OUTGOING_CALLS, mRecordOutgoingCalls);
                editor.apply();
            }

            if (mRecordOutgoingCalls && !MainService.sIsServiceRunning) {
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
        return "Outgoing";
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_baseline_call_made_24px;
    }

    // ----

    private OutgoingCallRecyclerViewAdapter populateAdapter(@NonNull Context context, @NonNull List<OutgoingCallObject> outgoingCallObjectList) {
        Calendar calendar = Calendar.getInstance();

        int todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR), yesterdayDayOfYear = todayDayOfYear - 1;

        boolean hasToday = false, hasYesterday = false;

        List<OutgoingCallObject> list = new ArrayList<>();

        // Today check
        if (!outgoingCallObjectList.isEmpty()) {
            calendar.setTime(new Date(outgoingCallObjectList.get(0).getBeginTimestamp()));

            if (calendar.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                hasToday = true;
            }

            if (hasToday) {
                list.add(new OutgoingCallObject(true, context.getString(R.string.today)));

                for (Iterator<OutgoingCallObject> iterator = outgoingCallObjectList.iterator(); iterator.hasNext(); ) {
                    OutgoingCallObject outgoingCallObject = iterator.next();

                    calendar.setTime(new Date(outgoingCallObject.getBeginTimestamp()));

                    if (calendar.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                        iterator.remove();

                        list.add(outgoingCallObject);
                    } else {
                        break;
                    }
                }

                list.get(list.size() - 1).setIsLastInCategory(true);
            }
        }
        // - Today check

        // Yesterday check
        if (!outgoingCallObjectList.isEmpty()) {
            calendar.setTime(new Date(outgoingCallObjectList.get(0).getBeginTimestamp()));

            if (calendar.get(Calendar.DAY_OF_YEAR) == yesterdayDayOfYear) {
                hasYesterday = true;
            }

            if (hasYesterday) {
                list.add(new OutgoingCallObject(true, context.getString(R.string.yesterday)));

                for (Iterator<OutgoingCallObject> iterator = outgoingCallObjectList.iterator(); iterator.hasNext(); ) {
                    OutgoingCallObject outgoingCallObject = iterator.next();

                    calendar.setTime(new Date(outgoingCallObject.getBeginTimestamp()));

                    if (calendar.get(Calendar.DAY_OF_YEAR) == yesterdayDayOfYear) {
                        iterator.remove();

                        list.add(outgoingCallObject);
                    } else {
                        break;
                    }
                }

                list.get(list.size() - 1).setIsLastInCategory(true);
            }
        }
        // - Yesterday check

        if (!outgoingCallObjectList.isEmpty()) {
            /*if (hasToday || hasYesterday) {*/
                list.add(new OutgoingCallObject(true, context.getString(R.string.older)));
            /*}*/

            list.addAll(outgoingCallObjectList);
        }

        // ----

        try {
            if (ActivityCompat.checkSelfPermission(getContextNonNull(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                return new OutgoingCallRecyclerViewAdapter(context, list, true);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        return new OutgoingCallRecyclerViewAdapter(context, list);
    }

    private void setAdapter(@NonNull OutgoingCallRecyclerViewAdapter outgoingCallRecyclerViewAdapter) {
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(outgoingCallRecyclerViewAdapter);
            mRecyclerView.setItemViewCacheSize(outgoingCallRecyclerViewAdapter.getItemCount());
        }

        if (mSearchView != null) {
            mSearchView.setQuery(null, true);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    Filter filter = outgoingCallRecyclerViewAdapter.getFilter();

                    if (filter != null) {
                        filter.filter(s);

                        mSearchView.clearFocus();

                        return true;
                    }

                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    Filter filter = outgoingCallRecyclerViewAdapter.getFilter();

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
