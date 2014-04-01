/*
 * Copyright (C) 2014 VanirAOSP
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

package com.android.settings.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.UserHandle;
import android.os.RemoteException;
import android.provider.Settings;

import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import org.cyanogenmod.hardware.KeyDisabler;

public class HardwareKeyNavbarHelper {

    public static boolean shouldShowNavbarToggle() {
        try {
            final IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            return !wm.needsNavigationBar();
        } catch (RemoteException e) {
        }
        return false;
    }

    public static void restoreKeyDisabler(Context context) {
        writeDisableNavkeysOption(context, Settings.System.getInt(context.getContentResolver(),
                Settings.System.ENABLE_NAVIGATION_BAR, 0) != 0);
    }

    public static void writeDisableNavkeysOption(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.ENABLE_NAVIGATION_BAR, enabled ? 1 : 0);
        if (!KeyDisabler.isSupported()) {
            return;
        }

        final SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("previous_button_baclkight_values", Context.MODE_PRIVATE);
        final int defaultBrightness = context.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

        /* Save/restore button timeouts to disable them in softkey mode */
        Editor editor = prefs.edit();

        if (enabled) {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, defaultBrightness);
            if (!prefs.contains("pre_navbar_button_backlight")) {
                editor.putInt("pre_navbar_button_backlight", currentBrightness);
            }
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, 0);
        } else {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS,
                    prefs.getInt("pre_navbar_button_backlight", defaultBrightness));
            editor.remove("pre_navbar_button_backlight");
        }
        editor.commit();
    }
}
