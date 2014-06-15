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
import android.content.DialogInterface;
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

import java.io.File;

public class AdditionalSystemUI extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AdditionalSystemUI";

    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    private static final String KEY_ANIMATION_OPTIONS = "category_animation_options";
    private static final String KEY_POWER_CRT_MODE = "system_power_crt_mode";
    private static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "vanir_interface_recents_mem_display";
    private static final String KEY_DUAL_PANEL = "force_dualpanel";
    private static final String RECENTS_CLEAR_ALL = "recents_clear_all";
    private static final CharSequence PREF_DISABLE_BOOTANIM = "customize_bootanimation";
    private static final String CUSTOM_RECENT_MODE = "custom_recent_mode";
    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE = "recent_panel_scale";
    private static final String RECENT_PANEL_EXPANDED_MODE = "recent_panel_expanded_mode";

    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    public static Intent INTENT_OMNISWITCH_SETTINGS = new Intent(Intent.ACTION_MAIN)
            .setClassName(OMNISWITCH_PACKAGE_NAME, OMNISWITCH_PACKAGE_NAME + ".SettingsActivity");

    private Preference mCustomLabel;
    private CheckBoxPreference mWakeWhenPluggedOrUnplugged;
    private ListPreference mCrtMode;
    private CheckBoxPreference mMembar;
    private CheckBoxPreference mDualPanel;
    private CheckBoxPreference mSystemLogging;
    private ListPreference mClearAll;
    private ListPreference mBootAnimation;
    private ListPreference mRecentsCustom;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;

    Preference mLcdDensity;
    int newDensityValue;
    DensityChanger densityFragment;
    private boolean mBootAnimationState = true;

    private String mCustomLabelText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_ui_more);
        PreferenceScreen prefSet = getPreferenceScreen();

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);

        // Default value for wake-on-plug behavior from config.xml
        boolean wakeUpWhenPluggedOrUnpluggedConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);
        mWakeWhenPluggedOrUnplugged = (CheckBoxPreference) findPreference(KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED);
        mWakeWhenPluggedOrUnplugged.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                (wakeUpWhenPluggedOrUnpluggedConfig ? 1 : 0)) == 1);

        // respect device default configuration
        // true fades while false animates
        boolean electronBeamFadesConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_animateScreenLights);
        PreferenceCategory animationOptions =
            (PreferenceCategory) prefSet.findPreference(KEY_ANIMATION_OPTIONS);
        mCrtMode = (ListPreference) prefSet.findPreference(KEY_POWER_CRT_MODE);
        if (!electronBeamFadesConfig && mCrtMode != null) {
            int crtMode = Settings.System.getInt(getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE, 1);
            mCrtMode.setValue(String.valueOf(crtMode));
            mCrtMode.setSummary(mCrtMode.getEntry());
            mCrtMode.setOnPreferenceChangeListener(this);
        } else if (animationOptions != null) {
            prefSet.removePreference(animationOptions);
        }

        mClearAll = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL);
        int value = Settings.System.getInt(getContentResolver(),
                Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, 4);
        mClearAll.setValue(String.valueOf(value));
        mClearAll.setSummary(mClearAll.getEntry());
        mClearAll.setOnPreferenceChangeListener(this);

        mMembar = (CheckBoxPreference) prefSet.findPreference(SYSTEMUI_RECENTS_MEM_DISPLAY);
        if (mMembar != null) {
        mMembar.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEMUI_RECENTS_MEM_DISPLAY, 0) == 1);
        }

        mDualPanel = (CheckBoxPreference) findPreference(KEY_DUAL_PANEL);
        mDualPanel.setChecked(Settings.System.getBoolean(getContentResolver(),
                Settings.System.FORCE_DUAL_PANEL, false));

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }
        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        mBootAnimation = (ListPreference) findPreference(PREF_DISABLE_BOOTANIM);
        mBootAnimation.setOnPreferenceChangeListener(this);

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

        updateCustomLabelTextSummary();
        resetBootAnimationSummary();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBootAnimation != null) {
            mBootAnimation.setSummary(mBootAnimation.getEntry());
        }
        updateRecentsDependencies();
    }

    private void updatePreference() {
        boolean customRecent = Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.CUSTOM_RECENTS, false);

        if (customRecent == false) {
            mRecentPanelLeftyMode.setEnabled(false);
            mRecentPanelScale.setEnabled(false);
            mRecentPanelExpandedMode.setEnabled(false);
        } else {
            mRecentPanelLeftyMode.setEnabled(true);
            mRecentPanelScale.setEnabled(true);
            mRecentPanelExpandedMode.setEnabled(true);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        ContentResolver resolver = getActivity().getContentResolver();

        if (KEY_POWER_CRT_MODE.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mCrtMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE,
                    value);
            mCrtMode.setSummary(mCrtMode.getEntries()[index]);
            return true;

        } else if (PREF_DISABLE_BOOTANIM.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mBootAnimation.findIndexOfValue((String) objValue);
            mBootAnimation.setSummary(mBootAnimation.getEntries()[index]);
            postBootAnimationPreference(index);
            return true;

        } else if (RECENTS_CLEAR_ALL.equals(key)) {
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

            if (value == 2 && !isOmniSwitchInstalled()) {
                openOmniSwitchNotInstalledWarning();
                return true;
            }

            Settings.System.putInt(resolver,
                    Settings.System.CUSTOM_RECENTS, value);
            mRecentsCustom.setSummary(mRecentsCustom.getEntries()[index]);
            updateRecentsDependencies();

            if (value == 2) {
                openOmniSwitchEnabledWarning();
            }
            return true;

        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
            updateRecentPanelScaleOptions(objValue);
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
        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mDualPanel) {
            Settings.System.putBoolean(getContentResolver(), Settings.System.FORCE_DUAL_PANEL,
                    mDualPanel.isChecked() ? true : false);
            return true;
        } else if (preference == mCustomLabel) {
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
        } else if (preference == mWakeWhenPluggedOrUnplugged) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                    mWakeWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateRecentsDependencies() {
        int mRecentsStyle = Settings.System.getInt(getActivity().getContentResolver(),
                                Settings.System.CUSTOM_RECENTS, RecentsConstants.RECENTS_AOSP);

        mClearAll.setEnabled(mRecentsStyle == RecentsConstants.RECENTS_AOSP);
        mMembar.setEnabled(mRecentsStyle == RecentsConstants.RECENTS_AOSP);
        mRecentPanelLeftyMode.setEnabled(mRecentsStyle == RecentsConstants.RECENTS_SLIM);
        mRecentPanelScale.setEnabled(mRecentsStyle == RecentsConstants.RECENTS_SLIM);
        mRecentPanelExpandedMode.setEnabled(mRecentsStyle == RecentsConstants.RECENTS_SLIM);
    }

    private void resetBootAnimationSummary() {
        if (!new File("/system/media/bootanimation.backup").exists()) {
           mBootAnimation.setValueIndex(0);
        }
    }

    /**
     * ListPreference index for boot animation preferences
     * 0 = Default
     * 1 = minimal
     * 2 = disabled
    */
    private void postBootAnimationPreference(int index) {
        String backup = "/system/media/bootanimation.backup";
        String defaultLocation = "/system/media/bootanimation.zip";
        String minimalLocation = "/system/media/bootanimation.minimal";
        String chmod = "chmod 644 /system/media/bootanimation.zip";
        String cmd = "";

        // make a backup of the default animation if one doesn't exist.  Do this to make it
        // possible to restore the default animation later when custom animation preference is added
        if (!new File(backup).exists()) {
            String saveDefault = ("cp " + defaultLocation + " " + backup);
            new AbstractAsyncSuCMDProcessor(true) {
                @Override
                protected void onPostExecute(String result) {
                }
            }.execute(saveDefault);
        }

        switch (index) {
            case 0:  // DEFAULT
                enableBootAnimation(true);
                cmd = ("cp " + backup + " " + defaultLocation);
                break;

            case 1:  // MINIMAL
                enableBootAnimation(true);
                cmd = ("mv " + defaultLocation + " " + minimalLocation);
                break;

            case 2:  // DISABLED
                enableBootAnimation(false);
                String cheese = getString(R.string.toast_disabled);
                Toast.makeText(mContext, cheese, Toast.LENGTH_SHORT).show();
                break;
        }

        AbstractAsyncSuCMDProcessor processor = new AbstractAsyncSuCMDProcessor(true) {
            @Override
            protected void onPostExecute(String result) {
            }
        };

        if (index == 0) {
            processor.execute(cmd, chmod);
        } else {
            processor.execute(cmd);
        }
    }

    private void enableBootAnimation(boolean myPreference) {
        if (mBootAnimationState != myPreference) {
            String cmd =  "";
            CommandResult cr = new CMDProcessor().su.runWaitFor(
                    "grep -q \"debug.sf.nobootanimation\" /system/build.prop");

            if (cr.success()) {
                cmd = ("busybox sed -i 's|debug.sf.nobootanimation=.*|"
                + "debug.sf.nobootanimation" + "=" + (myPreference ? "0" : "1") + "|' " + "/system/build.prop");
            } else {
                cmd = ("echo debug.sf.nobootanimation=" + (myPreference ? "0" : "1") + " >> /system/build.prop");
            }

            new AbstractAsyncSuCMDProcessor(true) {
                @Override
                protected void onPostExecute(String result) {
                }
            }.execute(cmd);
            mBootAnimationState = myPreference;
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

    private void updateRecentPanelScaleOptions(Object objValue) {
        int index = mRecentPanelScale.findIndexOfValue((String) objValue);
        int value = Integer.valueOf((String) objValue);
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
        mRecentPanelScale.setSummary(mRecentPanelScale.getEntries()[index]);
    }

     private void openOmniSwitchNotInstalledWarning() {
        new AlertDialog.Builder(getActivity())
        .setTitle(getResources().getString(R.string.omniswitch_warning_title))
        .setMessage(getResources().getString(R.string.omniswitch_not_installed_message))
        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        }).show();
    }

    private void openOmniSwitchEnabledWarning() {
        new AlertDialog.Builder(getActivity())
        .setTitle(getResources().getString(R.string.omniswitch_warning_title))
        .setMessage(getResources().getString(R.string.omniswitch_enabled_message))
        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                startActivity(INTENT_OMNISWITCH_SETTINGS);
            }
        }).show();
    }

    private boolean isOmniSwitchInstalled() {
        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(OMNISWITCH_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
