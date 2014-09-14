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

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
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
import com.android.settings.vanir.VanirPreferenceSwitch;
import com.android.settings.vanir.gesturepanel.GestureBuilderActivity;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.util.HardwareKeyNavbarHelper;

public class SystemUiSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_IMMERSIVE_MODE_STYLE = "immersive_mode_style";
    private static final String KEY_IMMERSIVE_MODE_STATE = "immersive_mode_state";
    private static final String KEY_IMMERSIVE_LOL = "immersive_mode_lol_profile";
    private static final String KEY_IMMERSIVE_ORIENTATION = "immersive_orientation";
    private static final String KEY_NAVRING_SWITCH = "navigation_bar_ring";
    private static final String KEY_NAVBAR_SWITCH = "navigation_bar";
    private static final String KEY_BUTTON_NAVIGATION = "old_buttons_navigation";
    private static final String HARDWARE_IMMERSIVE_STYLE = "hardware_immersive_style";
    private static final String IMMERSIVE_ENABLED = "immersive_enabled";
    private static final String IMMERSIVE_DISABLED = "immersive_disabled";

    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mImmersiveOrientation;
    private ListPreference mImmersiveModePref;
    private CheckBoxPreference mImmersiveLOL;
    private CheckBoxPreference mExpandedDesktop;
    private SwitchPreference mImmersiveModeState;
    private Preference mNavigation;

    private VanirPreferenceSwitch mNavringPreference;
    private VanirPreferenceSwitch mNavbarPreference;
    private int immersiveModeValue;
    private int deviceKeys;

    private SettingsObserver mSettingsObserver;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.ENABLE_NAVIGATION_BAR), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            final ContentResolver resolver = mContext.getContentResolver();

            boolean enabled = Settings.System.getInt(resolver,
                         Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1;

            setHardwareImmersiveState(enabled);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        mNavigation = (Preference) findPreference(KEY_BUTTON_NAVIGATION);
        if (!getResources().getBoolean(R.bool.config_userWantsLegacyFormat)) {
            getPreferenceScreen().removePreference(mNavigation);
        }

        mNavringPreference = (VanirPreferenceSwitch) findPreference(KEY_NAVRING_SWITCH);
        mNavringPreference.setSettingToWatch(Settings.System.ENABLE_NAVIGATION_RING, 1);

        mNavbarPreference = (VanirPreferenceSwitch) findPreference(KEY_NAVBAR_SWITCH);
        if (HardwareKeyNavbarHelper.shouldShowNavbarToggle())
            mNavbarPreference.setSettingToWatch(Settings.System.ENABLE_NAVIGATION_BAR);

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
        setListPreferenceSummary(mImmersiveOrientation, strValueOrientation);
        mImmersiveOrientation.setOnPreferenceChangeListener(this);
    
        mImmersiveModePref = (ListPreference) findPreference(KEY_IMMERSIVE_MODE_STYLE);
        immersiveModeValue = Settings.System.getInt(getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 2);
        updateImmersiveModeDependencies();
        mImmersiveModePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        if (deviceKeys > 0) {
            if (mSettingsObserver == null) {
                mSettingsObserver = new SettingsObserver(new Handler());
                mSettingsObserver.observe();
                mSettingsObserver.onChange(true);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSettingsObserver != null) {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(mSettingsObserver);
            mSettingsObserver = null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mNavringPreference || preference == mNavbarPreference) {
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
            setListPreferenceSummary(mImmersiveModePref, strValue);
            if (deviceKeys > 0) saveImmersiveState(immersiveModeValue);
            updateImmersiveModeDependencies();
            updateRebootDialog();
            return true;

        } else if (preference == mImmersiveOrientation) {
            final String strValue = (String)objValue;
            int value = Integer.valueOf(strValue);
            Settings.System.putInt(getContentResolver(), Settings.System.IMMERSIVE_ORIENTATION,
                    value);
            setListPreferenceSummary(mImmersiveOrientation, strValue);
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

    private void updateImmersiveModeDependencies() {
        boolean mmmBBQChickenSandwich = (immersiveModeValue > 0);
        mExpandedDesktop.setEnabled(mmmBBQChickenSandwich);
        mImmersiveOrientation.setEnabled(mmmBBQChickenSandwich);
        mImmersiveModeState.setEnabled(mmmBBQChickenSandwich);
    }

    public void setHardwareImmersiveState(boolean enabled) {
        final SharedPreferences prefs = mContext.getSharedPreferences(HARDWARE_IMMERSIVE_STYLE, Context.MODE_PRIVATE);
        int previousEnabledValue = prefs.getInt(IMMERSIVE_ENABLED, 2);
        int previousDisabledValue = prefs.getInt(IMMERSIVE_DISABLED, 1);
        if (previousDisabledValue > 1) previousDisabledValue = 1;

        final Resources res = getResources();
        mImmersiveModePref.setEntryValues(res.getStringArray(
                enabled ? R.array.immersive_mode_values : R.array.immersive_mode_values_no_navbar));
        mImmersiveModePref.setEntries(res.getStringArray(
                enabled ? R.array.immersive_mode_entries : R.array.immersive_mode_entries_no_navbar));

        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, enabled ? previousEnabledValue : previousDisabledValue);

        String strValue = String.valueOf(enabled ? previousEnabledValue : previousDisabledValue);
        mImmersiveModePref.setValue(strValue);
        setListPreferenceSummary(mImmersiveModePref, strValue);
    }

    private void saveImmersiveState(int newValue) {
        final SharedPreferences prefs = mContext.getSharedPreferences(HARDWARE_IMMERSIVE_STYLE, Context.MODE_PRIVATE);
        final ContentResolver resolver = mContext.getContentResolver();

        boolean enabled = Settings.System.getInt(resolver,
                Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1;

        if (enabled) {
            prefs.edit().putInt(IMMERSIVE_ENABLED, newValue).commit();
        } else {
            prefs.edit().putInt(IMMERSIVE_DISABLED, newValue).commit();
        }
    }

    private void setListPreferenceSummary(final ListPreference pref, final String value) {
        pref.setSummary(pref.getEntries()[pref.findIndexOfValue(value)]);
    }
}
