/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.profiles;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Profile;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.profiles.triggers.BluetoothTriggerFragment;
import com.android.settings.profiles.triggers.NfcTriggerFragment;
import com.android.settings.profiles.triggers.WifiTriggerFragment;
import com.google.android.collect.Lists;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * A {@link android.support.v4.app.FragmentPagerAdapter} class for swiping
 * between trigger fragments<br/>
 */
public class TriggerPagerAdapter extends FragmentPagerAdapter {

    private final SparseArray<WeakReference<Fragment>> mFragmentArray = new SparseArray<WeakReference<Fragment>>();

    private final List<Holder> mHolderList = Lists.newArrayList();

    private final Activity mFragmentActivity;

    private int mCurrentPage;

    /**
     * Constructor of <code>PagerAdatper<code>
     *
     * @param activity The {@link android.support.v4.app.FragmentActivity} of the
     *            {@link android.support.v4.app.Fragment}.
     * @param fm the FragmentManager to use.
     */
    public TriggerPagerAdapter(Activity activity, FragmentManager fm) {
        super(fm);
        mFragmentActivity = activity;
    }

    /**
     * Method that adds a new fragment class to the viewer (the fragment is
     * internally instantiate)
     *
     * @param className The full qualified name of fragment class.
     * @param params The instantiate params.
     */
    @SuppressWarnings("synthetic-access")
    public void add(final Class<? extends Fragment> className, final Bundle params) {
        final Holder mHolder = new Holder();
        mHolder.mClassName = className.getName();
        mHolder.mParams = params;

        final int mPosition = mHolderList.size();
        mHolderList.add(mPosition, mHolder);
        notifyDataSetChanged();
    }

    /**
     * Method that returns the {@link android.support.v4.app.Fragment} in the argument
     * position.
     *
     * @param position The position of the fragment to return.
     * @return Fragment The {@link android.support.v4.app.Fragment} in the argument position.
     */
    public Fragment getFragment(final int position) {
        final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null && mWeakFragment.get() != null) {
            return mWeakFragment.get();
        }
        return getItem(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        final Fragment mFragment = (Fragment)super.instantiateItem(container, position);
        final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null) {
            mWeakFragment.clear();
        }
        mFragmentArray.put(position, new WeakReference<Fragment>(mFragment));
        return mFragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Fragment getItem(final int position) {
        final Holder mCurrentHolder = mHolderList.get(position);
        final Fragment mFragment = Fragment.instantiate(mFragmentActivity,
                mCurrentHolder.mClassName, mCurrentHolder.mParams);
        return mFragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyItem(final ViewGroup container, final int position, final Object object) {
        super.destroyItem(container, position, object);
        final WeakReference<Fragment> mWeakFragment = mFragmentArray.get(position);
        if (mWeakFragment != null) {
            mWeakFragment.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mHolderList.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getPageTitle(final int position) {
        return mFragmentActivity.getResources().getStringArray(R.array.trigger_page_title)[position]
                .toUpperCase(Locale.getDefault());
    }

    /**
     * Method that returns the current page position.
     *
     * @return int The current page.
     */
    public int getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * Method that sets the current page position.
     *
     * @param currentPage The current page.
     */
    protected void setCurrentPage(final int currentPage) {
        mCurrentPage = currentPage;
    }

    /**
     * An enumeration of all the main fragments supported.
     */
    public enum TriggerFragments {
        /**
         * The wifi trigger fragment
         */
        WIFI(WifiTriggerFragment.class),
        /**
         * The bluetooth trigger fragment
         */
        BLUETOOTH(BluetoothTriggerFragment.class),
        /**
         * The nfc trigger fragment
         */
        NFC(NfcTriggerFragment.class);

        private Class<? extends Fragment> mFragmentClass;

        /**
         * Constructor of <code>TriggerFragments</code>
         *
         * @param fragmentClass The fragment class
         */
        private TriggerFragments(final Class<? extends Fragment> fragmentClass) {
            mFragmentClass = fragmentClass;
        }

        /**
         * Method that returns the fragment class.
         *
         * @return Class<? extends Fragment> The fragment class.
         */
        public Class<? extends Fragment> getFragmentClass() {
            return mFragmentClass;
        }

    }

    /**
     * A private class with information about fragment initialization
     */
    private final static class Holder {
        String mClassName;

        Bundle mParams;
    }
}
