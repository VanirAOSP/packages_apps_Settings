/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.performance;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.vanir.util.Helpers;
import com.vanir.util.CMDProcessor;
import com.vanir.util.CMDProcessor.CommandResult;

public class PerformanceSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "PerformanceSettings";

    private static final String VANIR_CONFIG = "vanir_config";

    private static final String USE_DITHERING_PREF = "pref_use_dithering";
    private static final String USE_DITHERING_PERSIST_PROP = "persist.sys.use_dithering";
    private static final String USE_DITHERING_DEFAULT = "1";

    private static final String USE_16BPP_ALPHA_PREF = "pref_use_16bpp_alpha";
    private static final String USE_16BPP_ALPHA_PROP = "persist.sys.use_16bpp_alpha";

    private static final String PURGEABLE_ASSETS_PREF = "pref_purgeable_assets";
    private static final String PURGEABLE_ASSETS_PERSIST_PROP = "persist.sys.purgeable_assets";
    private static final String PURGEABLE_ASSETS_DEFAULT = "1";

    private static final String DISABLE_BOOTANIMATION_PREF = "pref_disable_bootanimation";
    private static final String DISABLE_BOOTANIMATION_PERSIST_PROP = "persist.sys.nobootanimation";
    private static final String DISABLE_BOOTANIMATION_DEFAULT = "0";

    private static final String SYSTEMUI_RECENTS_MEM_DISPLAY = "vanir_interface_recents_mem_display";

    private ListPreference mUseDitheringPref;

    private SwitchPreference mVanirConfig;
    private CheckBoxPreference mUse16bppAlphaPref;
    private CheckBoxPreference mDisableBootanimPref;
    private CheckBoxPreference mPurgeableAssetsPref;
    private CheckBoxPreference mMembar;

    private boolean everenabled;

    private Handler mHandler = new Handler();
    private Runnable enabler = new Runnable() {
        public void run() {
            CommandResult cr = new CMDProcessor().su.runWaitFor("vanir enable");
            if (!cr.success()) {
                Log.e(TAG, "Failed: vanir enable\n\t"+cr.toString());
                mVanirConfig.setChecked(false);
            }
            else {
                Log.i(TAG, "vanir tweak application at boot enabled");
                everenabled = true;
                mVanirConfig.setSummary(getResources().getString(R.string.vanir_config_enabled_summary));
            }
            mVanirConfig.setEnabled(true);
        }
    };
    private Runnable disabler = new Runnable() {
        public void run() {
            CommandResult cr = new CMDProcessor().su.runWaitFor("vanir disable");
            if (!cr.success()) {
                Log.e(TAG, "Failed: vanir disabled\n\t"+cr.toString());
                mVanirConfig.setChecked(true);
            }
            else {
                Log.i(TAG, "vanir tweak application at boot disabled");
                mVanirConfig.setSummary(getResources().getString(R.string.vanir_config_just_disabled_summary));
            }
            mVanirConfig.setEnabled(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        everenabled = savedInstanceState != null && savedInstanceState.containsKey("ever_enabled") && savedInstanceState.getBoolean("ever_enabled");

        if (getPreferenceManager() != null) {

            addPreferencesFromResource(R.xml.performance_settings);

            PreferenceScreen prefSet = getPreferenceScreen();

            mPurgeableAssetsPref = (CheckBoxPreference) prefSet.findPreference(PURGEABLE_ASSETS_PREF);
            String purgeableAssets = SystemProperties.get(PURGEABLE_ASSETS_PERSIST_PROP,
                    PURGEABLE_ASSETS_DEFAULT);
            mPurgeableAssetsPref.setChecked("1".equals(purgeableAssets));

            mVanirConfig = (SwitchPreference) prefSet.findPreference(VANIR_CONFIG);

            CommandResult cr = new CMDProcessor().sh.runWaitFor("vanir status");
            if (!cr.success())
                Log.e(TAG, "Failed: vanir status\n\t"+cr.toString());
            else {
                final boolean enabled = cr.stdout.equals("1");
                mVanirConfig.setSummary(getResources().getString(enabled ? R.string.vanir_config_enabled_summary : (everenabled ? R.string.vanir_config_just_disabled_summary : R.string.vanir_config_disabled_summary)));
                mVanirConfig.setChecked(enabled);
                if (!everenabled) everenabled = enabled;
            }
            mVanirConfig.setOnPreferenceChangeListener(this);

            String useDithering = SystemProperties.get(USE_DITHERING_PERSIST_PROP, USE_DITHERING_DEFAULT);
            mUseDitheringPref = (ListPreference) prefSet.findPreference(USE_DITHERING_PREF);
            mUseDitheringPref.setOnPreferenceChangeListener(this);
            mUseDitheringPref.setValue(useDithering);
            mUseDitheringPref.setSummary(mUseDitheringPref.getEntry());

            mUse16bppAlphaPref = (CheckBoxPreference) prefSet.findPreference(USE_16BPP_ALPHA_PREF);
            String use16bppAlpha = SystemProperties.get(USE_16BPP_ALPHA_PROP, "0");
            mUse16bppAlphaPref.setChecked("1".equals(use16bppAlpha));
            
            mDisableBootanimPref = (CheckBoxPreference) getPreferenceScreen().findPreference(DISABLE_BOOTANIMATION_PREF);

            String disableBootanimation = SystemProperties.get(DISABLE_BOOTANIMATION_PERSIST_PROP,
                                                           DISABLE_BOOTANIMATION_DEFAULT);
            mDisableBootanimPref.setChecked("1".equals(disableBootanimation));
            
            mMembar = (CheckBoxPreference) getPreferenceScreen().findPreference(SYSTEMUI_RECENTS_MEM_DISPLAY);
            if (mMembar != null) {
            mMembar.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_RECENTS_MEM_DISPLAY, 0) == 1);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mUse16bppAlphaPref) {
            SystemProperties.set(USE_16BPP_ALPHA_PROP, mUse16bppAlphaPref.isChecked() ? "1" : "0");
            return true;
        } else if (preference == mPurgeableAssetsPref) {
            SystemProperties.set(PURGEABLE_ASSETS_PERSIST_PROP,
                    mPurgeableAssetsPref.isChecked() ? "1" : "0");
            return true;
        } else if (preference == mDisableBootanimPref) {
            SystemProperties.set(DISABLE_BOOTANIMATION_PERSIST_PROP,
                    mDisableBootanimPref.isChecked() ? "1" : "0");
            return true;
        } else if (preference == mMembar) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_RECENTS_MEM_DISPLAY, checked ? 1 : 0);
                    
            Helpers.restartSystemUI();
            return true;
        }

        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mUseDitheringPref) {
            String newVal = (String) newValue;
            int index = mUseDitheringPref.findIndexOfValue(newVal);
            SystemProperties.set(USE_DITHERING_PERSIST_PROP, newVal);
            mUseDitheringPref.setSummary(mUseDitheringPref.getEntries()[index]);
        } else if (preference == mVanirConfig) {
            mVanirConfig.setEnabled(false);
            mHandler.post(mVanirConfig.isChecked() ? enabler : disabler);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
       super.onSaveInstanceState(savedInstanceState);
       savedInstanceState.putBoolean("ever_enabled", everenabled);
    }

}
