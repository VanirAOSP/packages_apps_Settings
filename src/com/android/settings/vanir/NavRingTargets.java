/*
 * Copyright (C) 2013 Android Open Kang Project
 * Copyright (C) 2013 The Cyanogenmod Project
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

package com.android.settings.vanir;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import com.android.internal.util.vanir.AwesomeConstants;
import com.android.internal.util.aokp.NavRingHelpers;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.internal.widget.multiwaveview.TargetDrawable;
import com.android.settings.R;
import com.android.settings.util.ShortcutPickerHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import static com.android.internal.util.vanir.AwesomeConstants.ASSIST_ICON_METADATA_NAME;
import static com.android.internal.util.vanir.AwesomeConstants.AwesomeConstant;

public class NavRingTargets extends Fragment implements GlowPadView.OnTriggerListener {
    private static final String TAG = "NavRing";
    private static final boolean DEBUG = true;

    public static final int REQUEST_PICK_CUSTOM_ICON = 200;
    public static final int REQUEST_PICK_LANDSCAPE_ICON = 201;

    private GlowPadView mGlowPadView;
    private Spinner mTargetNumAmount;
    private Switch mLongPressStatus;

    private ShortcutPickerHelper mPicker;
    private String[] targetActivities = new String[5];
    private String[] longActivities = new String[5];
    private String[] customIcons = new String[5];
    private ViewGroup mContainer;

    private int mTargetIndex = 0;
    private int startPosOffset;
    private int endPosOffset;
    private int mNavRingAmount;
    private boolean mBoolLongPress;

    private enum E_Action {
        T_SHORT,
        T_LONG,
        T_ICON
    };
    private static final E_Action[] navring_noicon_dialog_values = new E_Action[] { E_Action.T_SHORT };
    private static final E_Action[] navring_noicon_withlong_dialog_values = new E_Action[] { E_Action.T_SHORT, E_Action.T_LONG };
    private static final E_Action[] navring_dialog_values = new E_Action[] { E_Action.T_SHORT, E_Action.T_ICON };
    private static final E_Action[] navring_withlong_dialog_values = new E_Action[] { E_Action.T_SHORT, E_Action.T_LONG, E_Action.T_ICON };

    //for onActivityResult handling
    private NavRingClickerer mCurrentClickerer;

    private ArrayList<Integer> intentList = new ArrayList<Integer>();
    private int intentCounter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        return inflater.inflate(R.layout.navigation_ring_targets, container,
                false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGlowPadView = ((GlowPadView) getActivity().findViewById(
                R.id.navring_target));
        mGlowPadView.setOnTriggerListener(this);

        mTargetNumAmount = (Spinner) getActivity().findViewById(
                R.id.amount_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<CharSequence>(
                getActivity(), android.R.layout.simple_spinner_item);
        spinnerAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final String[] entries = getResources().getStringArray(
                R.array.pref_navring_amount_entries);
        for (int i = 0; i < entries.length; i++) {
            spinnerAdapter.add(entries[i]);
        }
        mTargetNumAmount.setAdapter(spinnerAdapter);

        mLongPressStatus = (Switch) getActivity().findViewById(
                R.id.longpress_switch);

        mBoolLongPress = (Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.SYSTEMUI_NAVRING_LONG_ENABLE, false));
        mNavRingAmount = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEMUI_NAVRING_AMOUNT, 1);

        mTargetNumAmount.setSelection(mNavRingAmount - 1);
        mLongPressStatus.setChecked(mBoolLongPress);

        mTargetNumAmount.post(new Runnable() {
            public void run() {
                mTargetNumAmount
                        .setOnItemSelectedListener(new AmountListener());
            }
        });
        mLongPressStatus
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton v,
                            boolean checked) {
                        Settings.System.putBoolean(getActivity().getContentResolver(),
                                Settings.System.SYSTEMUI_NAVRING_LONG_ENABLE,
                                checked);
                        mBoolLongPress = checked;
                    }
                });

        for (int i = 0; i < 5; i++) {
            targetActivities[i] = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING[i]);
            longActivities[i] = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING_LONG[i]);
            customIcons[i] = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING_ICON[i]);
        }

        setDrawables();
    }

    public class AmountListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                long id) {
            final String[] values = getResources().getStringArray(
                    R.array.pref_navring_amount_values);
            mNavRingAmount = Integer.parseInt((String) values[pos]);
            Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING_AMOUNT, mNavRingAmount);
            setDrawables();
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    private void setDrawables() {
        intentCounter = 0;
        intentList.clear();

        // Custom Targets
        ArrayList<TargetDrawable> storedDraw = new ArrayList<TargetDrawable>();

        int endPosOffset = 0;
        int middleBlanks = 0;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { // NavRing on Bottom
            startPosOffset = 1;
            endPosOffset = (mNavRingAmount) + 1;

        } else { // Right... (Ring actually on left side of tablet)
            startPosOffset = (Math.min(1, mNavRingAmount / 2)) + 2;
            endPosOffset = startPosOffset - 1;
        }

        int middleStart = mNavRingAmount;
        int tqty = middleStart;
        int middleFinish = 0;

        if (middleBlanks > 0) {
            middleStart = (tqty / 2) + (tqty % 2);
            middleFinish = (tqty / 2);
        }

        // Add Initial Place Holder Targets
        for (int i = 0; i < startPosOffset; i++) {
            intentList.add(-1);
            storedDraw.add(NavRingHelpers.getTargetDrawable(getActivity(), null));
        }
        // Add User Targets
        for (int i = 0; i < middleStart; i++) {
            TargetDrawable drawable;
            if (!TextUtils.isEmpty(customIcons[i])) {
                drawable = NavRingHelpers.getCustomDrawable(getActivity(),
                        customIcons[i]);
            } else {
                drawable = NavRingHelpers.getTargetDrawable(getActivity(),
                        targetActivities[i]);
            }
            drawable.setEnabled(true);
            storedDraw.add(drawable);
            intentList.add(intentCounter);
            intentCounter = intentCounter + 1;
        }

        // Add middle Place Holder Targets
        for (int j = 0; j < middleBlanks; j++) {
            intentList.add(-1);
            storedDraw.add(NavRingHelpers.getTargetDrawable(getActivity(), null));
        }

        // Add Rest of User Targets for leftys
        for (int j = 0; j < middleFinish; j++) {
            TargetDrawable drawable;
            int i = j + middleStart;
            if (!TextUtils.isEmpty(customIcons[i])) {
                drawable = NavRingHelpers.getCustomDrawable(getActivity(),
                        customIcons[i]);
            } else {
                drawable = NavRingHelpers.getTargetDrawable(getActivity(),
                        targetActivities[i]);
            }
            drawable.setEnabled(true);
            storedDraw.add(drawable);
            intentList.add(intentCounter);
            intentCounter = intentCounter + 1;
        }

        // Add End Place Holder Targets
        for (int i = 0; i < endPosOffset; i++) {
            intentList.add(-1);
            storedDraw.add(NavRingHelpers.getTargetDrawable(getActivity(), null));
        }

        mGlowPadView.setTargetResources(storedDraw);

        if (DEBUG) {
            Log.i(TAG, "Drawables set");
        }

        //maybe swap search icon
        Intent intent = ((SearchManager) getActivity()
                .getSystemService(Context.SEARCH_SERVICE)).getAssistIntent(
                getActivity(), true, UserHandle.USER_CURRENT);
        if (intent != null) {
            ComponentName component = intent.getComponent();
            if (component == null
                    || !mGlowPadView
                            .replaceTargetDrawablesIfPresent(
                                    component,
                                    ASSIST_ICON_METADATA_NAME,
                                    com.android.internal.R.drawable.ic_action_assist_generic)) {
                if (DEBUG) {
                    Log.v(TAG, "MaybeSwapSearchIcon: Couldn't grab icon for component " + component);
                }
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                if (mCurrentClickerer != null) //it shouldn't be, but let's check anyways
                    mCurrentClickerer.onActivityResult(requestCode, resultCode, data);
            } else if ((requestCode == REQUEST_PICK_CUSTOM_ICON)
                    || (requestCode == REQUEST_PICK_LANDSCAPE_ICON)) {

                String iconName = "navring_icon_" + mTargetIndex + ".png";
                FileOutputStream iconStream = null;
                try {
                    iconStream = getActivity().openFileOutput(iconName,
                            Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Could not find icon file: "+iconName);
                    return; // NOOOOO
                }

                Uri selectedImageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                        "tmp_icon_" + mTargetIndex + ".png"));
                if (selectedImageUri == null) {
                    Log.e(TAG, "selectedImageUri is null?");
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());
                if (bitmap == null) {
                    Log.e(TAG, "decode failed or file wasn't there");
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconStream);
                customIcons[mTargetIndex] = Uri.fromFile(
                        new File(getActivity().getFilesDir(), iconName)).getPath();

                File f = new File(selectedImageUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
                Settings.System.putString(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING_ICON[mTargetIndex],customIcons[mTargetIndex]);
                setDrawables();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onTrigger(View v, final int target) {
        mTargetIndex = intentList.get(target);

        Log.i(TAG, "onTrigger target: "+target);

        final int entRes = mBoolLongPress ? R.array.navring_long_dialog_entries : R.array.navring_short_dialog_entries;

        final String[] entries = getActivity().getResources().getStringArray(entRes);

        final ArrayList<String> moddedEntries = new ArrayList<String>();

        final String shortact = AwesomeConstants.getProperName(getActivity(), targetActivities[mTargetIndex]);
        if (DEBUG) Log.d(TAG, "\""+entries[0]+"\" ==> \""+entries[0]+" : "+shortact+"\"");
        moddedEntries.add(entries[0] + "  :  " + shortact);

        if (mBoolLongPress) {
            final String longact = AwesomeConstants.getProperName(getActivity(), longActivities[mTargetIndex]);
            if (DEBUG) Log.d(TAG, "\""+entries[1]+"\" ==> \""+entries[1]+" : "+longact+"\"");
            moddedEntries.add(entries[1] + "  :  " + longact);
        }

        final boolean canSetIcon = !shortact.equals("**app**");
        if (canSetIcon)
            moddedEntries.add(entries[entries.length-1]);

        new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.choose_action_title)).setItems(
                moddedEntries.toArray(new String[0]),
                new NavRingSettingTypeClickerer(this, 
                            canSetIcon ?
                                    (mBoolLongPress ? navring_withlong_dialog_values : navring_dialog_values) :
                                    (mBoolLongPress ? navring_noicon_withlong_dialog_values : navring_noicon_dialog_values)
                            )).show();
    }

    //this handles the short/icon or short/long/icon selection
    private class NavRingSettingTypeClickerer implements DialogInterface.OnClickListener {

        private final E_Action[] vals;

        private final Fragment mParent;

        public NavRingSettingTypeClickerer(final Fragment p, final E_Action[] v) {
            if (DEBUG) Log.i("NavRingSettingTypeClickerer", "Constructing");
            vals = v;
            mParent = p;
        }

        @Override
        public void onClick(DialogInterface dialog, int item) {
            if (DEBUG) Log.v("NavRingSettingTypeClickerer", "Clicked on vals["+item+"]="+vals[item]);
            if (vals[item] == E_Action.T_ICON) {
                final int width = 85;
                final int height = width;

                final Uri tmpfile = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"tmp_icon_" + mTargetIndex + ".png"));

                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("scale", true);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, tmpfile);
                intent.putExtra("outputFormat",
                        Bitmap.CompressFormat.PNG.toString());
                if (DEBUG) {
                    Log.i("NavRingSettingTypeClickerer", "picking custom icon that should output to: "
                            + tmpfile);
                }
                startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON);
            } else {
                //handle short and long press options
                final boolean isLong = vals[item] == E_Action.T_LONG;
                final int titleRes = isLong ? R.string.choose_action_long_title : R.string.choose_action_short_title;
                final String[] acts = NavRingHelpers.getNavRingActions(getActivity());
                final int l = acts.length;
                final String[] mActionNames = new String[l];
                for(int i=0;i<l;i++)
                    mActionNames[i] = AwesomeConstants.getProperName(getActivity(), acts[i]);
                new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.choose_action_title))
                        .setItems(mActionNames,
                                (mCurrentClickerer = new NavRingClickerer(mParent, isLong)))
                        .show();
            }
        }
    }

    //displays action list, etc.
    private class NavRingClickerer implements DialogInterface.OnClickListener, ShortcutPickerHelper.OnPickListener {

        private boolean isLong = false;
        private final ShortcutPickerHelper mPicker;

        private static final String T_APP = "**app**";

        public NavRingClickerer(Fragment f, boolean l) {
            if (DEBUG) Log.i("NavRingClickerer", "Constructing");
            isLong = l;
            mPicker = new ShortcutPickerHelper(f, this);
        }

        public void onActivityResult(int req, int res, Intent i) {
            mPicker.onActivityResult(req, res, i);
        }

        public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
            if (isLong) {
                longActivities[mTargetIndex] = uri;
                Settings.System.putString(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING_LONG[mTargetIndex],longActivities[mTargetIndex]);
            } else {
                targetActivities[mTargetIndex] = uri;
                Settings.System.putString(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING[mTargetIndex],targetActivities[mTargetIndex]);
            }
            setDrawables();
        }

        @Override
        public void onClick(DialogInterface dialog, int item) {
            String act = NavRingHelpers.getNavRingActions(getActivity())[item];
            if (DEBUG) Log.v("NavRingClickerer", "Clicked on action["+item+"]="+act);
            if (T_APP.equals(act)) {
                mPicker.pickShortcut();
            } else {
                if (isLong) {
                    longActivities[mTargetIndex] = act;
                    Settings.System.putString(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING_LONG[mTargetIndex],longActivities[mTargetIndex]);
                } else {
                    // clear previous custom action, because the new short action has its own
                    Settings.System.putString(getActivity().getContentResolver(),Settings.System.SYSTEMUI_NAVRING_ICON[mTargetIndex],(customIcons[mTargetIndex] = ""));

                    targetActivities[mTargetIndex] = act;
                    Settings.System.putString(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING[mTargetIndex],targetActivities[mTargetIndex]);
                }
            }
            setDrawables();
        }
    }

    @Override
    public void onGrabbed(View v, int handle) {
    }

    @Override
    public void onReleased(View v, int handle) {
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }

    public void onTargetChange(View v, final int target) {
    }

    @Override
    public void onFinishFinalAnimation() {
    }
}
