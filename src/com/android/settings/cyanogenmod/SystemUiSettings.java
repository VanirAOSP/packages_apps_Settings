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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SystemUiSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String CATEGORY_NAVRING = "navigation_bar_ring";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_IMMERSIVE_MODE_STYLE = "immersive_mode_style";
    private static final String KEY_IMMERSIVE_MODE_STATE = "immersive_mode_state";

    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mImmersiveModePref;
    private CheckBoxPreference mImmersiveModeState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        mImmersiveModeState = (CheckBoxPreference) findPreference(KEY_IMMERSIVE_MODE_STATE);
        mImmersiveModeState.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STATE, 0) == 1);
        mImmersiveModeState.setOnPreferenceChangeListener(this);        
        
        mImmersiveModePref = (ListPreference) prefSet.findPreference(KEY_IMMERSIVE_MODE_STYLE);
        mImmersiveModePref.setOnPreferenceChangeListener(this);
        int immersiveModeValue = Settings.System.getInt(getContentResolver(), Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 0);
        mImmersiveModePref.setValue(String.valueOf(immersiveModeValue));

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);

//        try {
//            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

//            lets keep this around so i can modify it to act accordingly with hardware key only devices
//             Hide no-op "Status bar visible" mode on devices without navigation bar
//            if (hasNavBar) {
//                mExpandedDesktopPref.setOnPreferenceChangeListener(this);
//                mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
//                updateExpandedDesktop(expandedDesktopValue);
//                prefScreen.removePreference(mExpandedDesktopNoNavbarPref);
//            } else {
//                mExpandedDesktopNoNavbarPref.setOnPreferenceChangeListener(this);
//                mExpandedDesktopNoNavbarPref.setChecked(expandedDesktopValue > 0);
//                prefScreen.removePreference(mExpandedDesktopPref);
//            }
//

            // Hide navigation bar category on devices without navigation bar
            if (deviceKeys > 0) {
                prefSet.removePreference(findPreference(CATEGORY_NAVBAR));
                prefSet.removePreference(findPreference(CATEGORY_NAVRING));
            }

        updateImmersiveModeState(immersiveModeValue);
        updateImmersiveModeSummary(immersiveModeValue);
    }

    private void updateImmersiveModeState(int value) {
        if (value >=1) {
            mImmersiveModeState.setEnabled(true);
        } else {
            mImmersiveModeState.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mImmersiveModePref) {
            int immersiveModeValue = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, immersiveModeValue);
             updateImmersiveModeSummary(immersiveModeValue);
             updateImmersiveModeState(immersiveModeValue);
             return true;
        } else if (preference == mImmersiveModeState) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STATE,
                    (Boolean) objValue ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateImmersiveModeSummary(int value) {
        Resources res = getResources();
        if (value == 0) {
            /* expanded desktop deactivated */
            mImmersiveModePref.setSummary(res.getString(R.string.immersive_mode_disabled));
        } else if (value == 1) {
            String statusBarPresent = res.getString(R.string.immersive_mode_summary_status_bar);
            mImmersiveModePref.setSummary(res.getString(R.string.summary_immersive_mode, statusBarPresent));
        } else if (value == 2) {
            String statusBarPresent = res.getString(R.string.immersive_mode_summary_no_status_bar);
            mImmersiveModePref.setSummary(res.getString(R.string.summary_immersive_mode, statusBarPresent));
        }
    }  
}
