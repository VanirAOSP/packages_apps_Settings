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
    private static final boolean DEBUG = false;

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
        mTargetNumAmount.post(new Runnable() {
            public void run() {
                mTargetNumAmount
                        .setOnItemSelectedListener(new AmountListener());
            }
        });

        mLongPressStatus = (Switch) getActivity().findViewById(
                R.id.longpress_switch);
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
        mTargetNumAmount.setSelection(mNavRingAmount - 1);
        mLongPressStatus.setChecked(mBoolLongPress);

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
                    Log.v(TAG, "Couldn't grab icon for component " + component);
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
                    return; // NOOOOO
                }

                Uri selectedImageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                        "tmp_icon_" + mTargetIndex + ".png"));
                try {
                    Log.e(TAG,
                            "Selected image path: "
                                    + selectedImageUri.getPath());
                    Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri
                            .getPath());
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconStream);
                } catch (NullPointerException npe) {
                    Log.e(TAG, "SeletedImageUri was null.");
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                customIcons[mTargetIndex] = Uri.fromFile(
                        new File(getActivity().getFilesDir(), iconName)).getPath();

                File f = new File(selectedImageUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
                setDrawables();
                Settings.System.putString(getActivity().getContentResolver(), Settings.System.SYSTEMUI_NAVRING_ICON[mTargetIndex],customIcons[mTargetIndex]);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onTrigger(View v, final int target) {
        mTargetIndex = intentList.get(target);

        final int entRes = mBoolLongPress ? R.array.navring_long_dialog_entries : R.array.navring_short_dialog_entries;
        final int valRes = mBoolLongPress ? R.array.navring_long_dialog_values : R.array.navring_short_dialog_values;
        
        final String[] values = getActivity().getResources().getStringArray(valRes);

        values[0] += "  :  " + AwesomeConstants.getProperName(getActivity(), targetActivities[mTargetIndex]);
        if (mBoolLongPress) {
            values[1] += "  :  " + AwesomeConstants.getProperName(getActivity(), longActivities[mTargetIndex]);
        }

        new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.choose_action_title)).setItems(
                getActivity().getResources().getStringArray(entRes),
                new NavRingSettingTypeClickerer(this, values)).show();
    }

    //this handles the short/icon or short/long/icon selection
    private class NavRingSettingTypeClickerer implements DialogInterface.OnClickListener {

        private final String[] vals;

        private static final String T_ICON = "**icon**";
        private static final String T_SHORT = "**short**";
        private static final String T_LONG = "**long**";
        private final Fragment mParent;

        public NavRingSettingTypeClickerer(final Fragment p, final String[] v) {
            vals = v;
            mParent = p;
        }

        @Override
        public void onClick(DialogInterface dialog, int item) {
            if (T_ICON.equals(vals[item])) {
                final int width = 85;
                final int height = width;

                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("scale", true);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"tmp_icon_" + mTargetIndex + ".png")));
                intent.putExtra("outputFormat",
                        Bitmap.CompressFormat.PNG.toString());
                startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON);
            } else {
                //handle short and long press options
                final boolean isLong = T_LONG.equals(vals[item]);
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
            setDrawables();
        }
    }

    //displays action list, etc.
    private class NavRingClickerer implements DialogInterface.OnClickListener, ShortcutPickerHelper.OnPickListener {

        private boolean isLong = false;
        private final ShortcutPickerHelper mPicker;

        private static final String T_APP = "**app**";

        public NavRingClickerer(Fragment f, boolean l) {
            isLong = l;
            mPicker = new ShortcutPickerHelper(f, this);
        }

        public void onActivityResult(int req, int res, Intent i) {
            mPicker.onActivityResult(req, res, i);
        }

        public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
            if (isLong)
                longActivities[mTargetIndex] = AwesomeConstants.getProperName(getActivity(), uri);
            else
                targetActivities[mTargetIndex] = AwesomeConstants.getProperName(getActivity(), uri);
            setDrawables();
        }

        @Override
        public void onClick(DialogInterface dialog, int item) {
            String act = NavRingHelpers.getNavRingActions(getActivity())[item];
            if (T_APP.equals(act)) {
                mPicker.pickShortcut();
            } else {
                if (isLong)
                    longActivities[mTargetIndex] = AwesomeConstants.getProperName(getActivity(), act);
                else
                    targetActivities[mTargetIndex] = AwesomeConstants.getProperName(getActivity(), act);
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
