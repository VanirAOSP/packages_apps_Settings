/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.vanir.util.CMDProcessor;

import java.io.File;

/**
 * Performance Settings
 */
public class PerformanceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "PerformanceSettings";

    private static final String CATEGORY_PROFILES = "perf_profile_prefs";
    private static final String CATEGORY_SYSTEM = "perf_system_prefs";
    private static final String CATEGORY_GRAPHICS = "perf_graphics_prefs";

    private static final String PERF_PROFILE_PREF = "pref_perf_profile";
    private static final String USE_16BPP_ALPHA_PREF = "pref_use_16bpp_alpha";

    private static final String USE_16BPP_ALPHA_PROP = "persist.sys.use_16bpp_alpha";

    private static final String FORCE_HIGHEND_GFX_PREF = "pref_force_highend_gfx";
    private static final String FORCE_HIGHEND_GFX_PERSIST_PROP = "persist.sys.force_highendgfx";
    public static final String LOG_PREF = "disable_logging_set_on_boot";
    private static final String LOG_PATH = "/sys/module/logger/parameters/enabled";

    private ListPreference mPerfProfilePref;
    private CheckBoxPreference mUse16bppAlphaPref;
    private CheckBoxPreference mForceHighEndGfx;
    private static CheckBoxPreference mSystemLogging;

    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;
    private String mPerfProfileDefaultEntry;

    private ContentObserver mPerformanceProfileObserver = null;

    private PowerManager mPowerManager;

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            setCurrentValue();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mPerfProfileDefaultEntry = getString(
                com.android.internal.R.string.config_perf_profile_default_entry);
        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.performance_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        PreferenceCategory category = (PreferenceCategory) prefSet.findPreference(CATEGORY_PROFILES);

        mPerfProfilePref = (ListPreference)prefSet.findPreference(PERF_PROFILE_PREF);
        if (mPerfProfilePref != null && !mPowerManager.hasPowerProfiles()) {
            prefSet.removePreference(category);
            mPerfProfilePref = null;
        } else {
            mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());

            mPerfProfilePref.setEntries(mPerfProfileEntries);
            mPerfProfilePref.setEntryValues(mPerfProfileValues);
            setCurrentValue();
            mPerfProfilePref.setOnPreferenceChangeListener(this);
        }

        category = (PreferenceCategory) prefSet.findPreference(CATEGORY_GRAPHICS);
        mUse16bppAlphaPref = (CheckBoxPreference) prefSet.findPreference(USE_16BPP_ALPHA_PREF);
        String use16bppAlpha = SystemProperties.get(USE_16BPP_ALPHA_PROP, "0");
        mUse16bppAlphaPref.setChecked("1".equals(use16bppAlpha));

        if (ActivityManager.isLowRamDeviceStatic()) {
            mForceHighEndGfx = (CheckBoxPreference) prefSet.findPreference(FORCE_HIGHEND_GFX_PREF);
            String forceHighendGfx = SystemProperties.get(FORCE_HIGHEND_GFX_PERSIST_PROP, "false");
            mForceHighEndGfx.setChecked("true".equals(forceHighendGfx));
        } else {
            category.removePreference(findPreference(FORCE_HIGHEND_GFX_PREF));
        }

        // this kernel module is required for this feature
        // https://github.com/jimsth/vanir_hammerhead/commit/138cba1c61f364c5c31fd5999e738cbbea03d0d9
        mSystemLogging = (CheckBoxPreference) findPreference(LOG_PREF);
        if (!exists(LOG_PATH)) {
            getPreferenceScreen().removePreference(mSystemLogging);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPerfProfilePref != null) {
            setCurrentValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPerfProfilePref != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mPerformanceProfileObserver);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mUse16bppAlphaPref) {
            SystemProperties.set(USE_16BPP_ALPHA_PROP,
                    mUse16bppAlphaPref.isChecked() ? "1" : "0");
        } else if (preference == mForceHighEndGfx) {
            SystemProperties.set(FORCE_HIGHEND_GFX_PERSIST_PROP,
                    mForceHighEndGfx.isChecked() ? "true" : "false");
        } else if (preference == mSystemLogging) {
            writeSystemLoggingOptions();
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                mPowerManager.setPowerProfile(String.valueOf(newValue));
                setCurrentPerfProfileSummary();
                return true;
            }
        }
        return false;
    }

    private void setCurrentPerfProfileSummary() {
        String value = mPowerManager.getPowerProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].equals(value)) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format("%s", summary));
    }

    private void setCurrentValue() {
        mPerfProfilePref.setValue(mPowerManager.getPowerProfile());
        setCurrentPerfProfileSummary();
    }

    private String getCurrentPerformanceProfile() {
        String value = Settings.System.getString(getActivity().getContentResolver(),
                Settings.Secure.PERFORMANCE_PROFILE);
        if (TextUtils.isEmpty(value)) {
            value = mPerfProfileDefaultEntry;
        }
        return value;
    }

    private void writeSystemLoggingOptions() {

        if (mSystemLogging.isChecked()) {
            new CMDProcessor().su.runWaitFor("busybox echo 0 > /sys/module/logger/parameters/log_enabled");
        } else {
            new CMDProcessor().su.runWaitFor("busybox echo 1 > /sys/module/logger/parameters/log_enabled");
        }
    }

    private static boolean exists(String string) {
        File f = new File(string);
        if (f.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static void updateLogging(Context ctx) {
        if (mSystemLogging == null) return;
        boolean bool = mSystemLogging.isChecked();

        if (!bool) {
            return;
        } else {
            Log.i(TAG, "Setting logging to disabled by user preference");
            new CMDProcessor().su.runWaitFor("busybox echo 0 > /sys/module/logger/parameters/log_enabled");
        }
    }
}
