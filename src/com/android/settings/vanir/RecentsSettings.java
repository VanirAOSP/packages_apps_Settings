/*
 * Copyright (C) 2014 VanirAOSP && the Android Open Source Project
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

package com.android.settings.vanir;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.vanir.fragments.DensityChanger;
import com.android.settings.SettingsPreferenceFragment;

import com.vanir.util.AbstractAsyncSuCMDProcessor;
import com.vanir.util.CMDProcessor;
import com.vanir.util.CMDProcessor.CommandResult;
import com.vanir.util.Helpers;
import com.vanir.util.RecentsConstants;

public class RecentsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String CUSTOM_RECENT_MODE = "custom_recent_mode";
    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE = "recent_panel_scale";
    private static final String RECENT_PANEL_EXPANDED_MODE = "recent_panel_expanded_mode";
    private static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "vanir_interface_recents_mem_display";
    private static final String RECENTS_CLEAR_ALL = "recents_clear_all";

    private static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    private static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
            .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");

    private CheckBoxPreference mMembar;
    private ListPreference mClearAll;
    private ListPreference mRecentsCustom;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.recents_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        mClearAll = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL);
        int value = Settings.System.getInt(getContentResolver(),
                Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, 1);
        mClearAll.setValue(String.valueOf(value));
        mClearAll.setSummary(mClearAll.getEntry());
        mClearAll.setOnPreferenceChangeListener(this);

        mMembar = (CheckBoxPreference) prefSet.findPreference(SYSTEMUI_RECENTS_MEM_DISPLAY);
        if (mMembar != null) {
        mMembar.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEMUI_RECENTS_MEM_DISPLAY, 0) == 1);
        }

        mRecentsCustom = (ListPreference) findPreference(CUSTOM_RECENT_MODE);
        mRecentsCustom.setSummary(mRecentsCustom.getEntry());
        mRecentsCustom.setOnPreferenceChangeListener(this);

        mRecentPanelLeftyMode = (CheckBoxPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        mRecentPanelScale = (ListPreference) findPreference(RECENT_PANEL_SCALE);
        String recentPanelScale = Settings.System.getString(getActivity().getContentResolver(), Settings.System.RECENT_PANEL_SCALE_FACTOR);
        if (recentPanelScale != null) {
            mRecentPanelScale.setValue(recentPanelScale);
        }
        mRecentPanelScale.setOnPreferenceChangeListener(this);

        mRecentPanelExpandedMode = (ListPreference) findPreference(RECENT_PANEL_EXPANDED_MODE);
        String recentPanelExpandedMode = Settings.System.getString(getActivity().getContentResolver(), Settings.System.RECENT_PANEL_EXPANDED_MODE);
        if (recentPanelExpandedMode != null) {
            mRecentPanelExpandedMode.setValue(recentPanelExpandedMode);
            mRecentPanelScale.setSummary(mRecentPanelScale.getEntry());
        }
        mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        int recentsStyle = Settings.System.getInt(getActivity().getContentResolver(),
                                Settings.System.CUSTOM_RECENTS, RecentsConstants.RECENTS_AOSP);

        mClearAll.setEnabled(recentsStyle == RecentsConstants.RECENTS_AOSP);
        mMembar.setEnabled(recentsStyle == RecentsConstants.RECENTS_AOSP);
        mRecentPanelLeftyMode.setEnabled(recentsStyle == RecentsConstants.RECENTS_SLIM);
        mRecentPanelScale.setEnabled(recentsStyle == RecentsConstants.RECENTS_SLIM);
        mRecentPanelExpandedMode.setEnabled(recentsStyle == RecentsConstants.RECENTS_SLIM);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        ContentResolver resolver = getActivity().getContentResolver();

        if (RECENTS_CLEAR_ALL.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mClearAll.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.CLEAR_RECENTS_BUTTON_LOCATION,
                    value);
            mClearAll.setSummary(mClearAll.getEntries()[index]);
            return true;

        } else if (preference == mRecentsCustom) {
            int value = Integer.parseInt((String) objValue);
            int index = mRecentsCustom.findIndexOfValue((String) objValue);

            int warningResource = -1;

            if (value == RecentsConstants.RECENTS_OMNI) {
                //make sure the user didn't uninstall OmniSwitch (stupid users. teehee)

                final PackageManager pm = getPackageManager();
                try {
                    pm.getPackageInfo(OMNISWITCH_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
                } catch (NameNotFoundException e) {
                    warningResource = R.string.omniswitch_not_installed_message;
                }
            }

            if (warningResource == -1) { //aka: we didn't scold the user for picking an option they broke intentionally
                if (value == RecentsConstants.RECENTS_OMNI)
                    warningResource = R.string.omniswitch_not_installed_message;

                Settings.System.putInt(resolver,
                        Settings.System.CUSTOM_RECENTS, value);
                mRecentsCustom.setSummary(mRecentsCustom.getEntries()[index]);
                mClearAll.setEnabled(value == RecentsConstants.RECENTS_AOSP);
                mMembar.setEnabled(value == RecentsConstants.RECENTS_AOSP);
                mRecentPanelLeftyMode.setEnabled(value == RecentsConstants.RECENTS_SLIM);
                mRecentPanelScale.setEnabled(value == RecentsConstants.RECENTS_SLIM);
                mRecentPanelExpandedMode.setEnabled(value == RecentsConstants.RECENTS_SLIM);
            }

            if (warningResource != -1) {
                new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.omniswitch_warning_title))
                .setMessage(getResources().getString(warningResource))
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
            }

            return true;

        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
            //updateRecentPanelScaleOptions(objValue);
            int index = mRecentPanelScale.findIndexOfValue((String) objValue);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
            mRecentPanelScale.setSummary(mRecentPanelScale.getEntries()[index]);
            return true;

        } else if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) objValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;

        } else if (preference == mRecentPanelExpandedMode) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_EXPANDED_MODE, value);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mMembar) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_RECENTS_MEM_DISPLAY, checked ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
