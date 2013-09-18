/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.vanir;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.UserHandle;

import com.android.settings.R;

public class KeyboardBacklightBrightness extends DialogPreference implements SeekBar.OnSeekBarChangeListener,
                                                                CheckBox.OnCheckedChangeListener {
    private static final String TAG = "KeyboardBacklight";

    private static final int DEFAULT_BUTTON_TIMEOUT = 5;

    private SeekBar mSeekBar;
    private CheckBox mCheckBox;
    private TextView mValue;
    private TextView mWarning;

    private SeekBar mTimeoutBar;
    private TextView mTimeoutValue;

    private static boolean mIsSupported;
    private static ContentResolver mResolver;
    private boolean mIsSingleValue;

    public KeyboardBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs);

        /* If the device has any a >0 default kb brightness, enable this */
        mIsSupported = context.getResources().getInteger(
                com.android.internal.R.integer.config_keyboardBrightnessSettingDefault) > 0;

        mResolver = context.getContentResolver();

        mIsSingleValue = !context.getResources().getBoolean(
                com.android.internal.R.bool.config_deviceHasVariableButtonBrightness);

        setDialogLayoutResource(R.layout.keyboard_backlight);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.auto_brightness_reset_button,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mTimeoutBar = (SeekBar) view.findViewById(R.id.timeout_seekbar);
        mTimeoutValue = (TextView) view.findViewById(R.id.timeout_value);
        mTimeoutBar.setMax(30);
        mTimeoutBar.setOnSeekBarChangeListener(this);
        mTimeoutBar.setProgress(getBacklightTimeout());

        if (mIsSingleValue) {
            view.findViewById(R.id.seekbar_container).setVisibility(View.GONE);
            mCheckBox = (CheckBox) view.findViewById(R.id.backlight_switch);
            mCheckBox.setOnCheckedChangeListener(this);
            mCheckBox.setChecked((getBacklightValue()!=0));
        } else {
            view.findViewById(R.id.checkbox_container).setVisibility(View.GONE);
            mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
            mValue = (TextView) view.findViewById(R.id.value);

            mSeekBar.setMax(255);
            mSeekBar.setOnSeekBarChangeListener(this);
            mSeekBar.setProgress(getBacklightValue());
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimeoutBar.setProgress(DEFAULT_BUTTON_TIMEOUT);
                if (mIsSingleValue) {
                    mCheckBox.setChecked(true);
                } else {
                    mSeekBar.setProgress(255);
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            putTimeoutValue(mTimeoutBar.getProgress());
            putBacklightValue(mIsSingleValue ?
                       (mCheckBox.isChecked() ? 255 : 0) : mSeekBar.getProgress());
        } else {
            putTimeoutValue(DEFAULT_BUTTON_TIMEOUT);
            putBacklightValue(255);
        }
    }

    public static boolean isSupported() {
        return mIsSupported;
    }

    public static int getBacklightValue() {
        return Settings.System.getIntForUser(mResolver,
                         Settings.System.KEYBOARD_BRIGHTNESS, 255,
                         UserHandle.USER_CURRENT);
    }

    public static void putBacklightValue(int value) {
        Settings.System.putIntForUser(mResolver,
                         Settings.System.KEYBOARD_BRIGHTNESS, value,
                         UserHandle.USER_CURRENT);
    }

    public static int getBacklightTimeout() {
        return Settings.System.getIntForUser(mResolver,
                         Settings.System.BUTTON_BACKLIGHT_TIMEOUT, (DEFAULT_BUTTON_TIMEOUT * 1000),
                         UserHandle.USER_CURRENT) / 1000;
    }

    public static void putTimeoutValue(int value) {
        int timeout = value * 1000;
        Settings.System.putIntForUser(mResolver,
                         Settings.System.BUTTON_BACKLIGHT_TIMEOUT, timeout,
                         UserHandle.USER_CURRENT);
    }

    /* Behaviors when it's a seekbar */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mTimeoutBar != null && seekBar == mTimeoutBar) {
            putTimeoutValue(progress);
            mTimeoutValue.setText(String.format("%d", progress));
        } else {
            putBacklightValue(progress);
            mValue.setText(String.format("%d%%", (int)((progress * 100) / 255)));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    /* Behaviors when it's a plain checkbox */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        putBacklightValue(isChecked ? 255 : 0);
    }

}
