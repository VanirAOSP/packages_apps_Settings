/*
 * Copyright (C) 2013 The CyanogenMod project
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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.hardware.VibratorIntensity;
import com.vanir.util.CMDProcessor;

import org.cyanogenmod.hardware.AdaptiveBacklight;
import org.cyanogenmod.hardware.ColorEnhancement;
import org.cyanogenmod.hardware.DisplayGammaCalibration;
import org.cyanogenmod.hardware.SunlightEnhancement;
import org.cyanogenmod.hardware.TapToWake;

import java.io.File;

public class MoreDeviceSettings extends SettingsPreferenceFragment {
    private static final String TAG = "MoreDeviceSettings";

    private static final String KEY_SENSORS_MOTORS_CATEGORY = "sensors_motors_category";
    private static final String KEY_DISPLAY_CALIBRATION_CATEGORY = "display_calibration_category";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";
    private static final String KEY_ADAPTIVE_BACKLIGHT = "adaptive_backlight";
    private static final String KEY_SUNLIGHT_ENHANCEMENT = "sunlight_enhancement";
    private static final String KEY_COLOR_ENHANCEMENT = "color_enhancement";
    private static final String KEY_SCREEN_COLOR_SETTINGS = "screencolor_settings";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String KEY_BUTTON_NAVIGATION = "buttons_navigation";
    public static final String PREF_FCHARGE = "fast_charge";
    private static final String FCHARGE_PATH = "sys/kernel/fast_charge/force_fast_charge";
    private static final String KEY_ADVANCED_DISPLAY_SETTINGS = "advanced_display_settings";
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";

    private CheckBoxPreference mTapToWake;
    private static CheckBoxPreference mFastCharge;
    private CheckBoxPreference mAdaptiveBacklight;
    private CheckBoxPreference mSunlightEnhancement;
    private CheckBoxPreference mColorEnhancement;

    private Preference mAdvanced;
    private Preference mNavigation;
    private PreferenceScreen mScreenColorSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.more_device_settings);
        ContentResolver resolver = getContentResolver();

        final PreferenceGroup calibrationCategory =
                (PreferenceGroup) findPreference(KEY_DISPLAY_CALIBRATION_CATEGORY);

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        mNavigation = (Preference) findPreference(KEY_BUTTON_NAVIGATION);
        if (deviceKeys == 0 || getResources().getBoolean(R.bool.config_userWantsLegacyFormat)) {
            getPreferenceScreen().removePreference(mNavigation);
        }

        // this fast charge commit is required for this feature
        // https://github.com/jimsth/vanir_hammerhead/commit/44c571eda5afc309d6862ab76a9400518ade3be2
        mFastCharge = (CheckBoxPreference) findPreference(PREF_FCHARGE);
        if (!exists(FCHARGE_PATH)) {
            getPreferenceScreen().removePreference(mFastCharge);
        }

        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        boolean hasvib = VibratorIntensity.isSupported() && vibrator != null && vibrator.hasVibrator();
        if (!hasvib) {
            removePreference(KEY_SENSORS_MOTORS_CATEGORY);
        }

        // this was abandoned by CM.. but i guess we'll keep it anyways even though kernel apps do it too
        mTapToWake = (CheckBoxPreference) findPreference(KEY_TAP_TO_WAKE);
        if (!isTapToWakeSupported()) {
            getPreferenceScreen().removePreference(mTapToWake);
            mTapToWake = null;
        }
        if (isTapToWakeSupported()) {
            getPreferenceScreen().removePreference(findPreference(KEY_PROXIMITY_WAKE));
            Settings.System.putInt(resolver, Settings.System.PROXIMITY_ON_WAKE, 1);
        }

        mAdaptiveBacklight = (CheckBoxPreference) findPreference(KEY_ADAPTIVE_BACKLIGHT);
        mColorEnhancement = (CheckBoxPreference) findPreference(KEY_COLOR_ENHANCEMENT);
        mScreenColorSettings = (PreferenceScreen) findPreference(KEY_SCREEN_COLOR_SETTINGS);
        mAdvanced = getPreferenceScreen().findPreference(KEY_ADVANCED_DISPLAY_SETTINGS);

        boolean colors = DisplayColor.isSupported();
        boolean gamma = DisplayGamma.isSupported();
        boolean WTF = DisplayGammaCalibration.getNumberOfControls() == 0;

        mSunlightEnhancement = (CheckBoxPreference) findPreference(KEY_SUNLIGHT_ENHANCEMENT);
        if (!isSunlightEnhancementSupported()) {
            calibrationCategory.removePreference(mSunlightEnhancement);
            mSunlightEnhancement = null;
        }

        if (!gamma || WTF) {
            calibrationCategory.removePreference(findPreference(KEY_DISPLAY_GAMMA));
        }
        if (!colors) {
            calibrationCategory.removePreference(findPreference(KEY_DISPLAY_COLOR));
        }
        if (!isAdaptiveBacklightSupported()) {
            calibrationCategory.removePreference(mAdaptiveBacklight);
            mAdaptiveBacklight = null;
        }
        if (!isColorEnhancementSupported()) {
            calibrationCategory.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }
        if (!isPostProcessingSupported()) {
            calibrationCategory.removePreference(mScreenColorSettings);
            mScreenColorSettings = null;
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                calibrationCategory, KEY_ADVANCED_DISPLAY_SETTINGS);

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
        getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);

        if ((!gamma || WTF) && !colors
                && !isAdaptiveBacklightSupported()
                && !isColorEnhancementSupported()
                && !isPostProcessingSupported()
                && !!isSunlightEnhancementSupported()) {
            getPreferenceScreen().removePreference(calibrationCategory);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTapToWake != null) {
            mTapToWake.setChecked(TapToWake.isEnabled());
        }
        if (mAdaptiveBacklight != null) {
            mAdaptiveBacklight.setChecked(AdaptiveBacklight.isEnabled());
        }
        if (mColorEnhancement != null) {
            mColorEnhancement.setChecked(ColorEnhancement.isEnabled());
        }
        if (mSunlightEnhancement != null) {
            if (SunlightEnhancement.isAdaptiveBacklightRequired() &&
                    !AdaptiveBacklight.isEnabled()) {
                mSunlightEnhancement.setEnabled(false);
            } else {
                mSunlightEnhancement.setChecked(SunlightEnhancement.isEnabled());
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAdaptiveBacklight) {
            if (mSunlightEnhancement != null &&
                    SunlightEnhancement.isAdaptiveBacklightRequired()) {
                mSunlightEnhancement.setEnabled(mAdaptiveBacklight.isChecked());
            }
            return AdaptiveBacklight.setEnabled(mAdaptiveBacklight.isChecked());

        } else if (preference == mSunlightEnhancement) {
            return SunlightEnhancement.setEnabled(mSunlightEnhancement.isChecked());

        } else if (preference == mColorEnhancement) {
            return ColorEnhancement.setEnabled(mColorEnhancement.isChecked());

        } else if (preference == mTapToWake) {
            return TapToWake.setEnabled(mTapToWake.isChecked());

        } else if (preference == mFastCharge) {
            writeFastChargeOption();

        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    private static boolean isTapToWakeSupported() {
        try {
            return TapToWake.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private void writeFastChargeOption() {

        if (mFastCharge.isChecked()) {
            new CMDProcessor().su.runWaitFor("busybox echo 0 > /sys/kernel/fast_charge/force_cast_charge");
        } else {
            new CMDProcessor().su.runWaitFor("busybox echo 1 > /sys/kernel/fast_charge/force_cast_charge");
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

    /**
     * Restore the properties associated with this preference on boot
     * @param ctx A valid context
     */
    public static void restore(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (isTapToWakeSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_TAP_TO_WAKE, true);
            if (!TapToWake.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore tap-to-wake settings.");
            } else {
                Log.d(TAG, "Tap-to-wake settings restored.");
            }
        }

        if (isColorEnhancementSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_COLOR_ENHANCEMENT, true);
            if (!ColorEnhancement.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore color enhancement settings.");
            } else {
                Log.d(TAG, "Color enhancement settings restored.");
            }
        }

        if (isAdaptiveBacklightSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_ADAPTIVE_BACKLIGHT, true);
            if (!AdaptiveBacklight.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore adaptive backlight settings.");
            } else {
                Log.d(TAG, "Adaptive backlight settings restored.");
            }
        }

        if (isSunlightEnhancementSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_SUNLIGHT_ENHANCEMENT, true);
            if (SunlightEnhancement.isAdaptiveBacklightRequired() &&
                    !AdaptiveBacklight.isEnabled()) {
                SunlightEnhancement.setEnabled(false);
                Log.d(TAG, "SRE requires CABC, disabled");
            } else {
                if (!SunlightEnhancement.setEnabled(enabled)) {
                    Log.e(TAG, "Failed to restore SRE settings.");
                } else {
                    Log.d(TAG, "SRE settings restored.");
                }
            }
        }
    }
    
    private boolean isPostProcessingSupported() {
        boolean ret = true;
        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.qualcomm.display", PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            ret = false;
        }
        return ret;
    }

    private static boolean isAdaptiveBacklightSupported() {
        try {
            return AdaptiveBacklight.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isSunlightEnhancementSupported() {
        try {
            return SunlightEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isColorEnhancementSupported() {
        try {
            return ColorEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }
}
