package com.harsha.callrecorder.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

public class TabLayoutFragmentStatePagerAdapter extends FragmentStatePagerAdapter {
    // Adapter interface(s)
    public interface ITabLayoutFragmentStatePagerAdapter {
        Fragment getItem();

        CharSequence getPageTitle();
    }

    public interface ITabLayoutIconFragmentStatePagerAdapter {
        Fragment getItem();

        CharSequence getPageTitle();

        // Fragment additional
        int getIcon();
        // - Fragment additional
    }
    // - Adapter interface(s)

    // ----

    private List<ITabLayoutFragmentStatePagerAdapter> mTabLayoutFragmentStatePagerAdapterList;
    private List<ITabLayoutIconFragmentStatePagerAdapter> mTabLayoutIconFragmentStatePagerAdapterList;

    public TabLayoutFragmentStatePagerAdapter(@NonNull FragmentManager fragmentManager,
                                              @Nullable List<ITabLayoutFragmentStatePagerAdapter> tabLayoutFragmentStatePagerAdapterList,
                                              @Nullable List<ITabLayoutIconFragmentStatePagerAdapter> tabLayoutIconFragmentStatePagerAdapterList) {
        super(fragmentManager);

        if (tabLayoutFragmentStatePagerAdapterList != null) {
            mTabLayoutFragmentStatePagerAdapterList = tabLayoutFragmentStatePagerAdapterList;

            return;
        }

        if (tabLayoutIconFragmentStatePagerAdapterList != null) {
            mTabLayoutIconFragmentStatePagerAdapterList = tabLayoutIconFragmentStatePagerAdapterList;
        }
    }

    @Override
    public Fragment getItem(int i) {
        if (mTabLayoutFragmentStatePagerAdapterList != null) {
            return mTabLayoutFragmentStatePagerAdapterList.get(i).getItem();
        }

        if (mTabLayoutIconFragmentStatePagerAdapterList != null) {
            return mTabLayoutIconFragmentStatePagerAdapterList.get(i).getItem();
        }

        return null;
    }

    @Override
    public int getCount() {
        if (mTabLayoutFragmentStatePagerAdapterList != null) {
            return mTabLayoutFragmentStatePagerAdapterList.size();
        }

        if (mTabLayoutIconFragmentStatePagerAdapterList != null) {
            return mTabLayoutIconFragmentStatePagerAdapterList.size();
        }

        return 0;
    }

    @Override
    public CharSequence getPageTitle(int i) {
        if (mTabLayoutFragmentStatePagerAdapterList != null) {
            return mTabLayoutFragmentStatePagerAdapterList.get(i).getPageTitle();
        }

        if (mTabLayoutIconFragmentStatePagerAdapterList != null) {
            return mTabLayoutIconFragmentStatePagerAdapterList.get(i).getPageTitle();
        }

        return null;
    }
}
