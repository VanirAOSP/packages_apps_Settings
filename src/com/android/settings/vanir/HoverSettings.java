package com.android.settings.vanir;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.INotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.vanir.AppMultiSelectListPreference;
import com.android.settings.vanir.HoverSeekBar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HoverSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String TAG = "HoverSettings";

    private static final String KEY_EXCLUDED_APPS = "hover_excluded_apps";
    private static final String KEY_HOVER_FLOATING = "hover_floating_mode";
    private static final String KEY_HOVER_FULLSCREEN = "hover_require_fullscreen";
    private static final String KEY_HOVER_DURATION = "hover_duration";
    private static final String KEY_HOVER_DISMISS_ALL = "hover_force_removable";
    private static final String KEY_HIDE_LOW_PRIORITY = "hover_low_priority";
    private static final String KEY_HIDE_NON_CLEARABLE = "hover_non_clearable";
    private static final String KEY_EXCLUDE_FOREGROUND = "hover_exclude_foreground";

    private INotificationManager mNotificationManager;
    private PackageManager mPm;

    private Switch mEnabledSwitch;

    private boolean mDialogClicked;
    private Dialog mEnableDialog;

    private CheckBoxPreference mFloating;
    private HoverSeekBar mHoverDuration;
    private CheckBoxPreference mRequireFullScreen;
    private CheckBoxPreference mForceRemovable;
    private CheckBoxPreference mExcludeForeground;
    private CheckBoxPreference mHideLowPriority;
    private CheckBoxPreference mHideNonClearable;
    private AppMultiSelectListPreference mExcludedAppsPref;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
        mEnabledSwitch.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HOVER_ENABLED, 0) == 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.hover_settings);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver cr = getActivity().getContentResolver();

        mNotificationManager = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        mFloating = (CheckBoxPreference) prefs.findPreference(KEY_HOVER_FLOATING);
        mRequireFullScreen = (CheckBoxPreference) prefs.findPreference(KEY_HOVER_FULLSCREEN);
        mExcludeForeground = (CheckBoxPreference) prefs.findPreference(KEY_EXCLUDE_FOREGROUND);
        mHideLowPriority = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideNonClearable = (CheckBoxPreference) prefs.findPreference(KEY_HIDE_NON_CLEARABLE);
        mForceRemovable = (CheckBoxPreference) prefs.findPreference(KEY_HOVER_DISMISS_ALL);

        // default duration is 3.5 seconds
        mHoverDuration = (HoverSeekBar) prefs.findPreference(KEY_HOVER_DURATION);
        int PRIME = Settings.System.getInt(cr, Settings.System.HOVER_DURATION, 3500);
        mHoverDuration.setValue(PRIME);
        mHoverDuration.setOnPreferenceChangeListener(this);
        updateHoverDuration(PRIME);

        mExcludedAppsPref = (AppMultiSelectListPreference) prefs.findPreference(KEY_EXCLUDED_APPS);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

        updateDependency();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mForceRemovable != null )
            mForceRemovable.setEnabled(!mHideNonClearable.isChecked());

        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);

        updateDependency();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mFloating) {
            Settings.System.putInt(cr, Settings.System.HOVER_FLOATING,
                    mFloating.isChecked() ? 1 : 0);

        } else if (preference == mRequireFullScreen) {
            Settings.System.putInt(cr, Settings.System.HOVER_REQUIRE_FULLSCREEN,
                    mRequireFullScreen.isChecked() ? 1 : 0);

        } else if (preference == mForceRemovable) {
            Settings.System.putInt(cr, Settings.System.HOVER_REQUIRE_FULLSCREEN,
                    mForceRemovable.isChecked() ? 1 : 0);

        } else if (preference == mExcludeForeground) {
            Settings.System.putInt(cr, Settings.System.HOVER_EXCLUDE_FOREGROUND,
                    mExcludeForeground.isChecked() ? 1 : 0);

        } else if (preference == mHideLowPriority) {
            Settings.System.putInt(cr, Settings.System.HOVER_LOW_PRIORITY,
                    mHideLowPriority.isChecked() ? 1 : 0);

        } else if (preference == mHideNonClearable) {
            Settings.System.putInt(cr, Settings.System.HOVER_NON_CLEARABLE,
                    mHideNonClearable.isChecked() ? 1 : 0);
            mForceRemovable.setEnabled(!mHideNonClearable.isChecked());

        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {

        if (pref == mHoverDuration) {
            int PRIME = (Integer)value;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HOVER_DURATION, PRIME);
            updateHoverDuration(PRIME);
            return true;

        } else if (pref == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) value);
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch) {
            if (isChecked) {
                mDialogClicked = false;
                if (mEnableDialog != null) {
                    dismissDialogs();
                }
            }
            boolean value = ((Boolean)isChecked).booleanValue();
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.HOVER_ENABLED,
                    value ? 1 : 0);

            updateHaloPreference();
            updateDependency();
        }
    }

    private void dismissDialogs() {
        if (mEnableDialog != null) {
            mEnableDialog.dismiss();
            mEnableDialog = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mEnableDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mDialogClicked = true;
            }
        }
    }

    private void updateDependency() {
        boolean enable = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HOVER_ENABLED, 0) == 1;

        if (mFloating != null) mFloating.setEnabled(enable);
        if (mHoverDuration != null) mHoverDuration.setEnabled(enable);
        if (mRequireFullScreen != null) mRequireFullScreen.setEnabled(enable);
        if (mExcludeForeground != null) mExcludeForeground.setEnabled(enable);
        if (mForceRemovable != null) mForceRemovable.setEnabled(enable);
        if (mHideLowPriority != null) mHideLowPriority.setEnabled(enable);
        if (mHideNonClearable != null) mHideNonClearable.setEnabled(enable);
        if (mExcludedAppsPref != null) mExcludedAppsPref.setEnabled(enable);
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(getContentResolver(),
                Settings.System.HOVER_EXCLUDED_APPS);

        if (TextUtils.isEmpty(excluded)) return null;
        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(Set<String> values) {
        Set<String> excludedApps = getExcludedApps();
        boolean isUserChange = excludedApps != values;

        StringBuilder builder = new StringBuilder();
        String delimiter = "";

        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
            try {
                mNotificationManager.setHoverBlacklistStatus(value, true);
            } catch (android.os.RemoteException ex) {
                // oops?
            }
        }
        Settings.System.putString(getContentResolver(),
                Settings.System.HOVER_EXCLUDED_APPS, builder.toString());

    }

    public void onDismiss(DialogInterface dialog) {
        // ahh!
    }

    private void updateHoverDuration(int value) {
        mHoverDuration.setTitle(getResources().getText(R.string.hover_duration)
                + " " + ((float)(value)) / 1000.0f
                + " " + getText(R.string.seconds));
    }

    private void updateHaloPreference() {
        boolean value = Settings.System.getInt(getContentResolver(), 
                    Settings.System.HOVER_ENABLED, 0) == 1;
        if (value) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HALO_ENABLED, 0);
        }
    }
}
