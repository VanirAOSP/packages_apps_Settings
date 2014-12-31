package com.android.settings.vanir;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.vanir.widgets.AppMultiSelectListPreference;

import static android.hardware.Sensor.TYPE_PROXIMITY;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FlashNotifications extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_ENABLED = "flash_enable";
    private static final String KEY_POCKET_MODE = "flash_notifications_pocket_mode";
    private static final String KEY_LOW_PRIORITY = "allow_low_priority";
    private static final String KEY_NON_CLEARABLE = "allow_non_clearable";
    private static final String KEY_EXCLUDED_APPS = "excluded_apps";

    private SwitchPreference mEnabledPref;
    private CheckBoxPreference mPocketModePref;
    private CheckBoxPreference mAllowLowPriority;
    private CheckBoxPreference mAllowNonClearable;
    private AppMultiSelectListPreference mExcludedAppsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.flash_notifications);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver cr = getActivity().getContentResolver();

        mEnabledPref = (SwitchPreference) prefs.findPreference(KEY_ENABLED);
        mEnabledPref.setChecked(Settings.System.getInt(cr,
                Settings.System.FLASH_NOTIFICATIONS_ALPHA, 0) == 1);

        mPocketModePref = (CheckBoxPreference) prefs.findPreference(KEY_POCKET_MODE);
        mPocketModePref.setChecked(Settings.System.getInt(cr,
                Settings.System.FLASH_NOTIFICATIONS_POCKET_MODE, 0) == 1);
        if (!hasProximitySensor()) {
            getPreferenceScreen().removePreference(mPocketModePref);
        }

        mAllowLowPriority = (CheckBoxPreference) prefs.findPreference(KEY_LOW_PRIORITY);
        mAllowLowPriority.setChecked(Settings.System.getInt(cr,
                    Settings.System.FLASH_NOTIFICATIONS_LOW_PRIORITY, 0) == 1);

        mAllowNonClearable = (CheckBoxPreference) prefs.findPreference(KEY_NON_CLEARABLE);
        mAllowNonClearable.setChecked(Settings.System.getInt(cr,
                    Settings.System.FLASH_NOTIFICATIONS_NON_CLEARABLE, 0) == 1);

        mExcludedAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_APPS);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);
        setExcludedApps();
    }

    @Override
    public void onResume() {
		super.onResume();
		setExcludedApps();
	}

    private void setExcludedApps() {
	    AsyncTask.execute(new Runnable() {
            public void run() {
                Set<String> excludedNotifApps = getExcludedNotifApps(Settings.System.FLASH_NOTIFICATIONS_EXCLUDED_APPS);
                if (excludedNotifApps != null) mExcludedAppsPref.setValues(excludedNotifApps);
            }
        });
	}

    private boolean isKeyguardSecure() {
        LockPatternUtils mLockPatternUtils = new LockPatternUtils(getActivity());
        boolean isSecure = mLockPatternUtils.isSecure();
        return isSecure;
    }
        
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnabledPref) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FLASH_NOTIFICATIONS_ALPHA, mEnabledPref.isChecked() ? 1 : 0);

        } else if (preference == mAllowLowPriority) {
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FLASH_NOTIFICATIONS_LOW_PRIORITY,
                    mAllowLowPriority.isChecked() ? 1 : 0);

        } else if (preference == mAllowNonClearable) {
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.FLASH_NOTIFICATIONS_NON_CLEARABLE,
                    mAllowNonClearable.isChecked() ? 1 : 0);

        } else if (preference == mPocketModePref) {
			boolean enabled = mPocketModePref.isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FLASH_NOTIFICATIONS_POCKET_MODE, enabled ? 1 : 0);
            if (enabled) {
				String message = getActivity().getResources().getString(R.string.pocket_mode_warning);
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            }
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {
        if (pref == mExcludedAppsPref) {
			storeExcludedNotifApps((Set<String>) value, Settings.System.FLASH_NOTIFICATIONS_EXCLUDED_APPS);
			return true;
			
        } else {
            return false;
        }
    }

    private boolean hasProximitySensor() {
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(TYPE_PROXIMITY) != null;
    }

    Set<String> getExcludedNotifApps(String setting) {
        String excludedNotif = Settings.System.getString(getContentResolver(), setting);
        if (TextUtils.isEmpty(excludedNotif)) return null;

        return new HashSet<String>(Arrays.asList(excludedNotif.split("\\|")));
    }

    private void storeExcludedNotifApps(Set<String> values, String setting) {
        StringBuilder Notifbuilder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
			Notifbuilder.append(delimiter);
			Notifbuilder.append(value);
			delimiter = "|";
        }
        Settings.System.putString(getContentResolver(), setting, Notifbuilder.toString());
    }
}
