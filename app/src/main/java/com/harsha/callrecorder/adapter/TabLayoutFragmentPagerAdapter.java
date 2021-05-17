package com.harsha.callrecorder.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

public class TabLayoutFragmentPagerAdapter extends FragmentPagerAdapter {
    // Adapter interface(s)
    public interface ITabLayoutFragmentPagerAdapter {
        Fragment getItem();

        CharSequence getPageTitle();
    }

    public interface ITabLayoutIconFragmentPagerAdapter {
        Fragment getItem();

        CharSequence getPageTitle();

        // Fragment additional
        int getIcon();
        // - Fragment additional
    }
    // - Adapter interface(s)

    // ----

    private List<ITabLayoutFragmentPagerAdapter> mTabLayoutFragmentPagerAdapterList;
    private List<ITabLayoutIconFragmentPagerAdapter> mTabLayoutIconFragmentPagerAdapterList;

    public TabLayoutFragmentPagerAdapter(@NonNull FragmentManager fragmentManager,
                                         @Nullable List<ITabLayoutFragmentPagerAdapter> tabLayoutFragmentPagerAdapterList,
                                         @Nullable List<ITabLayoutIconFragmentPagerAdapter> tabLayoutIconFragmentPagerAdapterList) {
        super(fragmentManager);

        if (tabLayoutFragmentPagerAdapterList != null) {
            mTabLayoutFragmentPagerAdapterList = tabLayoutFragmentPagerAdapterList;

            return;
        }

        if (tabLayoutIconFragmentPagerAdapterList != null) {
            mTabLayoutIconFragmentPagerAdapterList = tabLayoutIconFragmentPagerAdapterList;
        }
    }

    @Override
    public Fragment getItem(int i) {
        if (mTabLayoutFragmentPagerAdapterList != null) {
            return mTabLayoutFragmentPagerAdapterList.get(i).getItem();
        }

        if (mTabLayoutIconFragmentPagerAdapterList != null) {
            return mTabLayoutIconFragmentPagerAdapterList.get(i).getItem();
        }

        return null;
    }

    @Override
    public int getCount() {
        if (mTabLayoutFragmentPagerAdapterList != null) {
            return mTabLayoutFragmentPagerAdapterList.size();
        }

        if (mTabLayoutIconFragmentPagerAdapterList != null) {
            return mTabLayoutIconFragmentPagerAdapterList.size();
        }

        return 0;
    }

    @Override
    public CharSequence getPageTitle(int i) {
        if (mTabLayoutFragmentPagerAdapterList != null) {
            return mTabLayoutFragmentPagerAdapterList.get(i).getPageTitle();
        }

        if (mTabLayoutIconFragmentPagerAdapterList != null) {
            return mTabLayoutIconFragmentPagerAdapterList.get(i).getPageTitle();
        }

        return null;
    }
}
