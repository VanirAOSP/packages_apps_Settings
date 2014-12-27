/*
 * Copyright (C) 2014 Vanir-Exodus
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import org.cyanogenmod.hardware.KeyDisabler;

public class GeneralButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";

    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_BLUETOOTH_INPUT_SETTINGS = "bluetooth_input_settings";
    private static final String BUTTON_HEADSETHOOK_LAUNCH_VOICE = "button_headsethook_launch_voice";
    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";

    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_GENERAL = "category_general";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_POWER_BUTTON = "power_key";
    private static final String CATEGORY_HEADSETHOOK = "button_headsethook";
    private static final String CATEGORY_NAVBAR = "navigation_bar";
    
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private CheckBoxPreference mCameraWake;
    private CheckBoxPreference mCameraSleepOnRelease;
    private CheckBoxPreference mCameraMusicControls;
    private ListPreference mVolumeKeyCursorControl;
    private CheckBoxPreference mHeadsetHookLaunchVoice;
    private SwitchPreference mDisableNavigationKeys;
    private CheckBoxPreference mPowerEndCall;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int deviceWakeKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareWakeKeys);
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;
        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        final boolean hasVolumeKeys = (deviceKeys & KEY_MASK_VOLUME) != 0;
        final boolean showVolumeWake = (deviceWakeKeys & KEY_MASK_VOLUME) != 0;

        boolean hasAnyBindableKey = false;
        final PreferenceCategory generalCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_GENERAL);
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        // Power button ends calls.
        mPowerEndCall = (CheckBoxPreference) findPreference(KEY_POWER_END_CALL);

        mHandler = new Handler();

        // Force Navigation bar related options
        mDisableNavigationKeys = (SwitchPreference) findPreference(DISABLE_NAV_KEYS);

        // Only visible on devices that does not have a navigation bar already,
        // and don't even try unless the existing keys can be disabled
        boolean needsNavigationBar = false;
        if (KeyDisabler.isSupported()) {
            try {
                IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                needsNavigationBar = wm.needsNavigationBar();
            } catch (RemoteException e) {
            }

            if (needsNavigationBar) {
                prefScreen.removePreference(mDisableNavigationKeys);
            } else {
                // Remove keys that can be provided by the navbar
                updateDisableNavkeysOption();
            }
        } else {
            prefScreen.removePreference(mDisableNavigationKeys);
        }

        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
                prefScreen.removePreference(powerCategory);
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

/*        if (hasCameraKey) {
            mCameraWake = (CheckBoxPreference)
                prefScreen.findPreference(Settings.System.CAMERA_WAKE_SCREEN);
            mCameraSleepOnRelease = (CheckBoxPreference)
                prefScreen.findPreference(Settings.System.CAMERA_SLEEP_ON_RELEASE);
            mCameraMusicControls = (CheckBoxPreference)
                prefScreen.findPreference(Settings.System.CAMERA_MUSIC_CONTROLS);
            boolean value = mCameraWake.isChecked();
            mCameraMusicControls.setEnabled(!value);
            mCameraSleepOnRelease.setEnabled(value);
            if (getResources().getBoolean(
                com.android.internal.R.bool.config_singleStageCameraKey)) {
                cameraCategory.removePreference(mCameraSleepOnRelease);
            }
        } else {
            prefScreen.removePreference(cameraCategory);
        }

        if (!hasCameraKey) {
            prefScreen.removePreference(generalCategory);
        }
*/
        if (Utils.hasVolumeRocker(getActivity())) {

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);
        } else {
            prefScreen.removePreference(volumeCategory);
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_BLUETOOTH_INPUT_SETTINGS);

        mHeadsetHookLaunchVoice = (CheckBoxPreference) findPreference(BUTTON_HEADSETHOOK_LAUNCH_VOICE);
        mHeadsetHookLaunchVoice.setChecked(Settings.System.getInt(resolver,
                Settings.System.HEADSETHOOK_LAUNCH_VOICE, 1) == 1);
        updateHeadsetButtonSummary();

        final PreferenceCategory powerButtonCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER_BUTTON);
        final boolean isTorchSupported = isPackageInstalled(getActivity(), "net.cactii.flash2");
        final boolean isCameraPresent = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

        final boolean hasAnyPowerButtonOptions = isTorchSupported  || isCameraPresent /* || etc. */;
        if (!hasAnyPowerButtonOptions) {
            prefScreen.removePreference(powerButtonCategory);
            if (!Utils.isVoiceCapable(getActivity())) {
                powerButtonCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
           }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean checked = false;
        if (preference instanceof CheckBoxPreference) {
            checked = ((CheckBoxPreference)preference).isChecked();
        }

        if (preference == mCameraWake) {
            mCameraMusicControls.setEnabled(!checked);
            mCameraSleepOnRelease.setEnabled(checked);
            return true;

        } else if (preference == mHeadsetHookLaunchVoice) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HEADSETHOOK_LAUNCH_VOICE,
                    mHeadsetHookLaunchVoice.isChecked() ? 1 : 0);
            updateHeadsetButtonSummary();
            return true;

        } else if (preference == mDisableNavigationKeys) {
            writeDisableNavkeysOption(getActivity(), mDisableNavigationKeys.isChecked());
            updateDisableNavkeysOption();
            return true;

        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private static void writeDisableNavkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int defaultBrightness = context.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

        Settings.System.putInt(context.getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
        KeyDisabler.setActive(enabled);

        /* Save/restore button timeouts to disable them in softkey mode */
        Editor editor = prefs.edit();

        if (enabled) {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, defaultBrightness);
            if (!prefs.contains("pre_navbar_button_backlight")) {
                editor.putInt("pre_navbar_button_backlight", currentBrightness);
            }
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, 0);
        } else {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS,
                    prefs.getInt("pre_navbar_button_backlight", defaultBrightness));
            editor.remove("pre_navbar_button_backlight");
        }
        editor.commit();
    }


    private void updateDisableNavkeysOption() {
        boolean enabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) != 0;

        mDisableNavigationKeys.setChecked(enabled);
    }

    public static void restoreKeyDisabler(Context context) {
        if (!KeyDisabler.isSupported()) {
            return;
        }

        writeDisableNavkeysOption(context, Settings.System.getInt(context.getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) != 0);
    }

    private void updateHeadsetButtonSummary() {
        mHeadsetHookLaunchVoice.setSummary(mHeadsetHookLaunchVoice.isChecked() ?
                R.string.button_headsethook_launch_voice_checked_summary :
                R.string.button_headsethook_launch_voice_unchecked_summary);
    }
    
    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }
}
