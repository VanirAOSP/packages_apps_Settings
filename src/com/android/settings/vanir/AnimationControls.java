package com.android.settings.vanir;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.IWindowManager;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.vanir.AnimBarPreference;
import com.android.settings.R;

import com.vanir.util.VanirAnimationHelper;

import java.util.Arrays;

public class AnimationControls extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String ACTIVITY_OPEN = "activity_open";
    private static final String ACTIVITY_CLOSE = "activity_close";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String TASK_OPEN = "task_open";
    private static final String TASK_CLOSE = "task_close";
    private static final String TASK_MOVE_TO_FRONT = "task_move_to_front";
    private static final String TASK_MOVE_TO_BACK = "task_move_to_back";
    private static final String ANIMATION_DURATION = "animation_duration";
    private static final String ANIMATION_NO_OVERRIDE = "animation_no_override";
    private static final String WALLPAPER_OPEN = "wallpaper_open";
    private static final String WALLPAPER_CLOSE = "wallpaper_close";
    private static final String WALLPAPER_INTRA_OPEN = "wallpaper_intra_open";
    private static final String WALLPAPER_INTRA_CLOSE = "wallpaper_intra_close";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";

    private SwitchPreference mAnimNoOverride;
    private ListPreference mWindowAnimationScale;
    private ListPreference mTransitionAnimationScale;
    private ListPreference mAnimatorDurationScale;
    private CheckBoxPreference mListViewAnimation;
    private AnimBarPreference mAnimationDuration;
    private ListPreference mActivityOpenPref;
    private ListPreference mActivityClosePref;
    private ListPreference mTaskOpenPref;
    private ListPreference mTaskClosePref;
    private ListPreference mTaskMoveToFrontPref;
    private ListPreference mTaskMoveToBackPref;
    private ListPreference mWallpaperOpen;
    private ListPreference mWallpaperClose;
    private ListPreference mWallpaperIntraOpen;
    private ListPreference mWallpaperIntraClose;

    private int[] mAnimations;
    private String[] mAnimationsStrings;
    private String[] mAnimationsNum;
    private IWindowManager mWindowManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_animation_controls);

        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        PreferenceScreen prefs = getPreferenceScreen();
        mAnimations = VanirAnimationHelper.getAnimationsList();
        int animqty = mAnimations.length;
        mAnimationsStrings = new String[animqty];
        mAnimationsNum = new String[animqty];
        for (int i = 0; i < animqty; i++) {
            mAnimationsStrings[i] = VanirAnimationHelper.getProperName(mContext, mAnimations[i]);
            mAnimationsNum[i] = String.valueOf(mAnimations[i]);
        }

        mAnimNoOverride = (SwitchPreference) findPreference(ANIMATION_NO_OVERRIDE);
        mAnimNoOverride.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE, 0) == 1);
        mAnimNoOverride.setOnPreferenceChangeListener(this);

        mWindowAnimationScale = addListPreference(WINDOW_ANIMATION_SCALE_KEY);
        mTransitionAnimationScale = addListPreference(TRANSITION_ANIMATION_SCALE_KEY);
        mAnimatorDurationScale = addListPreference(ANIMATOR_DURATION_SCALE_KEY);

        //ListView Animations
        mListViewAnimation = (CheckBoxPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        mListViewAnimation.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
            Settings.System.LISTVIEW_ANIMATION, 0) == 1));

        mActivityOpenPref = (ListPreference) findPreference(ACTIVITY_OPEN);
        mActivityOpenPref.setOnPreferenceChangeListener(this);
        mActivityOpenPref.setSummary(getProperSummary(mActivityOpenPref));
        mActivityOpenPref.setEntries(mAnimationsStrings);
        mActivityOpenPref.setEntryValues(mAnimationsNum);

        mActivityClosePref = (ListPreference) findPreference(ACTIVITY_CLOSE);
        mActivityClosePref.setOnPreferenceChangeListener(this);
        mActivityClosePref.setSummary(getProperSummary(mActivityClosePref));
        mActivityClosePref.setEntries(mAnimationsStrings);
        mActivityClosePref.setEntryValues(mAnimationsNum);

        mTaskOpenPref = (ListPreference) findPreference(TASK_OPEN);
        mTaskOpenPref.setOnPreferenceChangeListener(this);
        mTaskOpenPref.setSummary(getProperSummary(mTaskOpenPref));
        mTaskOpenPref.setEntries(mAnimationsStrings);
        mTaskOpenPref.setEntryValues(mAnimationsNum);

        mTaskClosePref = (ListPreference) findPreference(TASK_CLOSE);
        mTaskClosePref.setOnPreferenceChangeListener(this);
        mTaskClosePref.setSummary(getProperSummary(mTaskClosePref));
        mTaskClosePref.setEntries(mAnimationsStrings);
        mTaskClosePref.setEntryValues(mAnimationsNum);

        mTaskMoveToFrontPref = (ListPreference) findPreference(TASK_MOVE_TO_FRONT);
        mTaskMoveToFrontPref.setOnPreferenceChangeListener(this);
        mTaskMoveToFrontPref.setSummary(getProperSummary(mTaskMoveToFrontPref));
        mTaskMoveToFrontPref.setEntries(mAnimationsStrings);
        mTaskMoveToFrontPref.setEntryValues(mAnimationsNum);

        mTaskMoveToBackPref = (ListPreference) findPreference(TASK_MOVE_TO_BACK);
        mTaskMoveToBackPref.setOnPreferenceChangeListener(this);
        mTaskMoveToBackPref.setSummary(getProperSummary(mTaskMoveToBackPref));
        mTaskMoveToBackPref.setEntries(mAnimationsStrings);
        mTaskMoveToBackPref.setEntryValues(mAnimationsNum);

        mWallpaperOpen = (ListPreference) findPreference(WALLPAPER_OPEN);
        mWallpaperOpen.setOnPreferenceChangeListener(this);
        mWallpaperOpen.setSummary(getProperSummary(mWallpaperOpen));
        mWallpaperOpen.setEntries(mAnimationsStrings);
        mWallpaperOpen.setEntryValues(mAnimationsNum);

        mWallpaperClose = (ListPreference) findPreference(WALLPAPER_CLOSE);
        mWallpaperClose.setOnPreferenceChangeListener(this);
        mWallpaperClose.setSummary(getProperSummary(mWallpaperClose));
        mWallpaperClose.setEntries(mAnimationsStrings);
        mWallpaperClose.setEntryValues(mAnimationsNum);

        mWallpaperIntraOpen = (ListPreference) findPreference(WALLPAPER_INTRA_OPEN);
        mWallpaperIntraOpen.setOnPreferenceChangeListener(this);
        mWallpaperIntraOpen.setSummary(getProperSummary(mWallpaperIntraOpen));
        mWallpaperIntraOpen.setEntries(mAnimationsStrings);
        mWallpaperIntraOpen.setEntryValues(mAnimationsNum);

        mWallpaperIntraClose = (ListPreference) findPreference(WALLPAPER_INTRA_CLOSE);
        mWallpaperIntraClose.setOnPreferenceChangeListener(this);
        mWallpaperIntraClose.setSummary(getProperSummary(mWallpaperIntraClose));
        mWallpaperIntraClose.setEntries(mAnimationsStrings);
        mWallpaperIntraClose.setEntryValues(mAnimationsNum);

        int defaultDuration = Settings.System.getInt(getContentResolver(),
                Settings.System.ANIMATION_CONTROLS_DURATION, 0);
        mAnimationDuration = (AnimBarPreference) findPreference(ANIMATION_DURATION);
        mAnimationDuration.setInitValue((int) (defaultDuration));
        mAnimationDuration.setOnPreferenceChangeListener(this);
    }

    private ListPreference addListPreference(String prefKey) {
        ListPreference pref = (ListPreference) findPreference(prefKey);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if (preference == mListViewAnimation) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION, mListViewAnimation.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mAnimNoOverride) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            validator();

        } else if (preference == mActivityOpenPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[0], val);

        } else if (preference == mActivityClosePref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[1], val);

        } else if (preference == mTaskOpenPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[2], val);

        } else if (preference == mTaskClosePref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[3], val);

        } else if (preference == mTaskMoveToFrontPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[4], val);

        } else if (preference == mTaskMoveToBackPref) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[5], val);
        } else if (preference == mWallpaperOpen) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[6], val);
        } else if (preference == mWallpaperClose) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[7], val);
        } else if (preference == mWallpaperIntraOpen) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[8], val);
        } else if (preference == mWallpaperIntraClose) {

            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVITY_ANIMATION_CONTROLS[9], val);
        } else if (preference == mAnimationDuration) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ANIMATION_CONTROLS_DURATION,
                    val);

        } else if (preference == mWindowAnimationScale) {
            writeAnimationScaleOption(0, mWindowAnimationScale, newValue);
            return true;

        } else if (preference == mTransitionAnimationScale) {
            writeAnimationScaleOption(1, mTransitionAnimationScale, newValue);
            return true;

        } else if (preference == mAnimatorDurationScale) {
            writeAnimationScaleOption(2, mAnimatorDurationScale, newValue);
            return true;

        }
        preference.setSummary(getProperSummary(preference));
        return result;
    }
    private String getProperSummary(Preference preference) {
        String mString = "";
        if (preference == mActivityOpenPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[0];
        } else if (preference == mActivityClosePref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[1];
        } else if (preference == mTaskOpenPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[2];
        } else if (preference == mTaskClosePref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[3];
        } else if (preference == mTaskMoveToFrontPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[4];
        } else if (preference == mTaskMoveToBackPref) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[5];
        } else if (preference == mWallpaperOpen) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[6];
        } else if (preference == mWallpaperClose) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[7];
        } else if (preference == mWallpaperIntraOpen) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[8];
        } else if (preference == mWallpaperIntraClose) {
            mString = Settings.System.ACTIVITY_ANIMATION_CONTROLS[9];
        }

        int mNum = Settings.System.getInt(getContentResolver(), mString, 0);
        return mAnimationsStrings[mNum];
    }

    private void validator() {
        boolean cheeseburger = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE, 0) == 1;

        if (cheeseburger) {
            writeAnimationScaleOption(0, mWindowAnimationScale, null);
            writeAnimationScaleOption(1, mTransitionAnimationScale, null);
            writeAnimationScaleOption(2, mAnimatorDurationScale, null);
        } else {
            updateAnimationScaleValue(0, mWindowAnimationScale);
            updateAnimationScaleValue(1, mTransitionAnimationScale);
            updateAnimationScaleValue(2, mAnimatorDurationScale);
        }
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            CharSequence[] values = pref.getEntryValues();
            for (int i=0; i<values.length; i++) {
                float val = Float.parseFloat(values[i].toString());
                if (scale <= val) {
                    pref.setValueIndex(i);
                    pref.setSummary(pref.getEntries()[i]);
                    return;
                }
            }
            pref.setValueIndex(values.length-1);
            pref.setSummary(pref.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        try {
            float scale = newValue != null ? Float.parseFloat(newValue.toString()) : 1;
            mWindowManager.setAnimationScale(which, scale);
            updateAnimationScaleValue(which, pref);
        } catch (RemoteException e) {
        }
    }
}
