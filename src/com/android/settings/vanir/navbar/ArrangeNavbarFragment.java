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
package com.android.settings.vanir.navbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.util.vanir.AwesomeConstants;
import com.android.internal.util.vanir.AwesomeConstants.AwesomeConstant;
import com.android.internal.util.vanir.KeyButtonInfo;
import com.android.internal.util.aokp.NavBarHelpers;
import com.android.settings.R;
import com.android.settings.util.ShortcutPickerHelper;
import com.android.settings.util.ShortcutPickerHelper.OnPickListener;
import com.google.android.apps.dashclock.ui.DragGripView;
import com.google.android.apps.dashclock.ui.SwipeDismissListViewTouchListener;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;


public class ArrangeNavbarFragment extends Fragment implements OnPickListener {

    private static final String TAG = ArrangeNavbarFragment.class.getSimpleName();

    public static final int REQUEST_PICK_CUSTOM_ICON = 200;
    public static final int REQUEST_PICK_LANDSCAPE_ICON = 201;

    private static final String[] buttonSettingsStrings = new String[] {
        Settings.System.NAVIGATION_BAR_BUTTONS,Settings.System.NAVIGATION_BAR_BUTTONS_TWO,Settings.System.NAVIGATION_BAR_BUTTONS_THREE,Settings.System.NAVIGATION_BAR_BUTTONS_FOUR
    };

    DragSortListView mListView;
    NavbarButtonsAdapter mAdapter;
    DragSortController mDragSortController;
    CheckBox mNxOption;

    private ArrayList<KeyButtonInfo> mNavButtons = new ArrayList<KeyButtonInfo>();

    private ShortcutPickerHelper mPicker;
    private int mTargetIndex = 0;
    private int mTarget = 0;
    DialogConstant mActionTypeToChange;
    KeyButtonInfo mSelectedButton;
    private String[] mActions;
    private String[] mActionCodes;
    CharSequence[] items;
    int mCurrentLayout = 1;
    TextView mLayoutInfo;
    ViewGroup rootView;
    int mLayoutNumber;
    String mLayoutConfig;

    public static enum DialogConstant {
        ICON_ACTION {
            @Override
            public String value() {
                return "**icon**";
            }
        },
        LONG_ACTION {
            @Override
            public String value() {
                return "**long**";
            }
        },
        DOUBLE_TAP_ACTION {
            @Override
            public String value() {
                return "**double**";
            }
        },
        SHORT_ACTION {
            @Override
            public String value() {
                return "**short**";
            }
        },
        CUSTOM_APP {
            @Override
            public String value() {
                return "**app**";
            }
        },
        NOT_IN_ENUM {
            @Override
            public String value() {
                return "**notinenum**";
            }
        };

        public String value() {
            return this.value();
        }
    }

    public static DialogConstant funcFromString(String string) {
        DialogConstant[] allTargs = DialogConstant.values();
        for (int i = 0; i < allTargs.length; i++) {
            if (string.equals(allTargs[i].value())) {
                return allTargs[i];
            }
        }
        // not in ENUM must be custom
        return DialogConstant.NOT_IN_ENUM;
    }

    public ArrangeNavbarFragment() {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.navbar_setup, menu);
        MenuItem item = menu.findItem(R.id.change_navbar_number);

        if (mLayoutNumber == 1) {
            item.setVisible(false);
        } else {
            item.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.change_navbar_number:
                openLayoutPreferenceDialog();
                break;
            case R.id.menu_add_button:
                mNavButtons.add(new KeyButtonInfo(null, null, null, null));
                saveUserConfig();
                mAdapter.notifyDataSetChanged();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        final ContentResolver cr = getActivity().getContentResolver();
        mLayoutNumber = Settings.System.getInt(cr,
                Settings.System.NAVIGATION_BAR_ALTERNATE_LAYOUTS, 1);
        mLayoutConfig = Settings.System.getString(cr,
                Settings.System.NAVIGATION_BAR_NX_LAYOUTS);
        if (mLayoutConfig == null) {
            mLayoutConfig = "0-0-0-0-0";
        }

        // Get NavBar Actions
        mActionCodes = NavBarHelpers.getNavBarActions(getActivity());
        mActions = new String[mActionCodes.length];
        int actionqty = mActions.length;
        for (int i = 0; i < actionqty; i++) {
            mActions[i] = AwesomeConstants.getProperName(getActivity(),
                    mActionCodes[i]);
        }

        mPicker = new ShortcutPickerHelper(this, this);
        readUserConfig();
    }

    @Override
    public void onResume() {
        super.onResume();
        final ContentResolver cr = getActivity().getContentResolver();
        mLayoutNumber = Settings.System.getInt(cr,
                Settings.System.NAVIGATION_BAR_ALTERNATE_LAYOUTS, 1);
        if (mLayoutNumber == 1) mNxOption.setVisibility(View.GONE);
        updateLayoutInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup)
                inflater.inflate(R.layout.fragment_arrange_toggles, container, false);
        mLayoutInfo = (TextView) rootView.findViewById(R.id.navbar_arrange_info);

        mListView = (DragSortListView) rootView.findViewById(android.R.id.list);
        mNxOption = (CheckBox) rootView.findViewById(R.id.nx_bar_style);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNxOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder s = new StringBuilder();
                String[] layouts = mLayoutConfig.split("-", 5);
                for (int i = 0; i <= 4; i++) {
                    if (i == mCurrentLayout - 1) {
                        if (mNxOption.isChecked()) {
                            s.append("1");
                        } else {
                            s.append("0");
                        }
                    } else {
                        s.append(layouts[i]);
                    }
                    if (i != 4) s.append("-");
                }
                Settings.System.putString(
                        getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_NX_LAYOUTS, s.toString());

                if (mNxOption.isChecked()) {
                    mListView.setEnabled(false);
                    mListView.setVisibility(View.INVISIBLE);
                } else {
                    mListView.setEnabled(true);
                    mListView.setVisibility(View.VISIBLE);
                }
            }
        });

        mListView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                if (from != to) {
                    KeyButtonInfo remove = mNavButtons.remove(from);
                    mNavButtons.add(to, remove);
                    saveUserConfig();
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        final SwipeDismissListViewTouchListener swipeOnTouchListener =
                new SwipeDismissListViewTouchListener(
                        mListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {

                            public boolean canDismiss(int position) {
                                return position < mAdapter.getCount();
                            }

                            public void onDismiss(ListView listView, int[]
                                    reverseSortedPositions) {
                                for (int index : reverseSortedPositions) {
                                    mNavButtons.remove(index);
                                }
                                saveUserConfig();
                                mAdapter.notifyDataSetChanged();
                            }
                        });
        mListView.setFloatViewManager(mDragSortController = new ConfigurationDragSortController());
        mListView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mDragSortController.onTouch(view, motionEvent)
                        || (!mDragSortController.isDragging()
                        && swipeOnTouchListener.onTouch(view, motionEvent));
            }
        });
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int postition, long id) {
                mSelectedButton = mAdapter.getItem(postition);

                final String[] entries = getActivity().getResources()
                        .getStringArray(R.array.navbar_dialog_entries);
                entries[0] = entries[0]
                        + "  :  "
                        + NavBarHelpers.getProperSummary(getActivity(),
                        mSelectedButton.singleAction);
                entries[1] = entries[1]
                        + "  :  "
                        + NavBarHelpers.getProperSummary(getActivity(),
                        mSelectedButton.longPressAction);
                entries[2] = entries[2]
                        + "  :  "
                        + NavBarHelpers.getProperSummary(getActivity(),
                        mSelectedButton.doubleTapAction);

                final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        onValueChange(getResources().getStringArray(R.array.navbar_dialog_values)[item]);
                        dialog.dismiss();
                    }
                };

                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.choose_action_title))
                        .setItems(entries, l)
                        .create();

                dialog.show();
            }
        });
        mListView.setOnScrollListener(swipeOnTouchListener.makeScrollListener());
        mListView.setItemsCanFocus(true);
        mListView.setDragEnabled(true);
        mListView.setFloatAlpha(0.8f);
        mListView.setAdapter(mAdapter = new NavbarButtonsAdapter(getActivity(), mNavButtons));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView.setAdapter(null);
        mAdapter = null;
    }

    public void onValueChange(String uri) {
        DialogConstant dConstant = funcFromString(uri);

        switch (dConstant) {
            case CUSTOM_APP:
                mPicker.pickShortcut();
                break;
            case SHORT_ACTION:
            case LONG_ACTION:
            case DOUBLE_TAP_ACTION:
                mActionTypeToChange = dConstant;
                createDialog(getTitleForTargetType(dConstant),
                        mActions, mActionCodes);
                break;
            case ICON_ACTION:
                mActionTypeToChange = dConstant;
                int width = 85;
                int height = width;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("scale", true);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, getTempFileUri());
                intent.putExtra("outputFormat",
                        Bitmap.CompressFormat.PNG.toString());
                Log.i(TAG, "started for result, should output to: "
                        + getTempFileUri());
                startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON);
                break;
            case NOT_IN_ENUM:
                // action was selected, uri should be the value
                // mSelectedButton
                switch (mActionTypeToChange) {
                    case SHORT_ACTION:
                        mSelectedButton.singleAction = uri;
                        break;
                    case LONG_ACTION:
                        mSelectedButton.longPressAction = uri;
                        break;
                    case DOUBLE_TAP_ACTION:
                        mSelectedButton.doubleTapAction = uri;
                        break;
                    case ICON_ACTION:
                        mSelectedButton.iconUri = uri;
                        break;
                }
                saveUserConfig();
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

    private String getTitleForTargetType(final DialogConstant constant) {
        String title = "";
        int stringRes = R.string.choose_action_double_tap_title;
        switch (constant) {
            case SHORT_ACTION:
                stringRes = R.string.choose_action_short_title;
                break;
            case LONG_ACTION:
                stringRes = R.string.choose_action_long_title;
                break;
            case DOUBLE_TAP_ACTION:
                stringRes = R.string.choose_action_double_tap_title;
                break;
            default:
                break;
        }
        title = getString(stringRes);
        return title;
    }

    private class NavbarButtonsAdapter extends ArrayAdapter<KeyButtonInfo> {

        boolean mShowDragGrips = true;

        public NavbarButtonsAdapter(Context context, ArrayList<KeyButtonInfo> toggles) {
            super(context, android.R.id.text1, toggles);
        }

        public void setShowDragGrips(boolean show) {
            this.mShowDragGrips = show;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_toggle, parent, false);

            TextView titleView = (TextView) convertView.findViewById(android.R.id.text1);

            KeyButtonInfo button = getItem(position);
            String text = NavBarHelpers.getProperSummary(getContext(), button.singleAction);
            ImageView image = (ImageView) convertView.findViewById(R.id.image);
            DragGripView dragGripView = (DragGripView) convertView.findViewById(R.id.drag_handle);

            titleView.setText(text);
            image.setImageDrawable(NavBarHelpers.getIconImage(getContext(),
                    button.iconUri.isEmpty() ? button.singleAction : button.iconUri));


            return convertView;
        }

    }

    public void createDialog(final String title, final String[] entries,
                             final String[] values) {
        final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                onValueChange(values[item]);
                dialog.dismiss();
            }
        };

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(title).setItems(entries, l).create();

        dialog.show();
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp,
                               boolean isApplication) {
        switch (mActionTypeToChange) {
            case SHORT_ACTION:
                mSelectedButton.singleAction = uri;
                break;
            case LONG_ACTION:
                mSelectedButton.longPressAction = uri;
                break;
            case DOUBLE_TAP_ACTION:
                mSelectedButton.doubleTapAction = uri;
                break;
            case ICON_ACTION:
                mSelectedButton.iconUri = uri;
                break;
        }
        saveUserConfig();
        mAdapter.notifyDataSetChanged();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            } else if ((requestCode == REQUEST_PICK_CUSTOM_ICON)
                    || (requestCode == REQUEST_PICK_LANDSCAPE_ICON)) {

                String iconName = getIconFileName(mNavButtons.indexOf(mSelectedButton));
                FileOutputStream iconStream = null;
                try {
                    iconStream = getActivity().openFileOutput(iconName,
                            Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }

                Uri tempSelectedUri = getTempFileUri();
                try {
                    Log.e(TAG,
                            "Selected image path: "
                                    + tempSelectedUri.getPath());
                    Bitmap bitmap = BitmapFactory.decodeFile(tempSelectedUri
                            .getPath());
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconStream);
                } catch (NullPointerException npe) {
                    Log.e(TAG, "SeletedImageUri was null.");
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }

                String imageUri = Uri.fromFile(
                        new File(getActivity().getFilesDir(), iconName)).getPath();

                switch (mActionTypeToChange) {
                    case SHORT_ACTION:
                        mSelectedButton.singleAction = imageUri;
                        break;
                    case LONG_ACTION:
                        mSelectedButton.longPressAction = imageUri;
                        break;
                    case DOUBLE_TAP_ACTION:
                        mSelectedButton.doubleTapAction = imageUri;
                        break;
                    case ICON_ACTION:
                        mSelectedButton.iconUri = imageUri;
                        break;
                }

                File f = new File(tempSelectedUri.getPath());
                if (f.exists()) {
                    f.delete();
                }

                saveUserConfig();
                mAdapter.notifyDataSetChanged();
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Uri getTempFileUri() {
        return Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                "tmp_icon_" + mNavButtons.indexOf(mSelectedButton) + ".png"));

    }

    private String getIconFileName(int index) {
        return "navbar_icon_" + index + ".png";
    }

    private void openLayoutPreferenceDialog() {
        items = new CharSequence[mLayoutNumber];
        for (int i = 0; i < mLayoutNumber; i++) {
            items[i] = Integer.toString(i + 1);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.change_layouts_dialog_title);
        builder.setSingleChoiceItems(items, mCurrentLayout - 1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                mCurrentLayout = item + 1;
                readUserConfig();
                mAdapter.notifyDataSetChanged();
                updateLayoutInfo();
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateLayoutInfo() {
        String[] layouts = mLayoutConfig.split("-", 5);
        mNxOption.setChecked(false);
        for (int i = 0; i <= 4; i++) {
            if (Integer.parseInt(layouts[i]) == 1 && i == mCurrentLayout - 1) {
                mNxOption.setChecked(true);
            }
        }

        if (mNxOption.isChecked()) {
            mListView.setEnabled(false);
            mListView.setVisibility(View.INVISIBLE);
        } else {
            mListView.setEnabled(true);
            mListView.setVisibility(View.VISIBLE);
        }
            
        String string = (getString(R.string.change_layouts_title)+ " " + mCurrentLayout);
        if (mLayoutNumber != 1) {
            mLayoutInfo.setText(string + ": " + getString(R.string.toggles_arrange_instructions));
        } else {
            mLayoutInfo.setText(getString(R.string.toggles_arrange_instructions));
        }
    }

    private class ConfigurationDragSortController extends DragSortController {

        public ConfigurationDragSortController() {
            super(ArrangeNavbarFragment.this.mListView, R.id.drag_handle,
                    DragSortController.ON_DRAG, (DragSortController.FLING_LEFT_REMOVE & DragSortController.FLING_RIGHT_REMOVE));
            setBackgroundColor(0x363636);
        }

        @Override
        public void onDragFloatView(View floatView, Point floatPoint, Point touchPoint) {
        }

        @Override
        public View onCreateFloatView(int position) {
            View v = mAdapter.getView(position, null, ArrangeNavbarFragment.this.mListView);
            return v;
        }

        @Override
        public void onDestroyFloatView(View floatView) {
        }

    }

    private void saveUserConfig() {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < mNavButtons.size(); i++) {
     //       if (mNxOption.isChecked()) {
                // will probably use 4 regular key button infos here
     //       } else {
                s.append(mNavButtons.get(i).toString());
                if (i != mNavButtons.size() - 1) {
                    s.append("|");
                }
     //       }
        }
        Settings.System.putString(getActivity().getContentResolver(), buttonSettingsStrings[mCurrentLayout-1], s.toString());
    }

    private void readUserConfig() {
        final ContentResolver cr = getActivity().getContentResolver();
        String buttons = "";
        mNavButtons.clear();
        buttons = Settings.System.getString(cr, buttonSettingsStrings[mCurrentLayout-1]);
        if (buttons == null || buttons.isEmpty()) {
            buttons = AwesomeConstants.defaultNavbarLayout(getActivity());
        }
        /**
        * Format:
        *
        * singleTapAction,doubleTapAction,longPressAction,iconUri|singleTap...
        */
        String[] userButtons = buttons.split("\\|");
        if (userButtons != null) {
            for (String button : userButtons) {
                String[] actions = button.split(",", 4);
                mNavButtons.add(new KeyButtonInfo(actions[0], actions[1], actions[2], actions[3]));
            }
        }
    }
}
