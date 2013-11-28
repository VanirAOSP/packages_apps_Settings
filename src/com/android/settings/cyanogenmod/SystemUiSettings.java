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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.vanir.DensityChanger;

public class SystemUiSettings extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_EXPANDED_DESKTOP = "expanded_desktop";
    private static final String KEY_EXPANDED_DESKTOP_NO_NAVBAR = "expanded_desktop_no_navbar";
    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_IMMERSIVE_MODE_STYLE = "immersive_mode_style";
    private static final String KEY_IMMERSIVE_MODE_STATE = "immersive_mode_state";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";

    private PreferenceScreen mPieControl;
    private ListPreference mExpandedDesktopPref;
    private CheckBoxPreference mExpandedDesktopNoNavbarPref;
    private ListPreference mImmersiveModePref;
    private CheckBoxPreference mImmersiveModeState;
    private Preference mCustomLabel;

    private String mCustomLabelText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);

        mImmersiveModeState = (CheckBoxPreference) findPreference(KEY_IMMERSIVE_MODE_STATE);
        mImmersiveModeState.setChecked(Settings.System.getInt(getContentResolver(), 
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STATE, 0) == 1);
        mImmersiveModeState.setOnPreferenceChangeListener(this);        
        
        mImmersiveModePref = (ListPreference) prefSet.findPreference(KEY_IMMERSIVE_MODE_STYLE);
        mImmersiveModePref.setOnPreferenceChangeListener(this);
        int immersiveModeValue = Settings.System.getInt(getContentResolver(), Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 0);
        mImmersiveModePref.setValue(String.valueOf(immersiveModeValue));

        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

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
            if (!hasNavBar) {
                prefSet.removePreference(findPreference(CATEGORY_NAVBAR));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }

        updateCustomLabelTextSummary();
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
 
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    mContext.sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
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

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
     }   
}
