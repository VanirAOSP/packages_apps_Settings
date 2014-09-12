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

import java.io.File;

public class AdditionalSystemUI extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "AdditionalSystemUI";

    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    private static final String KEY_ANIMATION_OPTIONS = "category_animation_options";
    private static final String KEY_POWER_CRT_MODE = "system_power_crt_mode";
    private static final String KEY_DUAL_PANEL = "force_dualpanel";
    private static final String PREF_DISABLE_BOOTANIM = "disable_bootanimation";

    private Preference mCustomLabel;
    private CheckBoxPreference mWakeWhenPluggedOrUnplugged;
    private ListPreference mCrtMode;
    private CheckBoxPreference mBootAnimation;
    private CheckBoxPreference mDualPanel;
    private CheckBoxPreference mSystemLogging;

    Preference mLcdDensity;
    int newDensityValue;
    DensityChanger densityFragment;

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

        mBootAnimation = (CheckBoxPreference) findPreference(PREF_DISABLE_BOOTANIM);
        updateBootAnimationSummary(true);

        updateCustomLabelTextSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (KEY_POWER_CRT_MODE.equals(preference.getKey())) {
            int value = Integer.parseInt((String) objValue);
            int index = mCrtMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE,
                    value);
            mCrtMode.setSummary(mCrtMode.getEntries()[index]);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mBootAnimation) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            postBootAnimationPreference(checked);
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

    private void postBootAnimationPreference(boolean isChecked) {
        if (isChecked) {
            enableBootAnimation(true);
        } else {
            enableBootAnimation(false);
            String cheese = getString(R.string.toast_disabled);
            Toast.makeText(mContext, cheese, Toast.LENGTH_SHORT).show();
        }
    }

    private void enableBootAnimation(boolean myPreference) {
        String cmd =  "";
        CommandResult cr = new CMDProcessor().su.runWaitFor(
                "grep -q \"debug.sf.nobootanimation\" /system/build.prop");

        if (myPreference) {
            if (cr.success()) {
                cmd = ("busybox sed -i 's|debug.sf.nobootanimation=.*||' /system/build.prop");
            }
        } else {
            if (!cr.success()) {
                cmd = ("echo debug.sf.nobootanimation=1 >> /system/build.prop");
            }
        }

        new AbstractAsyncSuCMDProcessor(true) {
            @Override
            protected void onPostExecute(String result) {
                updateBootAnimationSummary(false);
            }
        }.execute(cmd);
    }

    protected void updateBootAnimationSummary(boolean init) {
        boolean enabled;
        if (init) {
            CommandResult cr = new CMDProcessor().su.runWaitFor(
                    "grep -q \"debug.sf.nobootanimation\" /system/build.prop");
            enabled = !cr.success();
            mBootAnimation.setChecked(enabled);
        } else {
            enabled = mBootAnimation.isChecked();
        }

        mBootAnimation.setSummary(enabled
                ? getResources().getString(R.string.disable_bootanimation_default)
                : getResources().getString(R.string.disable_bootanimation_off));
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
