package com.android.settings.vanir.navbar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.util.aokp.AwesomeConstants.AwesomeConstant;
import com.android.settings.R;
import com.android.settings.util.HardwareKeyNavbarHelper;
import com.vanir.util.DeviceUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class NavbarSettingsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = NavbarSettingsFragment.class.getSimpleName();
    private static final String SHARED_PREFS_FILE = "shared_prefs_file";
    private static final String SHARED_PREFS_VALUES = "shared_prefs_values";

    private StatusBarManager mStatusBar;

    private SeekBar mNavigationBarHeight;
    private SeekBar mNavigationBarHeightLandscape;
    private SeekBar mNavigationBarWidth;
    private TextView mBarHeightValue;
    private TextView mBarHeightLandscapeValue;
    private TextView mBarWidthValue;
    private CheckBox mSideKeys;
    private CheckBox mArrows;

    private Switch mEnabledSwitch;

    private static int HValue;
    private static int LValue;
    private static int WValue;
    private static int mDefaultHeight;
    private static int mDefaultHeightLandscape;
    private static int mDefaultWidth;

    int MIN_HEIGHT_PERCENT;
    int MIN_WIDTH_PERCENT;

    boolean imebutton;
    boolean homebutton;
    boolean blankspace;
    CharSequence[] items;
    ArrayList selectedItems = new ArrayList();

    private static final HashMap<Integer, String> IME_LAYOUT = new HashMap<Integer, String>() {{
        put(1,"**back**,,,");
        put(2,"**ime**,,,");
        put(3,"**home**,,,");
        put(4,"**blank**,,,");
        put(5,"**arrow_left**,,,");
        put(6,"**arrow_up**,,,");
        put(7,"**arrow_down**,,,");
        put(8,"**arrow_right**,,,");
    }};

    private Handler mHandler = new Handler();
    private SettingsObserver mSettingsObserver;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.ENABLE_NAVIGATION_BAR), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            final ContentResolver resolver = getActivity().getContentResolver();

            boolean enabled = Settings.System.getInt(resolver,
                         Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1;
            mEnabledSwitch.setChecked(enabled);
        }
    }

    public NavbarSettingsFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (HardwareKeyNavbarHelper.shouldShowNavbarToggle()) {
            final Activity activity = getActivity();
            mEnabledSwitch = new Switch(activity);
            final int padding = activity.getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
            mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
            mEnabledSwitch.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (HardwareKeyNavbarHelper.shouldShowNavbarToggle()) {
            final Activity activity = getActivity();
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
            mEnabledSwitch.setChecked((Settings.System.getInt(activity.getContentResolver(),
                Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (HardwareKeyNavbarHelper.shouldShowNavbarToggle()) {
            final Activity activity = getActivity();
            activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources res = getActivity().getResources();

        items = new CharSequence[3];
        items[0] = res.getString(R.string.ime_layout_ime);
        items[1] = res.getString(R.string.ime_layout_home);
        items[2] = res.getString(R.string.ime_layout_blank);

        mStatusBar = (StatusBarManager) getActivity().getSystemService(Context.STATUS_BAR_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (HardwareKeyNavbarHelper.shouldShowNavbarToggle()) {
            if (mSettingsObserver == null) {
                mSettingsObserver = new SettingsObserver(mHandler);
                mSettingsObserver.observe();
            }
        }
        assignCheckBoxState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSettingsObserver != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mSettingsObserver);
            mSettingsObserver = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_navbar_settings, container, false);

        final Activity activity = getActivity();
        final Resources res = activity.getResources();
        final ContentResolver cr = activity.getContentResolver();

        mDefaultHeight = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
        mDefaultHeightLandscape = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height_landscape);
        mDefaultWidth = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_width);

        final int MAX_HEIGHT_PERCENT = res.getInteger(R.integer.navigation_bar_height_max_percent);
        final int MAX_WIDTH_PERCENT = res.getInteger(R.integer.navigation_bar_width_max_percent);
        MIN_HEIGHT_PERCENT = res.getInteger(R.integer.navigation_bar_height_min_percent);
        MIN_WIDTH_PERCENT = res.getInteger(R.integer.navigation_bar_width_min_percent);
        final double MAX_WIDTH_SCALAR = MAX_WIDTH_PERCENT/100.0;
        final double MIN_WIDTH_SCALAR = MIN_WIDTH_PERCENT/100.0;
        final double MAX_HEIGHT_SCALAR = MAX_HEIGHT_PERCENT/100.0;
        final double MIN_HEIGHT_SCALAR = MIN_HEIGHT_PERCENT/100.0;

        // load user settings
        HValue = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_HEIGHT, mDefaultHeight);
        LValue = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, mDefaultHeightLandscape);
        WValue = Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_WIDTH, mDefaultWidth);

        // load previous bar states
        SharedPreferences prefs = activity.getSharedPreferences("last_slider_values", Context.MODE_PRIVATE);
        final int currentHeightPercent = prefs.getInt("heightPercent",
                (int)(100.0 * ( HValue - MIN_HEIGHT_SCALAR * mDefaultHeight) /
                ( MAX_HEIGHT_SCALAR * mDefaultHeight - MIN_HEIGHT_SCALAR * mDefaultHeight )));
        final int currentHeightLandscapePercent = prefs.getInt("heightLandscapePercent",
                (int)(100.0 * ( LValue - MIN_HEIGHT_SCALAR * mDefaultHeightLandscape) /
                ( MAX_HEIGHT_SCALAR * mDefaultHeightLandscape - MIN_HEIGHT_SCALAR * mDefaultHeightLandscape )));
        final int currentWidthPercent = prefs.getInt("widthPercent",
                (int)(100.0 * ( WValue - MIN_WIDTH_SCALAR * mDefaultWidth) /
                ( MAX_WIDTH_SCALAR * mDefaultWidth - MIN_WIDTH_SCALAR * mDefaultWidth )));

        // Navbar height
        mNavigationBarHeight = (SeekBar) v.findViewById(R.id.navigation_bar_height);
        mBarHeightValue = (TextView) v.findViewById(R.id.navigation_bar_height_value);
        mNavigationBarHeight.setMax(MAX_HEIGHT_PERCENT - MIN_HEIGHT_PERCENT);
        mNavigationBarHeight.setProgress(currentHeightPercent);
        mBarHeightValue.setText(String.valueOf(currentHeightPercent + MIN_HEIGHT_PERCENT)+"%");
        mNavigationBarHeight.setOnSeekBarChangeListener(this);

        // Navbar height landscape seekbar (tablets only)
        mNavigationBarHeightLandscape = (SeekBar) v.findViewById(R.id.navigation_bar_height_landscape);
        mBarHeightLandscapeValue = (TextView) v.findViewById(R.id.navigation_bar_height_landscape_value);
        mNavigationBarHeightLandscape.setMax(MAX_HEIGHT_PERCENT - MIN_HEIGHT_PERCENT);
        mNavigationBarHeightLandscape.setProgress(currentHeightLandscapePercent);
        mBarHeightLandscapeValue.setText(String.valueOf(currentHeightLandscapePercent + MIN_HEIGHT_PERCENT)+"%");
        mNavigationBarHeightLandscape.setOnSeekBarChangeListener(this);

        // Navbar width (phones only)
        mNavigationBarWidth = (SeekBar) v.findViewById(R.id.navigation_bar_width);
        mBarWidthValue = (TextView) v.findViewById(R.id.navigation_bar_width_value);
        mNavigationBarWidth.setMax(MAX_WIDTH_PERCENT - MIN_WIDTH_PERCENT);
        mNavigationBarWidth.setProgress(currentWidthPercent);
        mBarWidthValue.setText(String.valueOf(currentWidthPercent + MIN_WIDTH_PERCENT)+"%");
        mNavigationBarWidth.setOnSeekBarChangeListener(this);

        // Legacy side menu keys
        mSideKeys = (CheckBox) v.findViewById(R.id.sidekey_checkbox);
        mSideKeys.setChecked(Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_SIDEKEYS, 1) == 1);
        mSideKeys.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((CheckBox) v).isChecked();
                Settings.System.putInt(cr, Settings.System.NAVIGATION_BAR_SIDEKEYS, isChecked ? 1 : 0);
            }
        });

        // Custom IME key layout
        mArrows = (CheckBox) v.findViewById(R.id.arrows_checkbox);
        mArrows.setChecked(Settings.System.getInt(cr, Settings.System.NAVIGATION_BAR_ARROWS, 0) == 1);
        mArrows.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((CheckBox) v).isChecked();
                modifyIMELayoutDialog(isChecked);
                Settings.System.putInt(cr, Settings.System.NAVIGATION_BAR_ARROWS, isChecked ? 1 : 0);
            }
        });

        if (DeviceUtils.isPhone(activity)) {
            v.findViewById(R.id.navigation_bar_height_landscape_text).setVisibility(View.GONE);
            mBarHeightLandscapeValue.setVisibility(View.GONE);
            mNavigationBarHeightLandscape.setVisibility(View.GONE);
        } else {
            v.findViewById(R.id.navigation_bar_width_text).setVisibility(View.GONE);
            mBarWidthValue.setVisibility(View.GONE);
            mNavigationBarWidth.setVisibility(View.GONE);
        }
        
        return v;
    }

    private void modifyIMELayoutDialog(boolean optionEnabled) {
        Activity activity = getActivity();

        if (optionEnabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(getString(R.string.customize_ime_layout_dialog_title));
            builder.setMultiChoiceItems(items, null,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                            if (isChecked) {
                                selectedItems.add(indexSelected);
                            } else if (selectedItems.contains(indexSelected)) {
                                selectedItems.remove(Integer.valueOf(indexSelected));
                            }
                            assignCheckBoxState();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            new updateLayoutAsyncTask().execute("");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
      
            AlertDialog dialog = builder.create(); //AlertDialog dialog; create like this outside onClick
            dialog.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                }
            });
            dialog.show();
        }
    }

    private void assignCheckBoxState() {
        imebutton = selectedItems.contains(0);
        homebutton = selectedItems.contains(1);
        blankspace = selectedItems.contains(2);
    }

    private void assignIMELayout() {
        StringBuilder mEtallica = new StringBuilder();
        String delimiter = "|";

        for (Integer button : IME_LAYOUT.keySet()) {
            switch (button) {
                case 2: // back
                    if (!imebutton) continue;
                    break;
                case 3: // home
                    if (!homebutton) continue;
                    break;
                case 4: // space
                    if (!blankspace) continue;
                    break;
                case 8: // last iteration
                    delimiter = "";
                    break;
            }

            mEtallica.append(IME_LAYOUT.get(button));
            mEtallica.append(delimiter);
        }

        Settings.System.putString(getActivity().getContentResolver(),
                Settings.System.NAVIGATION_IME_LAYOUT, mEtallica.toString());
    }

    private class updateLayoutAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            assignIMELayout();
            return null;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch) {
            mEnabledSwitch.setEnabled(false);
            HardwareKeyNavbarHelper.writeEnableNavbarOption(getActivity(), mEnabledSwitch.isChecked());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mEnabledSwitch.setEnabled(true);
                }
            }, 1000);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int rawprogress, boolean fromUser) {
        ContentResolver cr = getActivity().getContentResolver();
        double proportion = 1.0;

        if (fromUser) {
            if (seekbar == mNavigationBarWidth) {
                final int progress = rawprogress + MIN_WIDTH_PERCENT;
                proportion = ((double)progress/100.0);
                mBarWidthValue.setText(String.valueOf(progress)+"%");
                WValue = (int)(proportion*mDefaultWidth);
                Settings.System.putInt(cr,
                        Settings.System.NAVIGATION_BAR_WIDTH, WValue);

            } else if (seekbar == mNavigationBarHeight) {
                final int progress = rawprogress + MIN_HEIGHT_PERCENT;
                proportion = ((double)progress/100.0);
                mBarHeightValue.setText(String.valueOf(progress)+"%");
                HValue = (int)(proportion*mDefaultHeight);
                Settings.System.putInt(cr,
                        Settings.System.NAVIGATION_BAR_HEIGHT, HValue);

            } else if (seekbar == mNavigationBarHeightLandscape) {
                final int progress = rawprogress + MIN_HEIGHT_PERCENT;
                proportion = ((double)progress/100.0);
                mBarHeightLandscapeValue.setText(String.valueOf(progress)+"%");
                LValue = (int)(proportion*mDefaultHeightLandscape);
                Settings.System.putInt(cr,
                        Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE, LValue);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        SharedPreferences prefs = getActivity().getSharedPreferences("last_slider_values", Context.MODE_PRIVATE);
        prefs.edit().putInt("heightPercent", mNavigationBarHeight.getProgress())
                    .putInt("heightLandscapePercent", mNavigationBarHeightLandscape.getProgress())
                    .putInt("widthPercent", mNavigationBarWidth.getProgress()).commit();
    }
}
