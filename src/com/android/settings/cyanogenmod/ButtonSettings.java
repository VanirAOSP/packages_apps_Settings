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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.util.HardwareKeyNavbarHelper;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_BLUETOOTH_INPUT_SETTINGS = "bluetooth_input_settings";
    private static final String QUICK_CAM = "quick_cam";
    private static final String BUTTON_HEADSETHOOK_LAUNCH_VOICE = "button_headsethook_launch_voice";

    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String CATEGORY_GENERAL = "category_general";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String CATEGORY_HOME = "home_key_button";
    private static final String CATEGORY_POWER_BUTTON = "power_key";
    private static final String CATEGORY_HEADSETHOOK = "button_headsethook";
    
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_CAMERA = 0x20;

    private CheckBoxPreference mCameraWake;
    private CheckBoxPreference mCameraSleepOnRelease;
    private CheckBoxPreference mCameraMusicControls;
    private ListPreference mVolumeKeyCursorControl;
    private CheckBoxPreference mSwapVolumeButtons;
    private CheckBoxPreference mQuickCam;
    private CheckBoxPreference mHeadsetHookLaunchVoice;
    private CheckBoxPreference mPowerEndCall;
    private CheckBoxPreference mHomeAnswerCall;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;

        boolean hasAnyBindableKey = false;
        final PreferenceCategory generalCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_GENERAL);
        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        // Power button ends calls.
        mPowerEndCall = (CheckBoxPreference) findPreference(KEY_POWER_END_CALL);

        // Home button answers calls.
        mHomeAnswerCall = (CheckBoxPreference) findPreference(KEY_HOME_ANSWER_CALL);

        mHandler = new Handler();

        if (hasCameraKey) {
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

        if (Utils.hasVolumeRocker(getActivity())) {
            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (CheckBoxPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            if (!res.getBoolean(R.bool.config_show_volumeRockerWake)) {
                volumeCategory.removePreference(findPreference(Settings.System.VOLUME_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(volumeCategory);
        }

        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) findPreference(KEY_BUTTON_BACKLIGHT);
        if (!backlight.isButtonSupported() && !backlight.isKeyboardSupported()) {
            prefScreen.removePreference(backlight);
        }

        mQuickCam = (CheckBoxPreference) findPreference(QUICK_CAM);
        mQuickCam.setChecked(Settings.System.getInt(resolver,
                Settings.System.POWER_MENU_QUICKCAM, 0) == 1);

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_BLUETOOTH_INPUT_SETTINGS);

        mHeadsetHookLaunchVoice = (CheckBoxPreference) findPreference(BUTTON_HEADSETHOOK_LAUNCH_VOICE);
        mHeadsetHookLaunchVoice.setChecked(Settings.System.getInt(resolver,
                Settings.System.HEADSETHOOK_LAUNCH_VOICE, 1) == 1);
        updateHeadsetButtonSummary();

        final PreferenceCategory powerButtonCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER_BUTTON);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final boolean isTorchSupported = isPackageInstalled(getActivity(), "net.cactii.flash2");
        final boolean isCameraPresent = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

        if (!isTorchSupported) {
            powerButtonCategory.removePreference(findPreference(Settings.System.ENABLE_FAST_TORCH));
        }
        if (!isCameraPresent) {
            powerButtonCategory.removePreference(mQuickCam);
        }
        final boolean hasAnyPowerButtonOptions = isTorchSupported  || isCameraPresent /* || etc. */;
        if (!hasAnyPowerButtonOptions) {
            prefScreen.removePreference(powerButtonCategory);
            if (!Utils.isVoiceCapable(getActivity())) {
                powerButtonCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
           }
        }

        if (!hasHomeKey || !Utils.isVoiceCapable(getActivity())) {
            if (!res.getBoolean(R.bool.config_show_homeWake)) {
                prefScreen.removePreference(homeCategory);
                mHomeAnswerCall = null;
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

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
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

        if (preference == mSwapVolumeButtons) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                    checked ? (Utils.isTablet(getActivity()) ? 2 : 1) : 0);
            return true;

        } else if (preference == mCameraWake) {
            mCameraMusicControls.setEnabled(!checked);
            mCameraSleepOnRelease.setEnabled(checked);
            return true;

        } else if (preference == mQuickCam) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_MENU_QUICKCAM,
                    checked ? 1 : 0 );
            // update reboot dialog
            Intent u = new Intent();
            u.setAction("com.android.powermenu.ACTION_UPDATE_REBOOT_DIALOG");
            mContext.sendBroadcastAsUser(u, UserHandle.ALL);
            return true;

        } else if (preference == mHeadsetHookLaunchVoice) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HEADSETHOOK_LAUNCH_VOICE,
                    mHeadsetHookLaunchVoice.isChecked() ? 1 : 0);
            updateHeadsetButtonSummary();
            return true;

        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
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

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }
}
