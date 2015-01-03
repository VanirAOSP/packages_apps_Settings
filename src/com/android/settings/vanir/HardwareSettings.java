/*
 * Copyright (C) 2014 Exodus
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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

import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.hardware.VibratorIntensity;
import com.vanir.util.CMDProcessor;

import org.cyanogenmod.hardware.TapToWake;

import java.io.File;

public class HardwareSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Hardware.Settings";

    private static final String KEY_SENSORS_MOTORS_CATEGORY = "sensors_motors_category";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    private static final String KEY_BUTTON_NAVIGATION = "buttons_navigation";
    public static final String PREF_FCHARGE = "fast_charge";
    private static final String FCHARGE_PATH = "sys/kernel/fast_charge/force_fast_charge";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;

    private CheckBoxPreference mTapToWake;
    private CheckBoxPreference mProxWake;
    private CheckBoxPreference mLiftToWakePreference;
    private CheckBoxPreference mWakeWhenPluggedOrUnplugged;
    private static CheckBoxPreference mFastCharge;

    private Preference mButtonNavigation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.hardware_settings);
        ContentResolver resolver = getContentResolver();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        mButtonNavigation = (Preference) findPreference(KEY_BUTTON_NAVIGATION);
        if (!hasHomeKey && !hasBackKey && !hasMenuKey && !hasAssistKey && !hasAppSwitchKey) {
            getPreferenceScreen().removePreference(mButtonNavigation);
        }

        // this fast charge commit is required for this feature
        // https://github.com/jimsth/vanir_hammerhead/commit/44c571eda5afc309d6862ab76a9400518ade3be2
        mFastCharge = (CheckBoxPreference) findPreference(PREF_FCHARGE);
        if (!exists(FCHARGE_PATH)) {
            getPreferenceScreen().removePreference(mFastCharge);
        }

        if (isLiftToWakeAvailable(getActivity())) {
            mLiftToWakePreference = (CheckBoxPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        boolean hasvib = VibratorIntensity.isSupported() && vibrator != null && vibrator.hasVibrator();
        if (!hasvib) {
            removePreference(KEY_SENSORS_MOTORS_CATEGORY);
        }

        boolean proximityCheckOnWait = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        mProxWake = (CheckBoxPreference) findPreference(KEY_PROXIMITY_WAKE);
        if (!proximityCheckOnWait) {
            getPreferenceScreen().removePreference(mProxWake);
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 1);
        }

        mWakeWhenPluggedOrUnplugged =
                (CheckBoxPreference) findPreference(KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED);

        // this was abandoned by CM.. but i guess we'll keep it anyways even though kernel apps do it too
        mTapToWake = (CheckBoxPreference) findPreference(KEY_TAP_TO_WAKE);
        if (!isTapToWakeSupported()) {
            getPreferenceScreen().removePreference(mTapToWake);
            mTapToWake = null;
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
        getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Default value for wake-on-plug behavior from config.xml
        boolean wakeUpWhenPluggedOrUnpluggedConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);

        mWakeWhenPluggedOrUnplugged.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                (wakeUpWhenPluggedOrUnpluggedConfig ? 1 : 0)) == 1);

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        if (mTapToWake != null) {
            mTapToWake.setChecked(TapToWake.isEnabled());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mTapToWake) {
            return TapToWake.setEnabled(mTapToWake.isChecked());

        } else if (preference == mFastCharge) {
            writeFastChargeOption();
            return true;

        } else if (preference == mProxWake) {
			Settings.System.putInt(getContentResolver(),
			        Settings.System.PROXIMITY_ON_WAKE,
			        mProxWake.isChecked() ? 1 : 0);
			return true;

        } else if (preference == mWakeWhenPluggedOrUnplugged) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                    mWakeWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        return true;
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
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
    }
}
