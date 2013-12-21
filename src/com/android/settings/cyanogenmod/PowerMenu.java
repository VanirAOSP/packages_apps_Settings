/*
 * Copyright (C) 2012 CyanogenMod
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

import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PowerMenu extends SettingsPreferenceFragment {
    private static final String TAG = "PowerMenu";

    private static final String KEY_REBOOT = "power_menu_reboot";
    private static final String KEY_SCREENSHOT = "power_menu_screenshot";
    private static final String KEY_AIRPLANE = "power_menu_airplane";
    private static final String KEY_SILENT = "power_menu_silent";
    private static final String KEY_PROFILES = "power_menu_profiles";
    private static final String KEY_USERS = "power_menu_user";
    private static final String KEY_POWERMENU_IMMERSIVE_PREFS = "powermenu_immersive_prefs";
    private static final String POWER_MENU_SCREENRECORD = "power_menu_screenrecord";

    private CheckBoxPreference mRebootPref;
    private CheckBoxPreference mScreenshotPref;
    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mSilentPref;
    private CheckBoxPreference mProfile;
    private CheckBoxPreference mImmersiveModePref;
    private CheckBoxPreference mUsers;
    private CheckBoxPreference mScreenrecordPowerMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);

        boolean mHasScreenRecord = getActivity().getResources().getBoolean(
                com.android.internal.R.bool.config_enableScreenrecordChord);

        // disable immersive if immersive style is disabled
        mImmersiveModePref.setEnabled(
                Settings.System.getInt(getContentResolver(),
                Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 0)
                != 0);

        mRebootPref = (CheckBoxPreference) findPreference(KEY_REBOOT);
        mRebootPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_REBOOT_ENABLED, 1) == 1));

        mScreenshotPref = (CheckBoxPreference) findPreference(KEY_SCREENSHOT);
        mScreenshotPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_SCREENSHOT_ENABLED, 0) == 1));

        mAirplanePref = (CheckBoxPreference) findPreference(KEY_AIRPLANE);
        mAirplanePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_AIRPLANE_ENABLED, 1) == 1));

        mSilentPref = (CheckBoxPreference) findPreference(KEY_SILENT);
        mSilentPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_SILENT_ENABLED, 1) == 1));

        mImmersiveModePref = (CheckBoxPreference) findPreference(KEY_POWERMENU_IMMERSIVE_PREFS);
        mImmersiveModePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_IMMERSIVE, 1) == 1));

        mProfile = (CheckBoxPreference) findPreference(KEY_PROFILES);
        mProfile.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_PROFILES_ENABLED, 1) == 1));

        // Only enable profiles item if System Profiles are also enabled
        findPreference(KEY_PROFILES).setEnabled(Settings.System.getInt(getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) != 0);

        mUsers = (CheckBoxPreference) findPreference(KEY_USERS);
        mUsers.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_USER_ENABLED, 1) == 1));

        mScreenrecordPowerMenu = (CheckBoxPreference) findPreference(POWER_MENU_SCREENRECORD);
        if(mHasScreenRecord) {
			      mScreenrecordPowerMenu.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREENRECORD_IN_POWER_MENU, 0) == 1);
        } else {
      			getPreferenceScreen().removePreference(mScreenrecordPowerMenu);
        }

        if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
            getPreferenceScreen().removePreference(
                    findPreference("power_menu_user"));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mScreenshotPref) {
            value = mScreenshotPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_SCREENSHOT_ENABLED,
                    value ? 1 : 0);
        } else if (preference == mRebootPref) {
            value = mRebootPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_REBOOT_ENABLED,
                    value ? 1 : 0);
       } else if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_AIRPLANE_ENABLED,
                    value ? 1 : 0);
       } else if (preference == mSilentPref) {
            value = mSilentPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_SILENT_ENABLED,
                    value ? 1 : 0);
        } else if (preference == mImmersiveModePref) {
            value = mImmersiveModePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_IMMERSIVE,
                    value ? 1 : 0);
        } else if (preference == mUsers) {
            value = mUsers.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_USER_ENABLED,
                    value ? 1 : 0);
        } else if (preference == mProfile) {
            value = mProfile.isChecked();
            Settings.System.putInt(getContentResolver(),
                     Settings.System.POWER_MENU_PROFILES_ENABLED,
                     value ? 1 : 0);
        } else if (preference == mScreenrecordPowerMenu) {
			      value = mScreenrecordPowerMenu.isChecked();
			      Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREENRECORD_IN_POWER_MENU,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
