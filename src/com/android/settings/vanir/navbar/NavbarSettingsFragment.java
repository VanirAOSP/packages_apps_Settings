package com.android.settings.vanir.navbar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
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
    private static int deviceKeys = 0;

    private static int HValue;
    private static int LValue;
    private static int WValue;
    private static int defNavBarSize;
    private static int preSizer;
    private static int minimumBarSize;
    private static boolean firstShot = true;

    public NavbarSettingsFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        if (deviceKeys > 0) {
            final Activity activity = getActivity();
            mEnabledSwitch = new Switch(activity);

            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
            mEnabledSwitch.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (deviceKeys > 0) {
            final Activity activity = getActivity();
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
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
            final Activity activity = getActivity();
            activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(null);
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

        initHeightValues();

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
    public void onProgressChanged(SeekBar seekBar, int progress,
                                    boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.navigation_bar_height) {
                setNewHeight(seekBar, progress);
            } else if (seekBar.getId() == R.id.navigation_bar_height_landscape) {
                setNewHeight(seekBar, progress);
            } else if (seekBar.getId() == R.id.navigation_bar_width) {
                setNewHeight(seekBar, progress);
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

    private void setNewHeight(SeekBar seekbar, int progress) {

        if (seekbar == mNavigationBarWidth) {
            int width = (percentToPixels(progress) + minimumBarSize);
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_WIDTH,
                    width);
            setInitValue(progress, seekbar);

        } else if (seekbar == mNavigationBarHeight) {
            int height = (percentToPixels(progress) + minimumBarSize);
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT,
                    height);
            setInitValue(progress, seekbar);

        } else if (seekbar == mNavigationBarHeightLandscape) {
            int height = (percentToPixels(progress) + minimumBarSize);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE,
                    height);
            setInitValue(progress, seekbar);
        }
    }

    private void initHeightValues() {

        // Navigation bar height
        int customSize = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT, defNavBarSize());
        mNavigationBarHeight.setProgress((int)((float)customSize / (float)defNavBarSize() * 100.0f));
        mNavigationBarHeight.setOnSeekBarChangeListener(this);

        // Navigation bar height - landscape
        customSize = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, defNavBarSize());
        mNavigationBarHeightLandscape.setProgress((int)((float)customSize / (float)defNavBarSize() * 100.0f));
        mNavigationBarHeightLandscape.setOnSeekBarChangeListener(this);

        // Navigation bar width (phones only)
        customSize = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.NAVIGATION_BAR_WIDTH, defNavBarSize());
        mNavigationBarWidth.setProgress((int)((float)customSize / (float)defNavBarSize() * 100.0f));
        mNavigationBarWidth.setOnSeekBarChangeListener(this);
    }

    private int defNavBarSize() {

        if (firstShot) {
            DisplayMetrics metrics = new DisplayMetrics();
            float mScreenSize = (getResources().getDisplayMetrics().density
                        * metrics.DENSITY_DEFAULT);

            preSizer = getResources().getDimensionPixelSize(R.dimen.navigation_bar_height_default);
            defNavBarSize = R.dimen.navigation_bar_height_default + (int)(preSizer * 1.5);
            minimumBarSize = (int)(mScreenSize * 0.08);

            if (mScreenSize >= metrics.DENSITY_XXHIGH) {
                defNavBarSize = preSizer + 60;
            } else if (mScreenSize < metrics.DENSITY_XXHIGH && mScreenSize >= metrics.DENSITY_XHIGH) {
                defNavBarSize = preSizer + 20;
            }
            firstShot = false;
        }
        return defNavBarSize;
    }

    private void setInitValue(int progress, SeekBar seekBar) {
        if (seekBar.getId() == R.id.navigation_bar_height) {
            HValue = progress;
        } else if (seekBar.getId() == R.id.navigation_bar_height_landscape) {
            LValue = progress;
        } else if (seekBar.getId() == R.id.navigation_bar_width) {
            WValue = progress;
        }
    }

    private int percentToPixels(int percent) {
        return (int)((float)defNavBarSize() * ((float) percent * 0.01f)); 
    }
}
