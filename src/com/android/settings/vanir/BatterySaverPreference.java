/*
 * Copyright (C) 2014 VanirAOSP && The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.vanir.BatterySaverHelper;

public class BatterySaverPreference extends Preference implements OnCheckedChangeListener {

    private Switch mSwitch;
    private static final String TAG = "BatterySaverPreference";

    public BatterySaverPreference(Context context) {
        super(context);
    }

    public BatterySaverPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BatterySaverPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSwitch = (Switch) view.findViewById(R.id.mswitch);
        mSwitch.setChecked(Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.BATTERY_SAVER_OPTION, 1) != 0);
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Context context = this.getContext();

        Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.BATTERY_SAVER_OPTION, isChecked ? 1 : 0);

        BatterySaverHelper.setBatterySaverActive(context, isChecked ? 1 : 0);
        BatterySaverHelper.scheduleService(context);

        Log.i(TAG, String.valueOf("PowerSaverService Status: " + String.valueOf(isChecked)));
    }

}
