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

import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.cyanogenmod.SystemSettingSwitchPreference;

public class NotificationDrawer extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "NotificationDrawer";

    private static final String UI_COLLAPSE_BEHAVIOUR = "notification_drawer_collapse_on_dismiss";
    private static final String KEY_HOVER_SWITCH = "hover_switch";
    private static final String KEY_HALO = "halo_settings";

    private ListPreference mCollapseOnDismiss;
    private SwitchPreference mHover;
    private SystemSettingSwitchPreference mSwitchPreference;
    private Preference mHalo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notification_drawer);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mHover = (SwitchPreference) findPreference(KEY_HOVER_SWITCH);
        mHover.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.HOVER_ENABLED, 0) == 1);
        mHover.setOnPreferenceChangeListener(this);
        
        mSwitchPreference = (SystemSettingSwitchPreference)
                findPreference(Settings.System.HEADS_UP_NOTIFICATION);


        mHalo = (PreferenceScreen) findPreference(KEY_HALO);

        // Notification drawer
        int collapseBehaviour = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS,
                Settings.System.STATUS_BAR_COLLAPSE_IF_NO_CLEARABLE);
        mCollapseOnDismiss = (ListPreference) findPreference(UI_COLLAPSE_BEHAVIOUR);
        mCollapseOnDismiss.setValue(String.valueOf(collapseBehaviour));
        mCollapseOnDismiss.setOnPreferenceChangeListener(this);
        updateCollapseBehaviourSummary(collapseBehaviour);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean headsUpEnabled = Settings.System.getIntForUser(
                getActivity().getContentResolver(),
                Settings.System.HEADS_UP_NOTIFICATION, 0, UserHandle.USER_CURRENT) == 1;
        mSwitchPreference.setChecked(headsUpEnabled);
        updateHaloPreference();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mCollapseOnDismiss) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_COLLAPSE_ON_DISMISS, value);
            updateCollapseBehaviourSummary(value);
            return true;
        } else if (preference == mHover) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HOVER_ENABLED, (Boolean) objValue ? 1 : 0);
            updateHaloPreference();
            return true;
        }
        return false;
    }

    private void updateHaloPreference() {
        boolean value = Settings.System.getInt(getContentResolver(), 
                    Settings.System.HOVER_ENABLED, 0) == 1;
        if (value) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HALO_ENABLED, 0);
            mHalo.setEnabled(false);
        } else {
            mHalo.setEnabled(true);
        }
    }

    private void updateCollapseBehaviourSummary(int setting) {
        String[] summaries = getResources().getStringArray(
                R.array.notification_drawer_collapse_on_dismiss_summaries);
        mCollapseOnDismiss.setSummary(summaries[setting]);
    }
}
