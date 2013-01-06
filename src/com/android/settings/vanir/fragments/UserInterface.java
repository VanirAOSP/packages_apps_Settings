package com.android.settings.vanir.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Spannable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;

public class UserInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String KEY_BATTERY_PERCENTAGE = "battery_percentage";
    private static final String KEY_ENABLE_FAST_TORCH = "enable_fast_torch";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_CLOCK = "status_bar_show_clock";
    private static final String PREF_ENABLE = "clock_style";

    private CheckBoxPreference mBatteryPercentage;
    private CheckBoxPreference mFastTorch;
    private ListPreference mStatusBarAmPm;
    private ListPreference mStatusBarClock;

    Preference mLcdDensity;

    int newDensityValue;

    DensityChanger densityFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mBatteryPercentage = (CheckBoxPreference) findPreference(KEY_BATTERY_PERCENTAGE);
        mBatteryPercentage.setChecked((Settings.System.getInt(getContentResolver(),
            Settings.System.STATUS_BAR_BATTERY, 0) == 1));

        mFastTorch = (CheckBoxPreference) findPreference(KEY_ENABLE_FAST_TORCH);

        mStatusBarClock = (ListPreference) findPreference(PREF_ENABLE);
        mStatusBarClock.setOnPreferenceChangeListener(this);
        mStatusBarClock.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_CLOCK,
                1)));

        mStatusBarAmPm = (ListPreference) findPreference(STATUS_BAR_AM_PM);
        try {
            if (Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.TIME_12_24) == 24) {
                mStatusBarAmPm.setEnabled(false);
                mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
            }
        } catch (SettingNotFoundException e ) {
        }

        int statusBarAmPm = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_AM_PM, 2);
        mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
        mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
        mStatusBarAmPm.setOnPreferenceChangeListener(this);

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mBatteryPercentage) {
            boolean value = mBatteryPercentage.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.STATUS_BAR_BATTERY,
                    value ? 1 : 0);
        } else if (preference == mFastTorch) {
            boolean value = mFastTorch.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.ENABLE_FAST_TORCH, value?1:0);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) objValue);
            int index = mStatusBarAmPm.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
            return true;
        } else if (preference == mStatusBarClock) {
            int clockStyle = Integer.parseInt((String) objValue);
            int index = mStatusBarClock.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    STATUS_BAR_CLOCK, clockStyle);
            mStatusBarClock.setSummary(mStatusBarClock.getEntries()[index]);
            return true;
        }
            return false;
        }
}
