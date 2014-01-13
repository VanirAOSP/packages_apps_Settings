/*
 * Copyright (C) 2014 The OmniROM Project
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.provider.Settings;

import java.util.Calendar;

public class BatterySaverHelper {

    private final static String TAG = "BatterySaverHelper";

    private static final String SCHEDULE_BATTERY_SAVER =
            "com.android.settings.vanir.SCHEDULE_BATTERY_SAVER";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day

    // Pending intent to start/stop service
    private static PendingIntent makeServiceIntent(Context context,
            String action, int requestCode) {
        Intent intent = new Intent(context, BatterySaverReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static boolean deviceSupportsMobileData(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static void scheduleService(Context context) {
        boolean batterySaverEnabled = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.BATTERY_SAVER_OPTION, 0) != 0;
        int batterySaverStart = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.BATTERY_SAVER_START, 0);
        int batterySaverEnd = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.BATTERY_SAVER_END, 0);
        Intent serviceTriggerIntent = (new Intent())
                   .setClassName("com.android.systemui", "com.android.systemui.batterysaver.BatterySaverService");
        PendingIntent startIntent = makeServiceIntent(context, SCHEDULE_BATTERY_SAVER, 1);
        PendingIntent stopIntent = makeServiceIntent(context, SCHEDULE_BATTERY_SAVER, 2);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        am.cancel(startIntent);
        am.cancel(stopIntent);

        if (!batterySaverEnabled) {
            context.stopService(serviceTriggerIntent);
            return;
        }

        if (batterySaverStart == batterySaverEnd) {
            // 24 hours, start without stop
            context.startService(serviceTriggerIntent);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        boolean inBatterySaver = false;
        // time from now on (in minutes) when the service start/stop should be scheduled
        int serviceStartMinutes = -1, serviceStopMinutes = -1;

        if (batterySaverEnd < batterySaverStart) {
            // Starts at night, ends in the morning.
            if (currentMinutes >= batterySaverStart) {
                inBatterySaver = true;
                serviceStopMinutes = FULL_DAY - currentMinutes + batterySaverEnd;
            } else if (currentMinutes <= batterySaverEnd) {
                inBatterySaver = true;
                serviceStopMinutes = batterySaverEnd - currentMinutes;
            } else {
                inBatterySaver = false;
                serviceStartMinutes = batterySaverStart - currentMinutes;
                serviceStopMinutes = FULL_DAY - currentMinutes + batterySaverEnd;
            }
        } else {
            // Starts in the morning, ends at night.
            if (currentMinutes >= batterySaverStart && currentMinutes <= batterySaverEnd) {
                inBatterySaver = true;
                serviceStopMinutes = batterySaverEnd - currentMinutes;
            } else {
                inBatterySaver = false;
                if (currentMinutes <= batterySaverStart) {
                    serviceStartMinutes = batterySaverStart - currentMinutes;
                    serviceStopMinutes = batterySaverEnd - currentMinutes;
                } else {
                    serviceStartMinutes = FULL_DAY - currentMinutes + batterySaverStart;
                    serviceStopMinutes = FULL_DAY - currentMinutes + batterySaverEnd;
                }
            }
        }

        if (inBatterySaver) {
            context.startService(serviceTriggerIntent);
        } else {
            context.stopService(serviceTriggerIntent);
        }

        if (serviceStartMinutes >= 0) {
            // Start service a minute early
            serviceStartMinutes--;
            calendar.add(Calendar.MINUTE, serviceStartMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), startIntent);
            calendar.add(Calendar.MINUTE, -serviceStartMinutes);
        }

        if (serviceStopMinutes >= 0) {
            // Stop service a minute late
            serviceStopMinutes++;
            calendar.add(Calendar.MINUTE, serviceStopMinutes);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), stopIntent);
            calendar.add(Calendar.MINUTE, -serviceStopMinutes);
        }
    }
}
