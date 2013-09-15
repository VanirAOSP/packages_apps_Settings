/*
 * Copyright (C) 2012 ParanoidAndroid Project
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

package com.android.settings.vanir.fragments;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.INotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.bluetooth.DeviceListPreferenceFragment;
import com.android.settings.Utils;
import com.vanir.util.Helpers;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class Halo extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_HALO_ENABLED = "halo_enabled";
    private static final String KEY_HALO_PIE_ONLY = "halo_pie_only";
    private static final String KEY_HALO_STATE = "halo_state";
    private static final String KEY_HALO_HIDE = "halo_hide";
    private static final String KEY_HALO_NINJA = "halo_ninja";
    private static final String KEY_HALO_MSGBOX = "halo_msgbox";
    private static final String KEY_HALO_MSGBOX_ANIMATION = "halo_msgbox_animation";
    private static final String KEY_HALO_NOTIFY_COUNT = "halo_notify_count";
    private static final String KEY_HALO_UNLOCK_PING = "halo_unlock_ping";
    private static final String KEY_HALO_REVERSED = "halo_reversed";
    private static final String KEY_HALO_PAUSE = "halo_pause";
    private static final String KEY_HALO_SIZE = "halo_size";
    private static final String KEY_HALO_COLORS = "halo_colors";
    private static final String KEY_HALO_CIRCLE_COLOR = "halo_circle_color";
    private static final String KEY_HALO_EFFECT_COLOR = "halo_effect_color";
    private static final String KEY_HALO_BUBBLE_COLOR = "halo_bubble_color";
    private static final String KEY_HALO_BUBBLE_TEXT_COLOR = "halo_bubble_text_color";

    private SwitchPreference mHaloEnabled;
    private CheckBoxPreference mHaloPieOnly;
    private ListPreference mHaloState;
    private ListPreference mHaloSize;
    private ListPreference mHaloNotifyCount;
    private ListPreference mHaloMsgAnimate;

    private CheckBoxPreference mHaloNinja;
    private CheckBoxPreference mHaloMsgBox;
    private CheckBoxPreference mHaloUnlockPing;
    private CheckBoxPreference mHaloHide;
    private CheckBoxPreference mHaloReversed;
    private CheckBoxPreference mHaloPause;
    private CheckBoxPreference mHaloColors;

    ColorPickerPreference mHaloCircleColor;
    ColorPickerPreference mHaloEffectColor;
    ColorPickerPreference mHaloBubbleColor;
    ColorPickerPreference mHaloBubbleTextColor;

    private Context mContext;
    private INotificationManager mNotificationManager;
    private int mAllowedLocations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.halo_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mContext = getActivity();

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mHaloEnabled = (SwitchPreference) findPreference(KEY_HALO_ENABLED);
        mHaloEnabled.setChecked((Settings.System.getInt(getContentResolver(),
            Settings.System.HALO_ENABLED, 0) == 1));
        mHaloEnabled.setOnPreferenceChangeListener(this);

        mHaloPieOnly = (CheckBoxPreference) findPreference(KEY_HALO_PIE_ONLY);
        mHaloPieOnly.setChecked((Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.HALO_PIE_ONLY, 1) == 1));

        mHaloState = (ListPreference) prefSet.findPreference(KEY_HALO_STATE);
        mHaloState.setValue(String.valueOf((isHaloPolicyBlack() ? "1" : "0")));
        mHaloState.setOnPreferenceChangeListener(this);

        mHaloHide = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_HIDE);
        mHaloHide.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_HIDE, 0) == 1);

        mHaloNinja = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_NINJA);
        mHaloNinja.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_NINJA, 0) == 1);

        mHaloMsgBox = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_MSGBOX);
        mHaloMsgBox.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_MSGBOX, 1) == 1);

        mHaloUnlockPing = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_UNLOCK_PING);
        mHaloUnlockPing.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_UNLOCK_PING, 0) == 1);

        mHaloNotifyCount = (ListPreference) prefSet.findPreference(KEY_HALO_NOTIFY_COUNT);
        try {
            int haloCounter = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HALO_NOTIFY_COUNT, 4);
            mHaloNotifyCount.setValue(String.valueOf(haloCounter));
        } catch(Exception ex) {
            // fail...
        }
        mHaloNotifyCount.setOnPreferenceChangeListener(this);

        mHaloMsgAnimate = (ListPreference) prefSet.findPreference(KEY_HALO_MSGBOX_ANIMATION);
        try {
            int haloMsgAnimation = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HALO_MSGBOX_ANIMATION, 2);
            mHaloMsgAnimate.setValue(String.valueOf(haloMsgAnimation));
        } catch(Exception ex) {
            // fail...
        }
        mHaloMsgAnimate.setOnPreferenceChangeListener(this);

        mHaloReversed = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_REVERSED);
        mHaloReversed.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_REVERSED, 1) == 1);

        int isLowRAM = (ActivityManager.isLargeRAM()) ? 0 : 1;
        mHaloPause = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_PAUSE);
        mHaloPause.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_PAUSE, isLowRAM) == 1);

        mHaloSize = (ListPreference) findPreference(KEY_HALO_SIZE);
        try {
            float haloSize = Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.HALO_SIZE, 1.0f);
            mHaloSize.setValue(String.valueOf(haloSize));
        } catch(Exception ex) {
            // So what
        }
        mHaloSize.setOnPreferenceChangeListener(this);

        mHaloColors = (CheckBoxPreference) prefSet.findPreference(KEY_HALO_COLORS);
        mHaloColors.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HALO_COLORS, 0) == 1);

        mHaloEffectColor = (ColorPickerPreference) findPreference(KEY_HALO_EFFECT_COLOR);
        mHaloEffectColor.setOnPreferenceChangeListener(this);

        mHaloCircleColor = (ColorPickerPreference) findPreference(KEY_HALO_CIRCLE_COLOR);
        mHaloCircleColor.setOnPreferenceChangeListener(this);

        mHaloBubbleColor = (ColorPickerPreference) findPreference(KEY_HALO_BUBBLE_COLOR);
        mHaloBubbleColor.setOnPreferenceChangeListener(this);

        mHaloBubbleTextColor = (ColorPickerPreference) findPreference(KEY_HALO_BUBBLE_TEXT_COLOR);
        mHaloBubbleTextColor.setOnPreferenceChangeListener(this);

        }

    private boolean isHaloPolicyBlack() {
        try {
            return mNotificationManager.isHaloPolicyBlack();
        } catch (android.os.RemoteException ex) {
                // System dead
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHaloHide) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_HIDE, mHaloHide.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloPieOnly) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_PIE_ONLY, mHaloPieOnly.isChecked()
                    ? 1 : 0);

        } else if (preference == mHaloNinja) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_NINJA, mHaloNinja.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloMsgBox) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_MSGBOX, mHaloMsgBox.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloUnlockPing) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_UNLOCK_PING, mHaloUnlockPing.isChecked()
                    ? 1 : 0);

        } else if (preference == mHaloReversed) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_REVERSED, mHaloReversed.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloPause) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_PAUSE, mHaloPause.isChecked()
                    ? 1 : 0);
        } else if (preference == mHaloColors) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_COLORS, mHaloColors.isChecked()
                    ? 1 : 0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHaloEnabled) {
            boolean value = ((Boolean)newValue).booleanValue();
             Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HALO_ENABLED,
                    value ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mHaloState) {
            boolean state = Integer.valueOf((String) newValue) == 1;
            try {
                mNotificationManager.setHaloPolicyBlack(state);
            } catch (android.os.RemoteException ex) {
                // System dead
            }
            return true;
        } else if (preference == mHaloSize) {
            float haloSize = Float.valueOf((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.HALO_SIZE, haloSize);
            return true;
        } else if (preference == mHaloMsgAnimate) {
            int haloMsgAnimation = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_MSGBOX_ANIMATION, haloMsgAnimation);
            return true;
        } else if (preference == mHaloNotifyCount) {
            int haloNotifyCount = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_NOTIFY_COUNT, haloNotifyCount);
            return true;
        } else if (preference == mHaloCircleColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_CIRCLE_COLOR, intHex);
            return true;
        } else if (preference == mHaloEffectColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_EFFECT_COLOR, intHex);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mHaloBubbleColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_BUBBLE_COLOR, intHex);
            return true;
        } else if (preference == mHaloBubbleTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HALO_BUBBLE_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }
}
