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
    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";

    private static final String KEY_HOME_SWITCH = "home_switch";
    private static final String KEY_HOME_SHORT_PRESS = "hardware_keys_home_press";
    private static final String KEY_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP = "hardware_keys_home_double_tap";
    private static final String KEY_BACK_SWITCH = "back_switch";
    private static final String KEY_BACK_PRESS = "hardware_keys_back_press";
    private static final String KEY_BACK_LONG_PRESS = "hardware_keys_back_long_press";
    private static final String KEY_MENU_SWITCH = "menu_switch";
    private static final String KEY_MENU_PRESS = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String KEY_ASSIST_SWITCH = "assist_switch";
    private static final String KEY_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String KEY_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String KEY_APP_SWITCH_SWITCH = "app_switch_switch";
    private static final String KEY_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String KEY_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";

    private static final String CATEGORY_GENERAL = "category_general";
    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";
    private static final String CATEGORY_POWER_BUTTON = "power_key";
    private static final String CATEGORY_HEADSETHOOK = "button_headsethook";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;
    private static final int ACTION_LAUNCH_CAMERA = 6;
    private static final int ACTION_KILL_TARGET = 7;
    private static final int ACTION_IME = 8;
    private static final int ACTION_POWERMENU = 9;
    private static final int ACTION_FIST_YOURMOM = 10;
    private static final int ACTION_HOME = 11;
    private static final int ACTION_BACK = 12;
    private static final int ACTION_LASTAPP = 13;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;

    private ListPreference mHomePressAction;
    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private CheckBoxPreference mHomeSwitch;
    private ListPreference mBackPressAction;
    private ListPreference mBackLongPressAction;
    private CheckBoxPreference mBackSwitch;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private CheckBoxPreference mMenuSwitch;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private CheckBoxPreference mAssistSwitch;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private CheckBoxPreference mAppSwitchSwitch; // lulz
    private CheckBoxPreference mCameraWake;
    private CheckBoxPreference mCameraSleepOnRelease;
    private CheckBoxPreference mCameraMusicControls;
    private ListPreference mVolumeKeyCursorControl;
    private CheckBoxPreference mSwapVolumeButtons;
    private CheckBoxPreference mQuickCam;
    private CheckBoxPreference mHeadsetHookLaunchVoice;
    private CheckBoxPreference mDisableNavigationKeys;
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

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;

        boolean hasAnyBindableKey = false;
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory generalCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_GENERAL);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final PreferenceCategory cameraCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        // Power button ends calls.
        mPowerEndCall = (CheckBoxPreference) findPreference(KEY_POWER_END_CALL);

        // Home button answers calls.
        mHomeAnswerCall = (CheckBoxPreference) findPreference(KEY_HOME_ANSWER_CALL);

        mHandler = new Handler();

        // Force Navigation bar related options
        mDisableNavigationKeys = (CheckBoxPreference) findPreference(DISABLE_NAV_KEYS);

        // Only visible on devices that does not have a navigation bar already,
        // and don't even try unless the existing keys can be disabled
        if (HardwareKeyNavbarHelper.shouldShowHardwareNavkeyToggle(getActivity())) {
            // Remove keys that can be provided by the navbar
            updateDisableNavkeysOption(true);
        } else {
            prefScreen.removePreference(mDisableNavigationKeys);
        }

        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

        if (hasHomeKey) {
            if (!res.getBoolean(R.bool.config_show_homeWake)) {
                homeCategory.removePreference(findPreference(Settings.System.HOME_WAKE_SCREEN));
            }

            mHomeSwitch = (CheckBoxPreference) homeCategory.findPreference(KEY_HOME_SWITCH);
            mHomeSwitch.setChecked(Settings.System.getInt(resolver, 
                    Settings.System.KEY_HOME_ENABLED, 1) == 1);

            int defaultShortPressAction = ACTION_HOME;
            if (!Utils.isVoiceCapable(getActivity())) {
                homeCategory.removePreference(mHomeAnswerCall);
                mHomeAnswerCall = null;
            }


            int defaultLongPressAction = res.getInteger(
                    com.android.internal.R.integer.config_longPressOnHomeBehavior);
            if (defaultLongPressAction < ACTION_NOTHING ||
                    defaultLongPressAction > ACTION_IN_APP_SEARCH) {
                defaultLongPressAction = ACTION_NOTHING;
            }

            int defaultDoubleTapAction = res.getInteger(
                    com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
            if (defaultDoubleTapAction < ACTION_NOTHING ||
                    defaultDoubleTapAction > ACTION_IN_APP_SEARCH) {
                defaultDoubleTapAction = ACTION_NOTHING;
            }

            int shortPressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_HOME_ACTION, defaultShortPressAction);
            mHomePressAction = initActionList(KEY_HOME_SHORT_PRESS, shortPressAction);

            int longPressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                    defaultLongPressAction);
            mHomeLongPressAction = initActionList(KEY_HOME_LONG_PRESS, longPressAction);

            int doubleTapAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                    defaultDoubleTapAction);
            mHomeDoubleTapAction = initActionList(KEY_HOME_DOUBLE_TAP, doubleTapAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(homeCategory);
        }

        if (hasBackKey) {

            mBackSwitch = (CheckBoxPreference) backCategory.findPreference(KEY_BACK_SWITCH);
            mBackSwitch.setChecked(Settings.System.getInt(resolver, 
                    Settings.System.KEY_BACK_ENABLED, 1) == 1);
            
            int pressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_BACK_ACTION, ACTION_BACK);
            mBackPressAction = initActionList(KEY_BACK_PRESS, pressAction);

            int longPressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION, ACTION_NOTHING);
            mBackLongPressAction = initActionList(KEY_BACK_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(backCategory);
        }

        if (hasMenuKey) {

            mMenuSwitch = (CheckBoxPreference) menuCategory.findPreference(KEY_MENU_SWITCH);
            mMenuSwitch.setChecked(Settings.System.getInt(resolver, 
                    Settings.System.KEY_MENU_ENABLED, 1) == 1);
            
            int pressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_MENU_ACTION, ACTION_MENU);
            mMenuPressAction = initActionList(KEY_MENU_PRESS, pressAction);

            int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                        hasAssistKey ? ACTION_SEARCH : ACTION_NOTHING);
            mMenuLongPressAction = initActionList(KEY_MENU_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(menuCategory);
        }

        if (hasAssistKey) {

            mAssistSwitch = (CheckBoxPreference) assistCategory.findPreference(KEY_ASSIST_SWITCH);
            mAssistSwitch.setChecked(Settings.System.getInt(resolver, 
                    Settings.System.KEY_ASSIST_ENABLED, 1) == 1);
            
            int pressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_ASSIST_ACTION, ACTION_SEARCH);
            mAssistPressAction = initActionList(KEY_ASSIST_PRESS, pressAction);

            int longPressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, ACTION_VOICE_SEARCH);
            mAssistLongPressAction = initActionList(KEY_ASSIST_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(assistCategory);
        }

        if (hasAppSwitchKey) {

            mAppSwitchSwitch = (CheckBoxPreference) appSwitchCategory.findPreference(KEY_APP_SWITCH_SWITCH);
            mAppSwitchSwitch.setChecked(Settings.System.getInt(resolver, 
                    Settings.System.KEY_APPSWITCH_ENABLED, 1) == 1);
            
            int pressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_APP_SWITCH_ACTION, ACTION_APP_SWITCH);
            mAppSwitchPressAction = initActionList(KEY_APP_SWITCH_PRESS, pressAction);

            int longPressAction = Settings.System.getInt(resolver,
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, ACTION_NOTHING);
            mAppSwitchLongPressAction = initActionList(KEY_APP_SWITCH_LONG_PRESS, longPressAction);

            hasAnyBindableKey = true;
        } else {
            prefScreen.removePreference(appSwitchCategory);
        }

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

        if (!hasCameraKey && !hasAppSwitchKey && !hasAssistKey && !hasMenuKey && !hasBackKey && !hasHomeKey) {
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
        if (preference == mHomePressAction) {
            handleActionListChange(mHomePressAction, newValue,
                    Settings.System.KEY_HOME_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mHomeLongPressAction) {
            handleActionListChange(mHomeLongPressAction, newValue,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mHomeDoubleTapAction) {
            handleActionListChange(mHomeDoubleTapAction, newValue,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mBackPressAction) {
            handleActionListChange(mBackPressAction, newValue,
                    Settings.System.KEY_BACK_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mBackLongPressAction) {
            handleActionListChange(mBackLongPressAction, newValue,
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mMenuPressAction) {
            handleActionListChange(mMenuPressAction, newValue,
                    Settings.System.KEY_MENU_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mMenuLongPressAction) {
            handleActionListChange(mMenuLongPressAction, newValue,
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mAssistPressAction) {
            handleActionListChange(mAssistPressAction, newValue,
                    Settings.System.KEY_ASSIST_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mAssistLongPressAction) {
            handleActionListChange(mAssistLongPressAction, newValue,
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mAppSwitchPressAction) {
            handleActionListChange(mAppSwitchPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mAppSwitchLongPressAction) {
            handleActionListChange(mAppSwitchLongPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        }

        return false;
    }

    private void sendUpdateBroadcast() {
        Intent u = new Intent();
        u.setAction(Intent.ACTION_UPDATE_KEYS);
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }

    private void updateDisableNavkeysOption(boolean shouldSetChecked) {

        boolean enabled = HardwareKeyNavbarHelper.getDisableHardwareNavkeysOption(getActivity());

        if (shouldSetChecked) {
            mDisableNavigationKeys.setChecked(enabled);
        }

        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Disable hw-key options if they're disabled */
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        /* Toggle backlight control depending on navbar state, force it to
           off if enabling */
        if (backlight != null) {
            backlight.setEnabled(!enabled);
        }

        /* Toggle hardkey control availability depending on navbar state */
        if (homeCategory != null) {
            homeCategory.setEnabled(!enabled);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!enabled);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!enabled);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!enabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean checked = false;
        if (preference instanceof CheckBoxPreference) {
            checked = ((CheckBoxPreference)preference).isChecked();
        }

        
        if (preference == mHomeSwitch) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEY_HOME_ENABLED,
                    checked ? 1 : 0);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mBackSwitch) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEY_BACK_ENABLED,
                    checked ? 1 : 0);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mMenuSwitch) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEY_MENU_ENABLED,
                    checked ? 1 : 0);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mAssistSwitch) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEY_ASSIST_ENABLED,
                    checked ? 1 : 0);
            sendUpdateBroadcast();
            return true;

        } else if (preference == mAppSwitchSwitch) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEY_APPSWITCH_ENABLED,
                    checked ? 1 : 0);
            sendUpdateBroadcast();
            return true;
        } else if (preference == mSwapVolumeButtons) {
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

        } else if (preference == mDisableNavigationKeys) {
            mDisableNavigationKeys.setEnabled(false);
            HardwareKeyNavbarHelper.writeDisableHardwareNavkeysOption(getActivity(), mDisableNavigationKeys.isChecked());
            updateDisableNavkeysOption(false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDisableNavigationKeys.setEnabled(true);
                }
            }, 1000);
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
