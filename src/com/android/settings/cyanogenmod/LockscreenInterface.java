/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
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
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class LockscreenInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String LOCKSCREEN_BACKGROUND = "lockscreen_background";
    private static final String LOCKSCREEN_IMAGE_CROPPER_PREF = "lockscreen_wallpaper_settings";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private static final String WALLPAPER_NAME = "lockscreen_wallpaper";
    private static final String KEY_ENABLE_MAXIMIZE_WIGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    private static final String KEY_LOCKSCREEN_TARGETS = "lockscreen_targets";
    private static final String KEY_ALLOW_ROTATION = "allow_rotation";
    private static final String KEY_ALLOW_POWER_MENU = "allow_power_menu";
    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String KEY_BLUR_BEHIND = "blur_behind";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";

    private Preference mBackgroundCropper;
    private ListPreference mBatteryStatus;
    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mSeeThrough;
    private CheckBoxPreference mMusicControls;
    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mAllowPM;
    private CheckBoxPreference mBlurBehind;
    private SeekBarPreference mBlurRadius;
    private CheckBoxPreference mEnableCameraWidget;

    private CheckBoxPreference mEnableModLock;
    private CheckBoxPreference mEnableMaximizeWidgets;

    private PreferenceCategory mLockscreenBackground;
    private Preference mLockscreenTargets;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Find categories
        PreferenceCategory generalCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        mEnableMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_MAXIMIZE_WIGETS);
        mLockscreenTargets = findPreference(KEY_LOCKSCREEN_TARGETS);

        mEnableModLock = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MODLOCK_ENABLED);
        if (mEnableModLock != null) {
            mEnableModLock.setOnPreferenceChangeListener(this);
        }

        mBatteryStatus = (ListPreference) findPreference(KEY_BATTERY_STATUS);
        if (mBatteryStatus != null) {
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }

        // Remove lockscreen button actions if device doesn't have hardware keys
        if (!hasButtons()) {
            generalCategory.removePreference(findPreference(KEY_LOCKSCREEN_BUTTONS));
        }

        // Enable or disable camera widget based on device and policy
        if (Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
            mLockUtils.setCameraEnabled(false);
        } else if (mLockUtils.isSecure()) {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }

        // Enable or disable application widget based on policy
        if (mEnableApplicationWidget != null) {
            if (!checkDisabledByPolicy(mEnableApplicationWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_APPLICATION_WIDGET)) {
                mEnableApplicationWidget.setEnabled(true);
            }
        }

        boolean canEnableModLockscreen = false;
        final Bundle keyguard_metadata = Utils.getApplicationMetadata(
                getActivity(), "com.android.keyguard");
        if (keyguard_metadata != null) {
            canEnableModLockscreen = keyguard_metadata.getBoolean(
                    "com.cyanogenmod.keyguard", false);
        }

        if (mEnableModLock != null && !canEnableModLockscreen) {
            generalCategory.removePreference(mEnableModLock);
            mEnableModLock = null;
        }

        // Remove cLock settings item if not installed
        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    mEnableMaximizeWidgets);
        }

        mLockscreenBackground = (PreferenceCategory) findPreference(LOCKSCREEN_BACKGROUND);

        mBackgroundCropper = findPreference(LOCKSCREEN_IMAGE_CROPPER_PREF);

        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_TRHOUGH);
        mSeeThrough.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);

        mBlurBehind = (CheckBoxPreference) findPreference(KEY_BLUR_BEHIND);
        mBlurBehind.setChecked(Settings.System.getInt(getContentResolver(),
            Settings.System.LOCKSCREEN_BLUR_BEHIND, 0) == 1);
        mBlurRadius = (SeekBarPreference) findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setProgress(Settings.System.getInt(getContentResolver(),
            Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);

        mAllowRotation = (CheckBoxPreference) findPreference(KEY_ALLOW_ROTATION);
        mAllowRotation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_ROTATION, 0) == 1);

        mAllowPM = (CheckBoxPreference) findPreference(KEY_ALLOW_POWER_MENU);
        mAllowPM.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, 0) == 1);

        mMusicControls = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MUSIC_CONTROLS);
        mMusicControls.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 1) == 1);
        mMusicControls.setOnPreferenceChangeListener(this);

        updateVisiblePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update custom widgets and camera
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(mLockUtils.getWidgetsEnabled());
        }

        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());
        }

        // Update battery status
        if (mBatteryStatus != null) {
            ContentResolver cr = getActivity().getContentResolver();
            int batteryStatus = Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }
        // Update mod lockscreen status
        if (mEnableModLock != null) {
            ContentResolver cr = getActivity().getContentResolver();
            boolean checked = Settings.System.getInt(
                    cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED, 1) == 1;
            mEnableModLock.setChecked(checked);
        }

        updateAvailableModLockPreferences();
    }

    private void updateAvailableModLockPreferences() {
        if (mEnableModLock == null) {
            return;
        }
        boolean enabled = !mEnableModLock.isChecked();
        if (mEnableKeyguardWidgets != null) {
            // Enable or disable lockscreen widgets based on policy
            if(!checkDisabledByPolicy(mEnableKeyguardWidgets,
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL)) {
                mEnableKeyguardWidgets.setEnabled(enabled);
            }
        }
        if (mEnableMaximizeWidgets != null) {
            mEnableMaximizeWidgets.setEnabled(enabled);
        }
        if (mLockscreenTargets != null) {
            mLockscreenTargets.setEnabled(enabled);
        }
        if (mBatteryStatus != null) {
            mBatteryStatus.setEnabled(enabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            mLockUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;

        } else if (KEY_ENABLE_CAMERA.equals(key)) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
            return true;

        } else if (preference == mAllowRotation) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_ROTATION,
                    mAllowRotation.isChecked()
                    ? 1 : 0);
            return true;

        } else if (preference == mAllowPM) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_ENABLE_POWER_MENU,
                    mAllowPM.isChecked()
                    ? 1 : 0);
            return true;


        } else if (preference == mSeeThrough) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH,
                    mSeeThrough.isChecked()
                    ? 1 : 0);
            // Disable lockscreen blur
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BLUR_BEHIND, 0);
            updateVisiblePreferences();
            return true;

        } else if (preference == mBlurBehind) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BLUR_BEHIND,
                    mBlurBehind.isChecked()
                    ? 1 : 0);
            // Disable lockscreen transparency
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH, 0);
            updateVisiblePreferences();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mBlurRadius) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)value);
            return true;

         } else if (preference == mMusicControls) {
            boolean cube = (Boolean) value;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MUSIC_CONTROLS, cube ? 1 : 0);
            return true;

         } else if (preference == mBatteryStatus) {
            int intValue = Integer.valueOf((String) value);
            int index = mBatteryStatus.findIndexOfValue((String) value);
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, intValue);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;

        } else if (preference == mEnableModLock) {
            boolean bvalue = (Boolean) value;
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_MODLOCK_ENABLED,
                    bvalue ? 1 : 0);
            // force it so update picks up correct values
            ((CheckBoxPreference) preference).setChecked(bvalue);
            updateAvailableModLockPreferences();
            return true;
         }
         return false;
    }

    private void updateVisiblePreferences() {
        boolean seeThrough = mSeeThrough.isChecked();
        boolean blurBehind = mBlurBehind.isChecked();
        boolean enableGFX = ActivityManager.isHighEndGfx();

        //disable preferences accordingly
        if (seeThrough) {
            mBlurBehind.setChecked(!seeThrough);
            mBlurBehind.setEnabled(!seeThrough);
        } else if (blurBehind) {
            mSeeThrough.setChecked(!blurBehind);
            mSeeThrough.setEnabled(!blurBehind);
        } else {
            mBlurBehind.setEnabled(true);
            mSeeThrough.setEnabled(true);
        }

        //only enable cropper if not transparent
        mBackgroundCropper.setEnabled(!seeThrough && !blurBehind);

        //only enable blur radius if blurry
        mBlurRadius.setEnabled(blurBehind);

        if (!enableGFX) {
            mBlurBehind.setChecked(false);
            mLockscreenBackground.removePreference(mBlurBehind);
            mLockscreenBackground.removePreference(mBlurRadius);
        }
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return (getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) > 0);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     * @return True if disabled.
     */
    private boolean checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
        return disabled;
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }
}
