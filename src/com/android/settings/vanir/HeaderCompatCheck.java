/*
 * Copyright (C) 2014 Vanir && The Android Open Source Project
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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.hardware.VibratorIntensity;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.cyanogenmod.MoreDeviceSettings;

public class HeaderCompatCheck {

    private static Context mContext;

    private static Vibrator vibrates;

    private static boolean vibrator;
    private static boolean compatibility = false;

    public HeaderCompatCheck(Context context) {
        mContext = context;
        vibrates = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        boolean vibrator = hasVibratorIntensity();
        boolean colors = modifiableDisplayColors();
        boolean gamma = modifiableDisplayGamma();
        boolean devicepreferences = hasDevicePreferenceActivity();
        boolean lowRam = isLowRam();

        if (vibrator || colors || gamma || devicepreferences || lowRam) compatibility = true;
    }

    public static boolean hasCompatibility() {
        if (compatibility) {
            return true;
        }
        return false;
    }

    public static boolean hasVibratorIntensity() {
        if (VibratorIntensity.isSupported() && vibrates != null && vibrates.hasVibrator()) {
            return true;
        }
        return false;
    }

    public static boolean modifiableDisplayColors() {
        return (DisplayColor.isSupported() ? true : false);
    }

    public static boolean modifiableDisplayGamma() {
        return (DisplayGamma.isSupported() ? true : false);
    }

    public static boolean isLowRam() {
        if (ActivityManager.isLowRamDeviceStatic()) {
            return true;
        }
        return false;
    }

    private static boolean hasDevicePreferenceActivity() {
        if(isPackageExisted("com.cyanogenmod.settings.device")) {
            return true;
        }
        return false;
    }

    private static boolean isPackageExisted(String targetPackage){
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }
}
