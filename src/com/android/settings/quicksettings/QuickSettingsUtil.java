/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.quicksettings;

import static com.android.internal.util.cm.QSConstants.*;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.util.cm.QSUtils;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.android.settings.util.HardwareKeyNavbarHelper;

public class QuickSettingsUtil {
    private static final String TAG = "QuickSettingsUtil";

    public static final Map<String, TileInfo> TILES;

    private static final Map<String, TileInfo> ENABLED_TILES = new HashMap<String, TileInfo>();
    private static final Map<String, TileInfo> DISABLED_TILES = new HashMap<String, TileInfo>();

    static {
        TILES = Collections.unmodifiableMap(ENABLED_TILES);
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_AIRPLANE, R.string.title_tile_airplane,
                "com.android.systemui:drawable/ic_qs_airplane_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_BATTERY, R.string.title_tile_battery,
                "com.android.systemui:drawable/ic_qs_battery_neutral"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_BLUETOOTH, R.string.title_tile_bluetooth,
                "com.android.systemui:drawable/ic_qs_bluetooth_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_BRIGHTNESS, R.string.title_tile_brightness,
                "com.android.systemui:drawable/ic_qs_brightness_auto_off"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_CAMERA, R.string.title_tile_camera,
                "com.android.systemui:drawable/ic_qs_camera"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_IMMERSIVE, R.string.title_tile_immersive_desktop,
                "com.android.systemui:drawable/ic_qs_immersive_desktop_tile"));
        registerTile(new QuickSettingsUtil.TileInfo( 
                TILE_COMPASS, R.string.title_tile_compass,
                "com.android.systemui:drawable/ic_qs_compass_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_HEADS_UP, R.string.title_tile_heads_up,
                "com.android.systemui:drawable/ic_qs_heads_up_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SLEEP, R.string.title_tile_sleep,
                "com.android.systemui:drawable/ic_qs_sleep"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_GPS, R.string.title_tile_gps,
                "com.android.systemui:drawable/ic_qs_location_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_LOCKSCREEN, R.string.title_tile_lockscreen,
                "com.android.systemui:drawable/ic_qs_lock_screen_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_LTE, R.string.title_tile_lte,
                "com.android.systemui:drawable/ic_qs_lte_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_MOBILEDATA, R.string.title_tile_mobiledata,
                "com.android.systemui:drawable/ic_qs_signal_full_4"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NETWORKMODE, R.string.title_tile_networkmode,
                "com.android.systemui:drawable/ic_qs_2g3g_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NFC, R.string.title_tile_nfc,
                "com.android.systemui:drawable/ic_qs_nfc_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_AUTOROTATE, R.string.title_tile_autorotate,
                "com.android.systemui:drawable/ic_qs_auto_rotate"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_PROFILE, R.string.title_tile_profile,
                "com.android.systemui:drawable/ic_qs_profiles"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_PERFORMANCE_PROFILE, R.string.title_tile_performance_profile,
                "com.android.systemui:drawable/ic_qs_perf_profile"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_QUIETHOURS, R.string.title_tile_quiet_hours,
                "com.android.systemui:drawable/ic_qs_quiet_hours_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SCREENTIMEOUT, R.string.title_tile_screen_timeout,
                "com.android.systemui:drawable/ic_qs_screen_timeout_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SETTINGS, R.string.title_tile_settings,
                "com.android.systemui:drawable/ic_qs_settings"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_RINGER, R.string.title_tile_sound,
                "com.android.systemui:drawable/ic_qs_ring_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_SYNC, R.string.title_tile_sync,
                "com.android.systemui:drawable/ic_qs_sync_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_TORCH, R.string.title_tile_torch,
                "com.android.systemui:drawable/ic_qs_torch_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_USER, R.string.title_tile_user,
                "com.android.systemui:drawable/ic_qs_default_user"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_VOLUME, R.string.title_tile_volume,
                "com.android.systemui:drawable/ic_qs_volume"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_WIFI, R.string.title_tile_wifi,
                "com.android.systemui:drawable/ic_qs_wifi_full_4"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_WIFIAP, R.string.title_tile_wifiap,
                "com.android.systemui:drawable/ic_qs_wifi_ap_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_MUSIC, R.string.title_tile_music,
                "com.android.systemui:drawable/ic_qs_media_play"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NETWORKADB, R.string.title_tile_network_adb,
                "com.android.systemui:drawable/ic_qs_network_adb_off"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_QUICKRECORD, R.string.title_tile_quick_record,
                "com.android.systemui:drawable/ic_qs_quickrecord"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_ONTHEGO, R.string.title_tile_onthego,
                "com.android.systemui:drawable/ic_qs_onthego"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_POWERMENU, R.string.title_tile_powermenu,
                "com.android.systemui:drawable/ic_qs_powermenu"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_UPDATE, R.string.title_tile_update,
                "com.android.systemui:drawable/ic_qs_update"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_NAVBAR, R.string.title_navbar_tile,
                "com.android.systemui:drawable/ic_qs_navbar_on"));
        registerTile(new QuickSettingsUtil.TileInfo(
                TILE_GESTUREPANEL, R.string.title_gesturepanel_tile,
                "com.android.systemui:drawable/ic_qs_gesture"));
    }

    private static void registerTile(QuickSettingsUtil.TileInfo info) {
        ENABLED_TILES.put(info.getId(), info);
    }

    private static void removeTile(String id) {
        ENABLED_TILES.remove(id);
        DISABLED_TILES.remove(id);
        TILES_DEFAULT.remove(id);
    }

    private static void disableTile(String id) {
        if (ENABLED_TILES.containsKey(id)) {
            DISABLED_TILES.put(id, ENABLED_TILES.remove(id));
        }
    }

    private static void enableTile(String id) {
        if (DISABLED_TILES.containsKey(id)) {
            ENABLED_TILES.put(id, DISABLED_TILES.remove(id));
        }
    }

    protected static synchronized void removeUnsupportedTiles(Context context) {
        // Don't show mobile data options if not supported
        if (!QSUtils.deviceSupportsMobileData(context)) {
            removeTile(TILE_MOBILEDATA);
            removeTile(TILE_WIFIAP);
            removeTile(TILE_NETWORKMODE);
        }

        // Don't show the bluetooth options if not supported
        if (!QSUtils.deviceSupportsBluetooth()) {
            removeTile(TILE_BLUETOOTH);
        }

        // Don't show the NFC tile if not supported
        if (!QSUtils.deviceSupportsNfc(context)) {
            removeTile(TILE_NFC);
        }

        // Don't show the LTE tile if not supported
        if (!QSUtils.deviceSupportsLte(context)) {
            removeTile(TILE_LTE);
        }

        // Don't show the Torch tile if not supported
        if (!QSUtils.deviceSupportsTorch(context)) {
            removeTile(TILE_TORCH);
        }

        // Don't show the Camera tile if the device has no cameras
        if (!QSUtils.deviceSupportsCamera()) {
            removeTile(TILE_CAMERA);
        }

        // Don't show the performance profiles tile if is not available for the device
        if (!QSUtils.deviceSupportsPerformanceProfiles(context)) {
            removeTile(TILE_PERFORMANCE_PROFILE);
        }

        // Don't show the Compass tile if the device has no orientation sensor
        if (!QSUtils.deviceSupportsCompass(context)) {
            removeTile(TILE_COMPASS);
        }

        // Don't show the navbar tile on devices that really have a navbar
        if (!HardwareKeyNavbarHelper.shouldShowNavbarToggle()) {
            removeTile(TILE_NAVBAR);
        }
        // Don't show the updater tile on devices that not have vanirupdator
        if (!QSUtils.isOnline()) {
            removeTile(TILE_UPDATE);
        }
    }

    private static synchronized void refreshAvailableTiles(Context context) {
        ContentResolver resolver = context.getContentResolver();

        // Some phones run on networks not supported by the networkmode tile,
        // so make it available only where supported
        int networkState = -99;
        try {
            networkState = Settings.Global.getInt(resolver,
                    Settings.Global.PREFERRED_NETWORK_MODE);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Unable to retrieve PREFERRED_NETWORK_MODE", e);
        }

        switch (networkState) {
            // list of supported network modes
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_GSM_ONLY:
                enableTile(TILE_NETWORKMODE);
                break;
            default:
                disableTile(TILE_NETWORKMODE);
                break;
        }

        // Don't show the profiles tile if profiles are disabled
        if (QSUtils.systemProfilesEnabled(resolver)) {
            enableTile(TILE_PROFILE);
        } else {
            disableTile(TILE_PROFILE);
        }

        // Don't show the Expanded desktop tile if expanded desktop is disabled
        // disabled until i write a simpler method to dynamically remove tiles instead of redoing them all
//        if (QSUtils.immersiveDesktopEnabled(resolver) != 0) {
//            enableTile(TILE_IMMERSIVE);
//        } else {
//            disableTile(TILE_IMMERSIVE);
//        }

        // Don't show the Network ADB tile if adb debugging is disabled
        if (QSUtils.adbEnabled(resolver)) {
            enableTile(TILE_NETWORKADB);
        } else {
            disableTile(TILE_NETWORKADB);
        }
    }

    public static synchronized void updateAvailableTiles(Context context) {
        removeUnsupportedTiles(context);
        refreshAvailableTiles(context);
    }

    public static boolean isTileAvailable(String id) {
        return ENABLED_TILES.containsKey(id);
    }

    public static String getCurrentTiles(Context context, boolean isRibbon) {
        String tiles = Settings.System.getString(context.getContentResolver(),
                isRibbon ? Settings.System.QUICK_SETTINGS_RIBBON_TILES
                         : Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            tiles = getDefaultTiles(context);
        }
        return tiles;
    }

    public static void saveCurrentTiles(Context context, String tiles, boolean isRibbon) {
        Settings.System.putString(context.getContentResolver(),
                isRibbon ? Settings.System.QUICK_SETTINGS_RIBBON_TILES
                         : Settings.System.QUICK_SETTINGS_TILES, tiles);
    }

    public static void resetTiles(Context context, boolean isRibbon) {
        String defaultTiles = getDefaultTiles(context);
        Settings.System.putString(context.getContentResolver(),
                isRibbon ? Settings.System.QUICK_SETTINGS_RIBBON_TILES
                         : Settings.System.QUICK_SETTINGS_TILES, defaultTiles);
    }

    public static String mergeInNewTileString(String oldString, String newString) {
        ArrayList<String> oldList = getTileListFromString(oldString);
        ArrayList<String> newList = getTileListFromString(newString);
        ArrayList<String> mergedList = new ArrayList<String>();

        // add any items from oldlist that are in new list
        for (String tile : oldList) {
            if (newList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // append anything in newlist that isn't already in the merged list to
        // the end of the list
        for (String tile : newList) {
            if (!mergedList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // return merged list
        return getTileStringFromList(mergedList);
    }

    public static ArrayList<String> getTileListFromString(String tiles) {
        return new ArrayList<String>(Arrays.asList(tiles.split("\\|")));
    }

    public static String getTileStringFromList(ArrayList<String> tiles) {
        if (tiles == null || tiles.size() <= 0) {
            return "";
        } else {
            String s = tiles.get(0);
            for (int i = 1; i < tiles.size(); i++) {
                s += TILE_DELIMITER + tiles.get(i);
            }
            return s;
        }
    }

    public static String getDefaultTiles(Context context) {
        removeUnsupportedTiles(context);
        return TextUtils.join(TILE_DELIMITER, TILES_DEFAULT);
    }

    public static class TileInfo {
        private String mId;
        private int mTitleResId;
        private String mIcon;

        public TileInfo(String id, int titleResId, String icon) {
            mId = id;
            mTitleResId = titleResId;
            mIcon = icon;
        }

        public String getId() {
            return mId;
        }

        public int getTitleResId() {
            return mTitleResId;
        }

        public String getIcon() {
            return mIcon;
        }
    }
}
