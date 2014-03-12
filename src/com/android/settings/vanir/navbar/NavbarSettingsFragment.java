package com.android.settings.vanir.navbar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.BaseSetting;
import com.android.settings.BaseSetting.OnSettingChangedListener;
import com.android.settings.R;
import com.android.settings.SingleChoiceSetting;

import com.vanir.util.DeviceUtils;

public class NavbarSettingsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private static final String NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    private static final String NAVIGATION_BAR_WIDTH = "navigation_bar_width";

    private SeekBar mNavigationBarHeight;
    private SeekBar mNavigationBarHeightLandscape;
    private SeekBar mNavigationBarWidth;
    private TextView mBarHeight_Text;
    private TextView mBarHeightLandscape_Text;
    private TextView mBarWidth_Text;

    private Switch mEnabledSwitch;
    private int deviceKeys = 0;

    private static int HValue;
    private static int LValue;
    private static int WValue;
    private static boolean firstShot = true;
    private static int mDefaultHeight;
    private static int mDefaultHeightLandscape;
    private static int mDefaultWidth;

    private static final double MIN_HEIGHT_SCALAR = 0.666;
    private static final double MAX_HEIGHT_SCALAR = 1.5;
    private static final double MIN_WIDTH_SCALAR = 0.6;
    private static final double MAX_WIDTH_SCALAR = 1.5;

    public NavbarSettingsFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        deviceKeys = getResources().getInteger(com.android.internal.R.integer.config_deviceHardwareKeys);

        if (deviceKeys > 0) {
            mEnabledSwitch = new Switch(getActivity());

            final int padding = getActivity().getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
            mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
            mEnabledSwitch.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (deviceKeys > 0) {
            getActivity().getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            getActivity().getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
            mEnabledSwitch.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (deviceKeys > 0) {
            getActivity().getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            getActivity().getActionBar().setCustomView(null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mNavigationBarHeight.setProgress(HValue);
        mNavigationBarHeightLandscape.setProgress(LValue);
        mNavigationBarWidth.setProgress(WValue);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_navbar_settings, container, false);

        mNavigationBarHeight = (SeekBar) v.findViewById(R.id.navigation_bar_height); // make seekbar object
        mBarHeight_Text = (TextView) v.findViewById(R.id.navigation_bar_height_text);
        mBarHeight_Text.setText(getString(R.string.navigation_bar_height_text));

        mNavigationBarHeightLandscape = (SeekBar) v.findViewById(R.id.navigation_bar_height_landscape); // make seekbar object
        mBarHeightLandscape_Text = (TextView) v.findViewById(R.id.navigation_bar_height_landscape_text);
        mBarHeightLandscape_Text.setText(getString(R.string.navigation_bar_height_landscape_text));

        mNavigationBarWidth = (SeekBar) v.findViewById(R.id.navigation_bar_width); // make seekbar object
        mBarWidth_Text = (TextView) v.findViewById(R.id.navigation_bar_width_text);
        mBarWidth_Text.setText(getString(R.string.navigation_bar_width_text));

        if (firstShot) {
            final Resources res = getActivity().getResources();
            mDefaultHeight = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
            mDefaultHeightLandscape = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height_landscape);
            mDefaultWidth = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_width);
            firstShot = false;
        }

        final ContentResolver cr = getActivity().getContentResolver();
        HValue = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_HEIGHT, mDefaultHeight);
        LValue = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, mDefaultHeightLandscape);
        WValue = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_WIDTH, mDefaultWidth);

        final int currentHeightPercent = (int)(100.0 *
                ( HValue - MIN_HEIGHT_SCALAR * mDefaultHeight) /
                ( MAX_HEIGHT_SCALAR * mDefaultHeight - MIN_HEIGHT_SCALAR * mDefaultHeight ));
        final int currentHeightLandscapePercent = (int)(100.0 *
                ( LValue - MIN_HEIGHT_SCALAR * mDefaultHeightLandscape) /
                ( MAX_HEIGHT_SCALAR * mDefaultHeightLandscape - MIN_HEIGHT_SCALAR * mDefaultHeightLandscape ));
        final int currentWidthPercent = (int)(100.0 *
                ( WValue - MIN_WIDTH_SCALAR * mDefaultWidth) /
                ( MAX_WIDTH_SCALAR * mDefaultWidth - MIN_WIDTH_SCALAR * mDefaultWidth ));

        mNavigationBarHeight.setProgress(currentHeightPercent);
        mNavigationBarHeight.setOnSeekBarChangeListener(this);
        mNavigationBarHeightLandscape.setProgress(currentHeightLandscapePercent);
        mNavigationBarHeightLandscape.setOnSeekBarChangeListener(this);
        mNavigationBarWidth.setProgress(currentWidthPercent);
        mNavigationBarWidth.setOnSeekBarChangeListener(this);

        if (DeviceUtils.isPhone(getActivity())) {
            mBarHeightLandscape_Text.setVisibility(View.GONE);
            mNavigationBarHeightLandscape.setVisibility(View.GONE);
        } else {
            mBarWidth_Text.setVisibility(View.GONE);
            mNavigationBarWidth.setVisibility(View.GONE);
        }
        
        return v;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch) {
            boolean value = ((Boolean)isChecked).booleanValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_NAVIGATION_BAR,
                    value ? 1 : 0);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
        if (fromUser) {
            double min = 0;
            double max = 0;

            if (seekbar == mNavigationBarWidth) {
                min = MIN_WIDTH_SCALAR * mDefaultWidth;
                max = MAX_WIDTH_SCALAR * mDefaultWidth;
                WValue = (int)(min + ((double)progress/100.0)*(max - min));
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_WIDTH, WValue);
            } else if (seekbar == mNavigationBarHeight) {
                min = MIN_HEIGHT_SCALAR * mDefaultHeight;
                max = MAX_HEIGHT_SCALAR * mDefaultHeight;
                HValue = (int)(min + ((double)progress/100.0)*(max - min));
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT, HValue);
            } else if (seekbar == mNavigationBarHeightLandscape) {
                min = MIN_HEIGHT_SCALAR * mDefaultHeightLandscape;
                max = MAX_HEIGHT_SCALAR * mDefaultHeightLandscape;
                LValue = (int)(min + ((double)progress/100.0)*(max - min));
                Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, LValue);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // time to write the values..
    }
}
