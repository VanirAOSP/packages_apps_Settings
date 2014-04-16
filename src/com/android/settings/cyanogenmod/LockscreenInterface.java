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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockscreenInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String LOCKSCREEN_BACKGROUND = "lockscreen_background";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCKSCREEN_MUSIC_CONTROLS = "lockscreen_music_controls";
    private static final String WALLPAPER_NAME = "lockscreen_wallpaper";
    private static final String KEY_ENABLE_MAXIMIZE_WIGETS = "lockscreen_maximize_widgets";
    private static final String LOCKSCREEN_BACKGROUND_STYLE = "lockscreen_background_style";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    private static final String LOCKSCREEN_BACKGROUND_COLOR_FILL = "lockscreen_background_color_fill";
    private static final String KEY_ALLOW_ROTATION = "allow_rotation";
    private static final String KEY_SEE_TRHOUGH = "see_through";
    private static final String KEY_BLUR_BEHIND = "blur_behind";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";

    private ListPreference mBatteryStatus;
    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mSeeThrough;
    private CheckBoxPreference mMusicControls;
    private CheckBoxPreference mAllowRotation;
    private CheckBoxPreference mBlurBehind;
    private SeekBarPreference mBlurRadius;
    private CheckBoxPreference mEnableCameraWidget;

    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int COLOR_FILL = 0;
    private static final int CUSTOM_IMAGE = 1;
    private static final int DEFAULT = 2;

    private ColorPickerPreference mLockColorFill;
    private CheckBoxPreference mEnableModLock;
    private CheckBoxPreference mEnableMaximizeWidgets;
    private ListPreference mLockBackground;

    private PreferenceCategory mLockscreenBackground;
    private File wallpaperImage;
    private File wallpaperTemporary;

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

        mLockBackground = (ListPreference) findPreference(LOCKSCREEN_BACKGROUND_STYLE);
        mLockBackground.setOnPreferenceChangeListener(this);
        mLockBackground.setValue(Integer.toString(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2)));
        mLockBackground.setSummary(mLockBackground.getEntry());

        mLockColorFill = (ColorPickerPreference) findPreference(LOCKSCREEN_BACKGROUND_COLOR_FILL);
        mLockColorFill.setOnPreferenceChangeListener(this);
        mLockColorFill.setSummary(ColorPickerPreference.convertToARGB(
                Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_COLOR, 0x00000000)));

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

        } else if (preference == mBlurBehind) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BLUR_BEHIND,
                    mBlurBehind.isChecked() ? 1 : 0);
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

        } else if (preference == mLockBackground) {
            int index = mLockBackground.findIndexOfValue(String.valueOf(value));
            preference.setSummary(mLockBackground.getEntries()[index]);
            return handleBackgroundSelection(index);

        } else if (preference == mLockColorFill) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            preference.setSummary(hex);
            int color = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_COLOR, color);
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
        int visible = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);

        if (visible == 0) {
			mLockColorFill.setEnabled(true);
        } else {
			mLockColorFill.setEnabled(false);
        }
        if (visible != 2) {
            mBlurBehind.setEnabled(false);
            mBlurRadius.setEnabled(false);
        } else {
            mBlurBehind.setEnabled(true);
            mBlurRadius.setEnabled(true);
        }
        if (visible != 1) {
            mSeeThrough.setEnabled(true);
        } else {
            mSeeThrough.setEnabled(false);
        }
	}

    private Uri getLockscreenExternalUri() {
        File dir = getActivity().getExternalCacheDir();
        File wallpaper = new File(dir, WALLPAPER_NAME);
        return Uri.fromFile(wallpaper);
	}

    private boolean handleBackgroundSelection(int index) {
        if (index == COLOR_FILL) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 0);
            updateVisiblePreferences();
            return true;
        } else if (index == CUSTOM_IMAGE) {
            // Used to reset the image when already set
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
            // Launches intent for user to select an image/crop it to set as background
            Display display = getActivity().getWindowManager().getDefaultDisplay();

            int width = getActivity().getWallpaperDesiredMinimumWidth();
            int height = getActivity().getWallpaperDesiredMinimumHeight();
            float spotlightX = (float)display.getWidth() / width;
            float spotlightY = (float)display.getHeight() / height;

            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("aspectX", width);
            intent.putExtra("aspectY", height);
            intent.putExtra("outputX", width);
            intent.putExtra("outputY", height);
            intent.putExtra("spotlightX", spotlightX);
            intent.putExtra("spotlightY", spotlightY);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getLockscreenExternalUri());

            startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
        } else if (index == DEFAULT) {
            // Sets background to default
            Settings.System.putInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 2);
            updateVisiblePreferences();
            return true;
        }

        return false;
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_WALLPAPER) {
            FileOutputStream wallpaperStream = null;
            try {
                wallpaperStream = getActivity().openFileOutput(WALLPAPER_NAME,
                        Context.MODE_WORLD_READABLE);

            } catch (FileNotFoundException e) {
                return; // NOOOOO
            }
            Uri selectedImageUri = getLockscreenExternalUri();
            Bitmap bitmap;
            if (data != null) {
                Uri mUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),
                            mUri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, wallpaperStream);

                    Toast.makeText(getActivity(), getResources().getString(R.string.
                            background_result_successful), Toast.LENGTH_LONG).show();
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.LOCKSCREEN_BACKGROUND_STYLE, 1);
                    updateVisiblePreferences();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, wallpaperStream);
                } catch (NullPointerException npe) {
                    Log.e(TAG, "SeletedImageUri was null.");
                    Toast.makeText(getActivity(), getResources().getString(R.string.
                            background_result_not_successful), Toast.LENGTH_LONG).show();
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
            }
        }
    }
}
