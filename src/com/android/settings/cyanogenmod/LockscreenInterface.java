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

package com.android.settings.cyanogenmod;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SeekBarPreference;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class LockscreenInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private static final String KEY_ALLOW_ROTATION = "allow_rotation";
    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String KEY_BLUR_BEHIND = "blur_behind";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mSeeThrough;
    private CheckBoxPreference mMusicControls;
    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mBlurBehind;
    private SeekBarPreference mBlurRadius;
    private CheckBoxPreference mEnableCameraWidget;

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
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);

        // Remove/disable custom widgets based on device RAM and policy
        if (ActivityManager.isLowRamDeviceStatic()) {
            // Widgets take a lot of RAM, so disable them on low-memory devices
            widgetsCategory.removePreference(findPreference(KEY_ENABLE_WIDGETS));
            mEnableKeyguardWidgets = null;
        } else {
            checkDisabledByPolicy(mEnableKeyguardWidgets,
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL);
        }

        // Enable or disable camera widget based on device and policy
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
        } else {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }

        // Remove cLock settings item if not installed
        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    findPreference(Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS));
        }

        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_TRHOUGH);

        mAllowRotation = (CheckBoxPreference) findPreference(KEY_ALLOW_ROTATION);
        mAllowRotation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_ROTATION, 0) == 1); 

        mMusicControls = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MUSIC_CONTROLS);
        mMusicControls.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 1) == 1);
        mMusicControls.setOnPreferenceChangeListener(this); 

        mBlurBehind = (CheckBoxPreference) findPreference(KEY_BLUR_BEHIND);
        mBlurBehind.setChecked(Settings.System.getInt(getContentResolver(), 
            Settings.System.LOCKSCREEN_BLUR_BEHIND, 0) == 1);
        mBlurRadius = (SeekBarPreference) findPreference(KEY_BLUR_RADIUS);
        mBlurRadius.setProgress(Settings.System.getInt(getContentResolver(), 
            Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
        mBlurRadius.setOnPreferenceChangeListener(this);

        updateBlurPrefs();
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
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_ROTATION, mAllowRotation.isChecked()
                    ? 1 : 0);
            return true;

        } else if (preference == mBlurBehind) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BLUR_BEHIND,
                    mBlurBehind.isChecked() ? 1 : 0);
            updateBlurPrefs();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mBlurRadius) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer)value);

         } else if (preference == mMusicControls) {
            boolean cube = (Boolean) value;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MUSIC_CONTROLS, cube ? 1 : 0);
            return true;
        }

         return true;
    }

    public void updateBlurPrefs() {
        // until i get around to digging through the frameworks to find where transparent lockscreen
        // is breaking the animation for blur lets just be a little dirty dirty dirty...
        if (mBlurBehind.isChecked()) {
            mSeeThrough.setEnabled(false);
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH, 1);
        } else {
            mSeeThrough.setEnabled(true);
            if (mSeeThrough.isChecked()) {
                Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH, 1);
            } else {
                Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SEE_THROUGH, 0);
            }
        }
    }

    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     */
    private void checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
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
