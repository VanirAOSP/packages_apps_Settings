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
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
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

    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_ALLOW_ROTATION = "allow_rotation";
    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String KEY_BLUR_BEHIND = "blur_behind";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_DISABLE_CAMERA_WIDGET = "disable_camera_widget";

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mSeeThrough;
    private CheckBoxPreference mMusicControls;
    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mBlurBehind;
    private SeekBarPreference mBlurRadius;
    private CheckBoxPreference mCameraWidget;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private DevicePolicyManager mDPM;
    private boolean mIsPrimary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        PreferenceCategory widgetsCategory = (PreferenceCategory) findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Determine which user is logged in
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mIsPrimary) {
            // Its the primary user, show all the settings
            if (!Utils.isPhone(getActivity())) {
                if (widgetsCategory != null) {
                    widgetsCategory.removePreference(
                            findPreference(Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS));
                }
            }

        } else {
            // Secondary user is logged in, remove all primary user specific preferences
        }

        // This applies to all users
        // Enable or disable keyguard widget checkbox based on DPM state
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        if (mEnableKeyguardWidgets != null) {
            if (ActivityManager.isLowRamDeviceStatic()) {
                    /*|| mLockPatternUtils.isLockScreenDisabled()) {*/
                // Widgets take a lot of RAM, so disable them on low-memory devices
                if (widgetsCategory != null) {
                    widgetsCategory.removePreference(findPreference(KEY_ENABLE_WIDGETS));
                    mEnableKeyguardWidgets = null;
                }
            } else {
                final boolean disabled = (0 != (mDPM.getKeyguardDisabledFeatures(null)
                        & DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL));
                if (disabled) {
                    mEnableKeyguardWidgets.setSummary(
                            R.string.security_enable_widgets_disabled_summary);
                }
                mEnableKeyguardWidgets.setEnabled(!disabled);
            }
        }

        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_TRHOUGH);

        // Lockscreen Camera Widget
        mCameraWidget = (CheckBoxPreference) findPreference(KEY_DISABLE_CAMERA_WIDGET);
        mCameraWidget.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DISABLE_CAMERA_WIDGET, 0) == 1);

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
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(lockPatternUtils.getWidgetsEnabled());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_ENABLE_WIDGETS.equals(key)) {
            lockPatternUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;

        } else if (preference == mSeeThrough) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, mSeeThrough.isChecked()
                    ? 1 : 0);
            return true;

        } else if (preference == mAllowRotation) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_ROTATION, mAllowRotation.isChecked()
                    ? 1 : 0);
            return true;

        } else if (preference == mCameraWidget) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DISABLE_CAMERA_WIDGET, mCameraWidget.isChecked()
                    ? 1 : 0);

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

    public static class DeviceAdminLockscreenReceiver extends DeviceAdminReceiver {}

}
