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
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.vanir.AnimBarPreference;
import com.android.settings.R;
import com.vanir.util.VanirAnimationHelper;

import java.util.Arrays;

public class AnimationControls extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String LISTVIEW_ANIMATIONS = "listview_animations";
    private static final String LISTVIEW_DURATION = "listview_duration";
    private static final String ANIMATION_DURATION = "animation_duration";
    private static final String ANIMATION_NO_OVERRIDE = "animation_no_override";
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";
    private static final String LISTVIEW_INTERPOLATOR = "listview_interpolator";
    private static final String KEY_TOAST_ANIMATION = "toast_animation";

    private CheckBoxPreference mAnimNoOverride;
    private ListPreference mWindowAnimationScale;
    private ListPreference mTransitionAnimationScale;
    private ListPreference mAnimatorDurationScale;
    private ListPreference mListViewAnimation;
    private AnimBarPreference mListViewDuration;
    private ListPreference mListViewInterpolator;
    private AnimBarPreference mAnimationDuration;
    private ListPreference mToastAnimation;

    private IWindowManager mWindowManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_animation_controls);

        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        mAnimNoOverride = (CheckBoxPreference) findPreference(ANIMATION_NO_OVERRIDE);
        mAnimNoOverride.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE, 0) == 1);

        mWindowAnimationScale = (ListPreference) findPreference(WINDOW_ANIMATION_SCALE_KEY);
        mWindowAnimationScale.setOnPreferenceChangeListener(this);
        
        mTransitionAnimationScale = (ListPreference) findPreference(TRANSITION_ANIMATION_SCALE_KEY);
        mTransitionAnimationScale.setOnPreferenceChangeListener(this);
        
        mAnimatorDurationScale = (ListPreference) findPreference(ANIMATOR_DURATION_SCALE_KEY);
        mAnimatorDurationScale.setOnPreferenceChangeListener(this);

        //ListView Animations
        mListViewAnimation = (ListPreference) findPreference(LISTVIEW_ANIMATIONS);
        int listviewanimation = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.LISTVIEW_ANIMATIONS, 1);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this); 

        int mDuration = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_SCROLL_DURATION, 0);
        mListViewDuration = (AnimBarPreference) findPreference(LISTVIEW_DURATION);
        mListViewDuration.setInitValue((int) (mDuration));
        mListViewDuration.setOnPreferenceChangeListener(this);

        int defaultDuration = Settings.System.getInt(getContentResolver(),
                Settings.System.ANIMATION_CONTROLS_DURATION, 0);
        mAnimationDuration = (AnimBarPreference) findPreference(ANIMATION_DURATION);
        mAnimationDuration.setInitValue((int) (defaultDuration));
        mAnimationDuration.setOnPreferenceChangeListener(this);

        mToastAnimation = (ListPreference) findPreference(KEY_TOAST_ANIMATION);
        mToastAnimation.setSummary(mToastAnimation.getEntry());
        int CurrentToastAnimation = Settings.System.getInt(getContentResolver(), Settings.System.ACTIVITY_ANIMATION_CONTROLS[10], 1);
        mToastAnimation.setValueIndex(CurrentToastAnimation); //set to index of default value
        mToastAnimation.setSummary(mToastAnimation.getEntries()[CurrentToastAnimation]);
        mToastAnimation.setOnPreferenceChangeListener(this);

        updateAnimationScaleOptions();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
       if (preference == mAnimNoOverride) {
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.ANIMATION_CONTROLS_NO_OVERRIDE,
                        mAnimNoOverride.isChecked());
        }
        return true;
    } 

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference == mAnimationDuration) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ANIMATION_CONTROLS_DURATION,
                    val);
            return true;

        } else if (preference == mListViewDuration) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LISTVIEW_SCROLL_DURATION,
                    val);
            return true;

        } else if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) newValue);
            int index = mListViewAnimation.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATIONS,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            return true;

        } else if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) newValue);
           int index = mListViewInterpolator.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
            return true;

       } else if (preference == mToastAnimation) {
            int index = mToastAnimation.findIndexOfValue((String) newValue);
            Settings.System.putString(getContentResolver(), Settings.System.ACTIVITY_ANIMATION_CONTROLS[10], (String) newValue);
            mToastAnimation.setSummary(mToastAnimation.getEntries()[index]);
            Toast.makeText(mContext, "Toast Test", Toast.LENGTH_SHORT).show();
            return true;

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
        return false;
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

    private void updateAnimationScaleOptions() {		
	    updateAnimationScaleValue(0, mWindowAnimationScale);		
        updateAnimationScaleValue(1, mTransitionAnimationScale);		
        updateAnimationScaleValue(2, mAnimatorDurationScale);		
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
