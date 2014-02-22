package com.android.settings.vanir.navbar;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;
import com.android.settings.R;

public class NavbarTabHostFragment extends Fragment implements OnTabChangeListener {

    private FragmentTabHost mTabHost;

    static String sLastTab;

    public NavbarTabHostFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.container);

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        if (deviceKeys == 0) {
            mTabHost.addTab(mTabHost.newTabSpec("rearrange").setIndicator(getString(R.string.navbar_tab_arrange)),
                    ArrangeNavbarFragment.class, null);
            mTabHost.addTab(mTabHost.newTabSpec("settings").setIndicator(getString(R.string.navbar_tab_settings)),
                    NavbarSettingsFragment.class, null);
        } else {
            mTabHost.addTab(mTabHost.newTabSpec("settings").setIndicator(getString(R.string.navbar_tab_settings)),
                    NavbarSettingsFragment.class, null);
            mTabHost.addTab(mTabHost.newTabSpec("rearrange").setIndicator(getString(R.string.navbar_tab_arrange)),
                    ArrangeNavbarFragment.class, null);
        }

        mTabHost.setOnTabChangedListener(this);
        return mTabHost;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sLastTab != null) {
            mTabHost.setCurrentTabByTag(sLastTab);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTabHost = null;
    }

    @Override
    public void onTabChanged(String s) {
        sLastTab = s;
    }
}
