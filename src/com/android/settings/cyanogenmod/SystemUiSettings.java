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
import com.android.settings.vanir.NavringPreferenceSwitch;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.util.HardwareKeyNavbarHelper;

public class SystemUiSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_IMMERSIVE_MODE_STYLE = "immersive_mode_style";
    private static final String KEY_IMMERSIVE_MODE_STATE = "immersive_mode_state";
    private static final String KEY_IMMERSIVE_LOL = "immersive_mode_lol_profile";
    private static final String KEY_IMMERSIVE_ORIENTATION = "immersive_orientation";
    private static final String KEY_NAVRING_SWITCH = "navigation_bar_ring";
    private static final String KEY_BUTTON_NAVIGATION = "old_buttons_navigation";

    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mImmersiveOrientation;
    private ListPreference mImmersiveModePref;
    private CheckBoxPreference mImmersiveLOL;
    private CheckBoxPreference mExpandedDesktop;
    private SwitchPreference mImmersiveModeState;
    private Preference mNavigation;

    private NavringPreferenceSwitch mNavringPreference;
    private int immersiveModeValue;

    // in case the user rotates when in system ui settings...
    private boolean GROSS = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        mNavigation = (Preference) findPreference(KEY_BUTTON_NAVIGATION);
        if (!getResources().getBoolean(R.bool.config_userWantsLegacyFormat)) {
            getPreferenceScreen().removePreference(mNavigation);
        }

        mNavringPreference = (NavringPreferenceSwitch) findPreference(KEY_NAVRING_SWITCH);

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

        mImmersiveOrientation = (ListPreference) findPreference(KEY_IMMERSIVE_ORIENTATION);
        int orientationValue = Settings.System.getInt(getContentResolver(), Settings.System.IMMERSIVE_ORIENTATION, 0);
        final String strValueOrientation = String.valueOf(orientationValue);
        mImmersiveOrientation.setValue(strValueOrientation);
        smartSummary(mImmersiveOrientation, strValueOrientation);
        mImmersiveOrientation.setOnPreferenceChangeListener(this);
    
        mImmersiveModePref = (ListPreference) findPreference(KEY_IMMERSIVE_MODE_STYLE);
        immersiveModeValue = Settings.System.getInt(getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 2);
        setImmersiveModeEntries();
        updateImmersiveModeState();
        mImmersiveModePref.setOnPreferenceChangeListener(this);
    }

    // GROSS
    private void setImmersiveModeEntries() {
        if (GROSS) return;
        GROSS = true;

        final Resources res = getResources();
        boolean navbar = HardwareKeyNavbarHelper.hasNavbar();

        mImmersiveModePref.setEntries(res.getStringArray(
                navbar ? R.array.immersive_mode_entries : R.array.immersive_mode_entries_no_navbar));
        mImmersiveModePref.setEntryValues(res.getStringArray(
                navbar ? R.array.immersive_mode_values : R.array.immersive_mode_values_no_navbar));

        // we only need to disabled and no statusbar here unless there's a navbar..
        if (navbar && (immersiveModeValue == 1 || immersiveModeValue == 3 )) {
            Log.w("ImmersiveModePreferences", "Selected value is outside of entries range. Using default == 2");
            immersiveModeValue = 2;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, immersiveModeValue);
        }
        final String strValue = String.valueOf(immersiveModeValue);
        mImmersiveModePref.setValue(strValue);
        smartSummary(mImmersiveModePref, strValue);
    }

    private void updateImmersiveModeState() {
        mExpandedDesktop.setEnabled(immersiveModeValue > 0);
        mImmersiveOrientation.setEnabled(immersiveModeValue > 0);
        mImmersiveModeState.setEnabled(immersiveModeValue > 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        setImmersiveModeEntries();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mNavringPreference) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (preference == mImmersiveModePref) {
            final String strValue = (String) objValue;
            immersiveModeValue = Integer.valueOf(strValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, immersiveModeValue);
            updateImmersiveModeState();
            smartSummary(mImmersiveModePref, strValue);
            updateRebootDialog();
            return true;

        } else if (preference == mImmersiveOrientation) {
            final String strValue = (String)objValue;
            int value = Integer.valueOf(strValue);
            Settings.System.putInt(getContentResolver(), Settings.System.IMMERSIVE_ORIENTATION,
                    value);
            smartSummary(mImmersiveOrientation, strValue);
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
            updateRebootDialog();
            return true;

        } else if (preference == mImmersiveModeState) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STATE,
                    (Boolean) objValue ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateRebootDialog() {
        Intent u = new Intent();
        u.setAction("com.android.powermenu.ACTION_UPDATE_REBOOT_DIALOG");
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }

    private void smartSummary(final ListPreference pref, final String value) {
        pref.setSummary(pref.getEntries()[pref.findIndexOfValue(value)]);
    }
}
