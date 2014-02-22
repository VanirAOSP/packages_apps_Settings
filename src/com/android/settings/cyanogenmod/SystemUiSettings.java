/*
 * Copyright (C) 2012 The CyanogenMod project
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

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

public class SystemUiSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_IMMERSIVE_MODE_STYLE = "immersive_mode_style";
    private static final String KEY_IMMERSIVE_MODE_STATE = "immersive_mode_state";
    private static final String KEY_IMMERSIVE_LOL = "immersive_mode_lol_profile";

    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mImmersiveModePref;
    private CheckBoxPreference mImmersiveLOL;
    private CheckBoxPreference mExpandedDesktop;
    private SwitchPreference mImmersiveModeState;

    private int immersiveModeValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        mImmersiveModeState = (SwitchPreference) findPreference(KEY_IMMERSIVE_MODE_STATE);
        mImmersiveModeState.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STATE, 0) == 1);
        mImmersiveModeState.setOnPreferenceChangeListener(this);        

        mImmersiveLOL = (CheckBoxPreference) findPreference(KEY_IMMERSIVE_LOL);
        mImmersiveLOL.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.IMMERSIVE_LOL_PROFILE, 0) == 1);
        mImmersiveLOL.setOnPreferenceChangeListener(this);  

        mExpandedDesktop = (CheckBoxPreference) findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktop.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.EXPANDED_DESKTOP, 0) == 1);
        mExpandedDesktop.setOnPreferenceChangeListener(this);

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);
    
        mImmersiveModePref = (ListPreference) findPreference(KEY_IMMERSIVE_MODE_STYLE);
        immersiveModeValue = Settings.System.getInt(getContentResolver(), Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 2);
        setImmersiveModeEntries();
        mImmersiveModePref.setValue(String.valueOf(immersiveModeValue));
        updateImmersiveModeSummary();
        updateImmersiveModeState();
        mImmersiveModePref.setOnPreferenceChangeListener(this);
    }

    private void setImmersiveModeEntries() {
        final Resources res = getResources();
        boolean navbarToggleState = Settings.System.getInt(getContentResolver(), Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1;
        
        mImmersiveModePref.setEntries(res.getStringArray(navbarToggleState ? R.array.immersive_mode_entries : R.array.immersive_mode_entries_no_navbar));
        mImmersiveModePref.setEntryValues(res.getStringArray(navbarToggleState ? R.array.immersive_mode_values : R.array.immersive_mode_values_no_navbar));
        if (immersiveModeValue >= mImmersiveModePref.getEntries().length) {
            Log.w("ImmersiveModePreferences", "Selected value is outside of entries range. Using default == 2");
            immersiveModeValue = 2;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, immersiveModeValue);
            mImmersiveModePref.setValue(String.valueOf(immersiveModeValue));
        }
    }

    private void updateImmersiveModeState() {
        mExpandedDesktop.setEnabled(immersiveModeValue > 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (preference == mImmersiveModePref) {
            immersiveModeValue = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, immersiveModeValue);
            updateImmersiveModeSummary();
            updateImmersiveModeState();
            updateRebootDialog();
            return true;

        } else if (preference == mImmersiveLOL) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.IMMERSIVE_LOL_PROFILE,
                    (Boolean) objValue ? 1 : 0);
            return true;

        } else if (preference == mExpandedDesktop) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.EXPANDED_DESKTOP,
                    (Boolean) objValue ? 1 : 0);
            return true;

        } else if (preference == mImmersiveModeState) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STATE,
                    (Boolean) objValue ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateImmersiveModeSummary() {
        Resources res = getResources();
        String summary = "Microwave your phone on HIGH for 30 minutes";
        switch (immersiveModeValue) {
            case 0: summary = res.getString(R.string.immersive_mode_disabled);
                break;
            case 1: summary = res.getString(R.string.summary_immersive_mode,
                                  res.getString(R.string.immersive_mode_summary_status_bar));
                break;
            case 2: summary = res.getString(R.string.summary_immersive_mode,
                                  res.getString(R.string.immersive_mode_summary_no_status_bar));
                break;
            case 3: summary = res.getString(R.string.summary_immersive_mode,
                                  res.getString(R.string.immersive_mode_summary_no_status_bar));
                break;
        }
        mImmersiveModePref.setSummary(summary);
    }

    private void updateRebootDialog() {
        Intent u = new Intent();
        u.setAction("com.android.powermenu.ACTION_UPDATE_REBOOT_DIALOG");
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }
}
