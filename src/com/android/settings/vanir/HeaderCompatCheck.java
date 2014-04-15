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
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.Vibrator;

import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.hardware.VibratorIntensity;

public class HeaderCompatCheck {

    private static boolean hasvib;
    private static boolean hascolors;
    private static boolean hasgamma;
    private static boolean hasdeviceprefs;
    private static boolean haslowram;
    private static boolean hasANYTHING;
    private static boolean wasinit;

    private static void init(Context context) {
        wasinit = true;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        hasvib = VibratorIntensity.isSupported() && vibrator != null && vibrator.hasVibrator();
        hascolors = DisplayColor.isSupported();
        hasgamma = DisplayColor.isSupported();
        hasdeviceprefs = true;
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo("com.cyanogenmod.settings.device",PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            hasdeviceprefs = false;
        }
        haslowram = ActivityManager.isLowRamDeviceStatic();
        hasANYTHING = (hasvib || hascolors || hasgamma || hasdeviceprefs || haslowram);
    }

    public static boolean hasCompatibility(Context context) {
        if (!wasinit) init(context);
        return hasANYTHING;
    }

    public static boolean hasVibratorIntensity(Context context) {
        if (!wasinit) init(context);
        return hasvib;
    }

    public static boolean modifiableDisplayColors(Context context) {
        if (!wasinit) init(context);
        return hascolors;
    }

    public static boolean modifiableDisplayGamma(Context context) {
        if (!wasinit) init(context);
        return hasgamma;
    }

    public static boolean isLowRam(Context context) {
        if (!wasinit) init(context);
        return haslowram;
    }

    private static boolean hasDevicePreferenceActivity(Context context) {
        if (!wasinit) init(context);
        return hasdeviceprefs;
    }
}
