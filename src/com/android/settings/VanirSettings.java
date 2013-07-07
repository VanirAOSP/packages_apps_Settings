/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.text.Spannable;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.VolumePanel;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.widget.AlphaSeekBar;
import com.vanir.util.Helpers;

import java.util.Date;
import java.util.Calendar;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import com.android.settings.vanir.fragments.DensityChanger;

public class VanirSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "VanirSettings";

    private static final String KEY_VOLUME_OVERLAY = "volume_overlay";
    private static final String KEY_VOLUME_WAKE = "pref_volume_wake";
    private static final String KEY_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String KEY_QUIET_HOURS = "quiet_hours";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String KEY_ENABLE_FAST_TORCH = "enable_fast_torch";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_CLOCK = "status_bar_show_clock";
    private static final String PREF_CLOCK_PICKER = "clock_color";
    private static final String PREF_EXPANDED_CLOCK_PICKER = "expanded_clock_color";
    private static final String PREF_ENABLE = "clock_style";
    private static final String PREF_WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";
    private static final String KEY_DUAL_PANE = "dual_pane";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String KEY_EXPANDED_DESKTOP = "power_menu_expanded_desktop";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String PREF_STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String PREF_FORCE_DUAL_PANEL = "force_dualpanel";
    private static final String PREF_USER_MODE_UI = "user_mode_ui";
    private static final String PREF_HIDE_EXTRAS = "hide_extras";
    private static final CharSequence PREF_POWER_CRT_MODE = "system_power_crt_mode";
    private static final CharSequence PREF_POWER_CRT_SCREEN_OFF = "system_power_crt_screen_off";

    private ListPreference mStatusBarBattery;
    private CheckBoxPreference mFastTorch;
    private ListPreference mStatusBarAmPm;
    private ListPreference mStatusBarSignal;
    private ListPreference mStatusBarClock;
    private CheckBoxPreference mDualPane;
    private ColorPickerPreference mClockPicker;
    private ColorPickerPreference mExpandedClockPicker;
    private ListPreference mExpandedDesktopPref;
    private Preference mCustomLabel;
    private CheckBoxPreference mDualpane;
    private CheckBoxPreference mHideExtras;
    private ListPreference mUserModeUI;
    private ListPreference mCrtMode;
    private CheckBoxPreference mCrtOff;
    private CheckBoxPreference mWakeUpWhenPluggedOrUnplugged;
    private CheckBoxPreference mStatusBarNotifCount;
    private Preference mLcdDensity;
    private DensityChanger densityFragment;
    private ListPreference mVolumeOverlay;
    private CheckBoxPreference mVolumeWake;
    private CheckBoxPreference mVolBtnMusicCtrl;
    private CheckBoxPreference mVolumeAdjustSounds;
    private PreferenceScreen mQuietHours;

    int newDensityValue;
    private String mCustomLabelText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver mContentResolver = getContentResolver();

        addPreferencesFromResource(R.xml.vanir_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY);

        mStatusBarSignal = (ListPreference) findPreference(STATUS_BAR_SIGNAL);

        mFastTorch = (CheckBoxPreference) findPreference(KEY_ENABLE_FAST_TORCH);

        mStatusBarClock = (ListPreference) findPreference(PREF_ENABLE);
        mStatusBarClock.setOnPreferenceChangeListener(this);
        mStatusBarClock.setValue(Integer.toString(Settings.System.getInt(
            mContentResolver, Settings.System.STATUS_BAR_CLOCK, 1)));

        mClockPicker = (ColorPickerPreference) findPreference(PREF_CLOCK_PICKER);
        mClockPicker.setOnPreferenceChangeListener(this);

        mExpandedClockPicker = (ColorPickerPreference) findPreference(PREF_EXPANDED_CLOCK_PICKER);
        mExpandedClockPicker.setOnPreferenceChangeListener(this);

        mStatusBarAmPm = (ListPreference) findPreference(STATUS_BAR_AM_PM);
        try {
            if (Settings.System.getInt(mContentResolver,
                    Settings.System.TIME_12_24) == 24) {
                mStatusBarAmPm.setEnabled(false);
                mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
            }
        } catch (SettingNotFoundException e ) {
        }

        int statusBarAmPm = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_AM_PM, 2);
        mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
        mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
        mStatusBarAmPm.setOnPreferenceChangeListener(this);

        mHideExtras = (CheckBoxPreference) findPreference(PREF_HIDE_EXTRAS);
        mHideExtras.setChecked(Settings.System.getBoolean(mContentResolver,
                        Settings.System.HIDE_EXTRAS_SYSTEM_BAR, false));

        mUserModeUI = (ListPreference) findPreference(PREF_USER_MODE_UI);
        int uiMode = Settings.System.getInt(mContentResolver,
                Settings.System.CURRENT_UI_MODE, 0);
        mUserModeUI.setValue(Integer.toString(Settings.System.getInt(mContentResolver,
                Settings.System.USER_UI_MODE, uiMode)));
        mUserModeUI.setOnPreferenceChangeListener(this);

        mDualPane = (CheckBoxPreference) findPreference(KEY_DUAL_PANE);
        boolean preferDualPane = getResources().getBoolean(
                com.android.internal.R.bool.preferences_prefer_dual_pane);
        boolean dualPaneMode = Settings.System.getInt(mContentResolver,
                Settings.System.DUAL_PANE_PREFS, (preferDualPane ? 1 : 0)) == 1;
        mDualPane.setChecked(dualPaneMode);
        int statusBarBattery = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        PreferenceScreen prefSet = getPreferenceScreen();
        mExpandedDesktopPref = (ListPreference) prefSet.findPreference(KEY_EXPANDED_DESKTOP);
        mExpandedDesktopPref.setOnPreferenceChangeListener(this);
        int expandedDesktopValue = Settings.System.getInt(mContentResolver, Settings.System.EXPANDED_DESKTOP_STATUS_BAR_STATE, 0);
        mExpandedDesktopPref.setValue(String.valueOf(expandedDesktopValue));
        updateExpandedDesktopSummary(expandedDesktopValue);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        int signalStyle = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mStatusBarSignal.setValue(String.valueOf(signalStyle));
        mStatusBarSignal.setSummary(mStatusBarSignal.getEntry());
        mStatusBarSignal.setOnPreferenceChangeListener(this);

        mStatusBarNotifCount = (CheckBoxPreference) findPreference(PREF_STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked(Settings.System.getInt(mContentResolver,
                Settings.System.STATUSBAR_NOTIF_COUNT, 0) == 1);

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        boolean isCrtOffChecked = (Settings.System.getBoolean(mContentResolver,
                        Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF, true));
        mCrtOff = (CheckBoxPreference) findPreference(PREF_POWER_CRT_SCREEN_OFF);
        mCrtOff.setChecked(isCrtOffChecked);

        mCrtMode = (ListPreference) findPreference(PREF_POWER_CRT_MODE);
        int crtMode = Settings.System.getInt(mContentResolver,
                Settings.System.SYSTEM_POWER_CRT_MODE, 0);
        mCrtMode.setValue(Integer.toString(Settings.System.getInt(mContentResolver,
                Settings.System.SYSTEM_POWER_CRT_MODE, crtMode)));
        mCrtMode.setOnPreferenceChangeListener(this);

        mWakeUpWhenPluggedOrUnplugged = (CheckBoxPreference) findPreference(PREF_WAKEUP_WHEN_PLUGGED_UNPLUGGED);
        mWakeUpWhenPluggedOrUnplugged.setChecked(Settings.System.getBoolean(mContentResolver,
                Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED, true));

        // hide option if device is already set to never wake up
        if(!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen)) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mWakeUpWhenPluggedOrUnplugged);
        }

        mDualpane = (CheckBoxPreference) findPreference(PREF_FORCE_DUAL_PANEL);
        mDualpane.setChecked(Settings.System.getBoolean(mContentResolver,
                        Settings.System.FORCE_DUAL_PANEL, getResources().getBoolean(
                        com.android.internal.R.bool.preferences_prefer_dual_pane)));

        mQuietHours = (PreferenceScreen) findPreference(KEY_QUIET_HOURS);
        if (Settings.System.getInt(mContentResolver, Settings.System.QUIET_HOURS_ENABLED, 0) == 1) {
            mQuietHours.setSummary(getString(R.string.quiet_hours_active_from) + " " +
                    returnTime(Settings.System.getString(mContentResolver, Settings.System.QUIET_HOURS_START))
                    + " " + getString(R.string.quiet_hours_active_to) + " " +
                    returnTime(Settings.System.getString(mContentResolver, Settings.System.QUIET_HOURS_END)));
        } else {
           mQuietHours.setSummary(getString(R.string.quiet_hours_summary));
        }

        mVolumeOverlay = (ListPreference) findPreference(KEY_VOLUME_OVERLAY);
        mVolumeOverlay.setOnPreferenceChangeListener(this);
        int volumeOverlay = Settings.System.getInt(mContentResolver,
                Settings.System.MODE_VOLUME_OVERLAY,
                VolumePanel.VOLUME_OVERLAY_EXPANDABLE);
        mVolumeOverlay.setValue(Integer.toString(volumeOverlay));
        mVolumeOverlay.setSummary(mVolumeOverlay.getEntry());

        mVolumeAdjustSounds = (CheckBoxPreference) findPreference(KEY_VOLUME_ADJUST_SOUNDS);
        mVolumeAdjustSounds.setChecked(Settings.System.getInt(mContentResolver,
                Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED, 1) != 0);

        mVolBtnMusicCtrl = (CheckBoxPreference) findPreference(KEY_VOLBTN_MUSIC_CTRL);
        mVolBtnMusicCtrl.setChecked(Settings.System.getInt(mContentResolver,
                Settings.System.VOLBTN_MUSIC_CONTROLS, 0) != 0);

        mVolumeWake = (CheckBoxPreference) findPreference(KEY_VOLUME_WAKE);
        if (mVolumeWake != null) {
            mVolumeWake.setChecked(Settings.System.getInt(mContentResolver,
                    Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);
        }
    }

    private void openTransparencyDialog() {
        getFragmentManager().beginTransaction().add(new AdvancedTransparencyDialog(), null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // updateState in fact updates the UI to reflect the system state
    private void updateState(boolean force) {
        if (getActivity() == null) return;
        ContentResolver resolver = getContentResolver();

        if (Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_ENABLED, 0) == 1) {
            mQuietHours.setSummary(getString(R.string.quiet_hours_active_from) + " " +
                    returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_START))
                    + " " + getString(R.string.quiet_hours_active_to) + " " +
                    returnTime(Settings.System.getString(resolver, Settings.System.QUIET_HOURS_END)));
        } else {
            mQuietHours.setSummary(getString(R.string.quiet_hours_summary));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVolumeWake) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_WAKE_SCREEN,
            mVolumeWake.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mVolumeAdjustSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
            mVolumeAdjustSounds.isChecked() ? 1 : 0);
        } else if (preference == mVolBtnMusicCtrl) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLBTN_MUSIC_CONTROLS,
            mVolBtnMusicCtrl.isChecked() ? 1 : 0);
        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mFastTorch) {
            boolean value = mFastTorch.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.ENABLE_FAST_TORCH, value?1:0);
        } else if (preference == mDualPane) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DUAL_PANE_PREFS,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDualpane) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.FORCE_DUAL_PANEL,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    mContext.sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();

        } else if (preference == mHideExtras) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.HIDE_EXTRAS_SYSTEM_BAR,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference == mWakeUpWhenPluggedOrUnplugged) {
            Settings.System.putBoolean(getActivity().getContentResolver(),
                    Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED,
                    ((CheckBoxPreference) preference).isChecked());
        } else if (preference.getKey() != null && preference.getKey().equals("transparency_dialog")) {
            // getFragmentManager().beginTransaction().add(new
            // TransparencyDialog(), null).commit();
            openTransparencyDialog();
            return true;
        } else if (preference == mStatusBarNotifCount) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUSBAR_NOTIF_COUNT,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mCrtOff) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.SYSTEM_POWER_ENABLE_CRT_OFF,
                    ((TwoStatePreference) preference).isChecked());
            return true;
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mVolumeOverlay) {
            final int value = Integer.valueOf((String) objValue);
            final int index = mVolumeOverlay.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_VOLUME_OVERLAY, value);
            mVolumeOverlay.setSummary(mVolumeOverlay.getEntries()[index]);
        } else if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) objValue);
            int index = mStatusBarAmPm.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) objValue);
            int index = mStatusBarBattery.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarClock) {
            int clockStyle = Integer.parseInt((String) objValue);
            int index = mStatusBarClock.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    STATUS_BAR_CLOCK, clockStyle);
            mStatusBarClock.setSummary(mStatusBarClock.getEntries()[index]);
            return true;
        } else if (preference == mClockPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_COLOR, intHex);
            // Log.e("VANIR", "Statusbar: "+intHex + "");
        } else if (preference == mExpandedClockPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_EXPANDED_CLOCK_COLOR, intHex);
            // Log.e("VANIR", "Expanded: "+intHex + "");
        } else if (preference == mStatusBarSignal) {
            int signalStyle = Integer.valueOf((String) objValue);
            int index = mStatusBarSignal.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mStatusBarSignal.setSummary(mStatusBarSignal.getEntries()[index]);
            return true;
        } else if (preference == mExpandedDesktopPref) {
            int expandedDesktopValue = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.EXPANDED_DESKTOP_STATUS_BAR_STATE, expandedDesktopValue);
            updateExpandedDesktopSummary(expandedDesktopValue);
            return true;
        } else if (preference == mUserModeUI) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.USER_UI_MODE, Integer.parseInt((String) objValue));
            Helpers.restartSystemUI();
            return true;
                } else if (preference == mCrtMode) {
            int crtMode = Integer.valueOf((String) objValue);
            int index = mCrtMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE, crtMode);
            mCrtMode.setSummary(mCrtMode.getEntries()[index]);
            return true;
        }
        return true;
    }

    private String returnTime(String t) {
        if (t == null || t.equals("")) {
            return "";
        }
        int hr = Integer.parseInt(t.trim());
        int mn = hr;

        hr = hr / 60;
        mn = mn % 60;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hr);
        cal.set(Calendar.MINUTE, mn);
        Date date = cal.getTime();
        return DateFormat.getTimeFormat(getActivity().getApplicationContext()).format(date);
    }

    public static class AdvancedTransparencyDialog extends DialogFragment {

        private static final int KEYGUARD_ALPHA = 112;
        private static final int STATUSBAR_ALPHA = 0;
        private static final int STATUSBAR_KG_ALPHA = 1;
        private static final int NAVBAR_ALPHA = 2;
        private static final int NAVBAR_KG_ALPHA = 3;

        boolean linkTransparencies = true;
        CheckBox mLinkCheckBox, mMatchStatusbarKeyguard, mMatchNavbarKeyguard;
        ViewGroup mNavigationBarGroup;

        TextView mSbLabel;

        AlphaSeekBar mSeekBars[] = new AlphaSeekBar[4];

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setShowsDialog(true);
            setRetainInstance(true);
            linkTransparencies = getSavedLinkedState();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View layout = View.inflate(getActivity(), R.layout.dialog_transparency, null);
            mLinkCheckBox = (CheckBox) layout.findViewById(R.id.transparency_linked);
            mLinkCheckBox.setChecked(linkTransparencies);

            mNavigationBarGroup = (ViewGroup) layout.findViewById(R.id.navbar_layout);
            mSbLabel = (TextView) layout.findViewById(R.id.statusbar_label);
            mSeekBars[STATUSBAR_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.statusbar_alpha);
            mSeekBars[STATUSBAR_KG_ALPHA] = (AlphaSeekBar) layout
                    .findViewById(R.id.statusbar_keyguard_alpha);
            mSeekBars[NAVBAR_ALPHA] = (AlphaSeekBar) layout.findViewById(R.id.navbar_alpha);
            mSeekBars[NAVBAR_KG_ALPHA] = (AlphaSeekBar) layout
                    .findViewById(R.id.navbar_keyguard_alpha);

            mMatchStatusbarKeyguard = (CheckBox) layout.findViewById(R.id.statusbar_match_keyguard);
            mMatchNavbarKeyguard = (CheckBox) layout.findViewById(R.id.navbar_match_keyguard);

            try {
                // restore any saved settings
                int alphas[] = new int[2];
                final String sbConfig = Settings.System.getString(getActivity()
                        .getContentResolver(),
                        Settings.System.STATUS_BAR_ALPHA_CONFIG);
                if (sbConfig != null) {
                    String split[] = sbConfig.split(";");
                    alphas[0] = Integer.parseInt(split[0]);
                    alphas[1] = Integer.parseInt(split[1]);

                    mSeekBars[STATUSBAR_ALPHA].setCurrentAlpha(alphas[0]);
                    mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);

                    mMatchStatusbarKeyguard.setChecked(alphas[1] == KEYGUARD_ALPHA);

                    if (linkTransparencies) {
                        mSeekBars[NAVBAR_ALPHA].setCurrentAlpha(alphas[0]);
                        mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);
                    } else {
                        final String navConfig = Settings.System.getString(getActivity()
                                .getContentResolver(),
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG);
                        if (navConfig != null) {
                            split = navConfig.split(";");
                            alphas[0] = Integer.parseInt(split[0]);
                            alphas[1] = Integer.parseInt(split[1]);
                            mSeekBars[NAVBAR_ALPHA].setCurrentAlpha(alphas[0]);
                            mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(alphas[1]);

                            mMatchNavbarKeyguard.setChecked(alphas[1] == KEYGUARD_ALPHA);
                        }
                    }
                }
            } catch (Exception e) {
                resetSettings();
            }

            updateToggleState();
            mMatchStatusbarKeyguard.setOnCheckedChangeListener(mUpdateStatesListener);
            mMatchNavbarKeyguard.setOnCheckedChangeListener(mUpdateStatesListener);
            mLinkCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    linkTransparencies = isChecked;
                    saveSavedLinkedState(isChecked);
                    updateToggleState();
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(layout);
            builder.setTitle(getString(R.string.transparency_dialog_title));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (linkTransparencies) {
                        String config = mSeekBars[STATUSBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[STATUSBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.STATUS_BAR_ALPHA_CONFIG, config);
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, config);
                    } else {
                        String sbConfig = mSeekBars[STATUSBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[STATUSBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.STATUS_BAR_ALPHA_CONFIG, sbConfig);

                        String nbConfig = mSeekBars[NAVBAR_ALPHA].getCurrentAlpha() + ";" +
                                mSeekBars[NAVBAR_KG_ALPHA].getCurrentAlpha();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, nbConfig);
                    }
                }
            });

            return builder.create();
        }

        private void resetSettings() {
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_ALPHA_CONFIG, null);
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_ALPHA_CONFIG, null);
        }

        private void updateToggleState() {
            if (linkTransparencies) {
                mSbLabel.setText(R.string.transparency_dialog_transparency_sb_and_nv);
                mNavigationBarGroup.setVisibility(View.GONE);
            } else {
                mSbLabel.setText(R.string.transparency_dialog_statusbar);
                mNavigationBarGroup.setVisibility(View.VISIBLE);
            }

            mSeekBars[STATUSBAR_KG_ALPHA]
                    .setEnabled(!mMatchStatusbarKeyguard.isChecked());
            mSeekBars[NAVBAR_KG_ALPHA]
                    .setEnabled(!mMatchNavbarKeyguard.isChecked());

            // disable keyguard alpha if needed
            if (!mSeekBars[STATUSBAR_KG_ALPHA].isEnabled()) {
                mSeekBars[STATUSBAR_KG_ALPHA].setCurrentAlpha(KEYGUARD_ALPHA);
            }
            if (!mSeekBars[NAVBAR_KG_ALPHA].isEnabled()) {
                mSeekBars[NAVBAR_KG_ALPHA].setCurrentAlpha(KEYGUARD_ALPHA);
            }
        }

        @Override
        public void onDestroyView() {
            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage(null);
            super.onDestroyView();
        }

        private CompoundButton.OnCheckedChangeListener mUpdateStatesListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateToggleState();
            }
        };

        private boolean getSavedLinkedState() {
            return getActivity().getSharedPreferences("transparency", Context.MODE_PRIVATE)
                    .getBoolean("link", true);
        }

        private void saveSavedLinkedState(boolean v) {
            getActivity().getSharedPreferences("transparency", Context.MODE_PRIVATE).edit()
                    .putBoolean("link", v).commit();
        }
    }

    private void updateExpandedDesktopSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
			/* full expanded desktop */
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.expanded_desktop_summary_status_bar);
            mExpandedDesktopPref.setSummary(res.getString(R.string.summary_expanded_desktop, statusBarPresent));
        } else if (value == 1) {
			/* expanded desktop with statusbar only */
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.expanded_desktop_summary_no_status_bar);
            mExpandedDesktopPref.setSummary(res.getString(R.string.summary_expanded_desktop, statusBarPresent));
        } else if (value == 2) {
			/* expanded desktop deactivated */
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.expanded_desktop_off);
            mExpandedDesktopPref.setSummary(res.getString(R.string.summary_expanded_desktop, statusBarPresent));
        }
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }
}
