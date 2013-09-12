
package com.android.settings.vanir.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.ContactsContract;
import android.provider.Settings;
import com.android.settings.Utils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.TouchInterceptor;
import com.android.settings.widget.SeekBarPreference;
import com.android.settings.vanir.ImageListPreference;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Statusbar extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "TogglesLayout";

    private static final String PREF_ENABLE_QS = "enabled_qs";
    private static final String PREF_ENABLE_TOGGLES = "enable_toggles";
    private static final String PREF_TOGGLES_PER_ROW = "toggles_per_row";
    private static final String PREF_TOGGLE_FAV_CONTACT = "toggle_fav_contact";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String PREF_TOGGLES_STYLE = "toggle_style";
    private static final String PREF_ALT_BUTTON_LAYOUT = "toggles_layout_preference";
    private static final String TOGGLE_CONTROLS = "toggle_controls";
    
    private final int PICK_CONTACT = 1;

    //quicksettings
    private Preference mEnabledQS;
    private Preference mLayout;
    private ListPreference mTogglesPerRow;
    private Preference mFavContact;
    private ListPreference mQuickPulldown;

    //toggles
    private SwitchPreference mToggleControl;
    private Preference mEnabledToggles;
    private Preference mTogLayout; 
    private ImageListPreference mTogglesLayout;
    private ListPreference mToggleStyle;
    private Preference mResetToggles; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setTitle(R.string.title_statusbar_toggles);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_statusbar);

        mEnabledQS = findPreference(PREF_ENABLE_QS);
        
        mTogglesPerRow = (ListPreference) findPreference(PREF_TOGGLES_PER_ROW);
        mTogglesPerRow.setOnPreferenceChangeListener(this);
        mTogglesPerRow.setValue(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QUICK_TOGGLES_PER_ROW, 3) + "");

        mLayout = findPreference("toggles");

        mFavContact = findPreference(PREF_TOGGLE_FAV_CONTACT);

        final String[] QSentries = getResources().getStringArray(R.array.available_QStoggles_entries);
        final String[] entries = getResources().getStringArray(R.array.available_toggles_entries);

        List<String> allQSToggles = Arrays.asList(QSentries);
        List<String> allToggles = Arrays.asList(entries);

        if (allQSToggles.contains("FAVCONTACT")) {
            ArrayList<String> enabledQSToggles = getQSTogglesStringArray(getActivity());
            mFavContact.setEnabled(enabledQSToggles.contains("FAVCONTACT"));
        }
        else {
            getPreferenceScreen().removePreference(mFavContact);
        }

        mToggleControl = (SwitchPreference) findPreference(TOGGLE_CONTROLS);
        mToggleControl.setOnPreferenceChangeListener(this);
        
        mEnabledToggles = findPreference(PREF_ENABLE_TOGGLES);

        mToggleStyle = (ListPreference) findPreference(PREF_TOGGLES_STYLE);
        mToggleStyle.setOnPreferenceChangeListener(this);
        mToggleStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_TOGGLES_STYLE, 3)));

        mTogglesLayout = (ImageListPreference) findPreference(PREF_ALT_BUTTON_LAYOUT);
        mTogglesLayout.setOnPreferenceChangeListener(this);

        mTogLayout = findPreference("og_toggles");

        mResetToggles = findPreference("reset_toggles"); 
    }
    
     @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        
        mQuickPulldown = (ListPreference) prefSet.findPreference(QUICK_PULLDOWN);
        if (!Utils.isPhone(getActivity())) {
            getPreferenceScreen().removePreference(mQuickPulldown);
        } else {
            mQuickPulldown.setOnPreferenceChangeListener(this);
            int quickPulldownValue = Settings.System.getInt(resolver, Settings.System.QS_QUICK_PULLDOWN, 0);
            mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
            updatePulldownSummary();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        boolean result = false;

        if (preference == mTogglesPerRow) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TOGGLES_PER_ROW, val);
            return true;
        } else if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QS_QUICK_PULLDOWN,
                    statusQuickPulldown);
            updatePulldownSummary();
            return true;
        } else if (preference == mToggleStyle) {
            int val = Integer.parseInt((String) newValue);
            result = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_TOGGLES_STYLE, val);
        } else if (preference == mTogglesLayout) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_TOGGLES_STYLE, val == 0 ? 3 : 2);
            result = Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_TOGGLES_USE_BUTTONS,
                    val);
        }
        return result;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        if (preference == mEnabledQS) {
            AlertDialog.Builder builderQS = new AlertDialog.Builder(getActivity());

            ArrayList<String> enabledQS = getQSTogglesStringArray(getActivity());

            final String[] finalArray = getResources().getStringArray(
                    R.array.available_QStoggles_entries);
            final String[] values = getResources().getStringArray(R.array.available_QStoggles_values);

            boolean checkedQSToggles[] = new boolean[finalArray.length];

            for (int i = 0; i < checkedQSToggles.length; i++) {
                if (enabledQS.contains(finalArray[i])) {
                    checkedQSToggles[i] = true;
                }
            }

            builderQS.setTitle(R.string.toggles_display_dialog);
            builderQS.setCancelable(true);
            builderQS.setPositiveButton(R.string.toggles_display_close,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builderQS.setMultiChoiceItems(values, checkedQSToggles, new OnMultiChoiceClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    String QSKey = (finalArray[which]);

                    if (isChecked)
                        addQSToggle(getActivity(), QSKey);
                    else
                        removeQSToggle(getActivity(), QSKey);
                        
                     if (QSKey.equals("FAVCONTACT")) {
                        mFavContact.setEnabled(isChecked);
                    }
                }
            });

            AlertDialog d = builderQS.create();

            d.show();

            return true;
        } else if (preference == mLayout) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            QSTogglesLayout fragment = new QSTogglesLayout();
            ft.addToBackStack("toggles_layout");
            ft.replace(this.getId(), fragment);
            ft.commit();
        } else if (preference == mFavContact) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        } else if (preference == mEnabledToggles) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            ArrayList<String> enabledToggles = getTogglesStringArray(getActivity());

            final String[] togglesArray = getResources().getStringArray(
                    R.array.available_toggles_entries);
            final String[] toggleValues = getResources().getStringArray(R.array.available_toggles_values);

            boolean checkedToggles[] = new boolean[togglesArray.length];

            for (int i = 0; i < checkedToggles.length; i++) {
                if (enabledToggles.contains(togglesArray[i])) {
                    checkedToggles[i] = true;
                }
            }

            builder.setTitle(R.string.toggles_display_dialog);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.toggles_display_close,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.setMultiChoiceItems(toggleValues, checkedToggles, new OnMultiChoiceClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    String toggleKey = (togglesArray[which]);

                    if (isChecked)
                        addToggle(getActivity(), toggleKey);
                    else
                        removeToggle(getActivity(), toggleKey);
                }
            });

            AlertDialog d = builder.create();

            d.show();

            return true;
        } else if (preference == mTogLayout) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            TogglesLayout fragment = new TogglesLayout();
            ft.addToBackStack("og_toggles_layout");
            ft.replace(this.getId(), fragment);
            ft.commit();

        } else if (preference == mResetToggles) {
            // return default setup
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_TOGGLES, "WIFI|BT|GPS|ROTATE|VIBRATE|SYNC|SILENT");
            return true;
        } 
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_CONTACT) {
                Uri contactData = data.getData();
                String[] projection = new String[] {ContactsContract.Contacts.LOOKUP_KEY};
                String selection = ContactsContract.Contacts.DISPLAY_NAME + " IS NOT NULL";
                CursorLoader cursorLoader =  null; 
                Cursor cursor = null; 
                try {
					cursorLoader = new CursorLoader(getActivity().getBaseContext(), contactData, projection, selection, null, null);
					cursor = cursorLoader.loadInBackground();
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            String lookup_key = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                            Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.QUICK_TOGGLE_FAV_CONTACT, lookup_key);
                        }
                    }
                    } finally {
                        if (cursor != null) {
                            cursorLoader = null;
                            cursor.close();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addQSToggle(Context context, String key) {
        ArrayList<String> enabledQS = getQSTogglesStringArray(context);
        enabledQS.add(key);
        setQSTogglesFromStringArray(context, enabledQS);
    }

    public void addToggle(Context context, String key) {
        ArrayList<String> enabledToggles = getTogglesStringArray(context);
        enabledToggles.add(key);
        setTogglesFromStringArray(context, enabledToggles);
    } 

    public void removeQSToggle(Context context, String key) {
        ArrayList<String> enabledQS = getQSTogglesStringArray(context);
        enabledQS.remove(key);
        setQSTogglesFromStringArray(context, enabledQS);
    }

    public void removeToggle(Context context, String key) {
        ArrayList<String> enabledToggles = getTogglesStringArray(context);
        enabledToggles.remove(key);
        setTogglesFromStringArray(context, enabledToggles);
    } 

    private void updatePulldownSummary() {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        int summaryId;
        int directionId;
        summaryId = R.string.summary_quick_pulldown;
        String value = Settings.System.getString(resolver, Settings.System.QS_QUICK_PULLDOWN);
        String[] pulldownArray = getResources().getStringArray(R.array.quick_pulldown_values);
        if (pulldownArray[0].equals(value)) {
            directionId = R.string.quick_pulldown_off;
            mQuickPulldown.setValueIndex(0);
            mQuickPulldown.setSummary(getResources().getString(directionId));
        } else if (pulldownArray[1].equals(value)) {
            directionId = R.string.quick_pulldown_right;
            mQuickPulldown.setValueIndex(1);
            mQuickPulldown.setSummary(getResources().getString(directionId)
                    + " " + getResources().getString(summaryId));
        } else {
            directionId = R.string.quick_pulldown_left;
            mQuickPulldown.setValueIndex(2);
            mQuickPulldown.setSummary(getResources().getString(directionId)
                    + " " + getResources().getString(summaryId));
        }
    }

    // Quicksettings preference subclass
    private class QSTogglesLayout extends ListFragment {

        private ListView mQSButtonList;
        private QSButtonAdapter mQSButtonAdapter;
        private Context mContext;

        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            mContext = getActivity().getBaseContext();

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View v = inflater.inflate(R.layout.order_power_widget_buttons_activity, container,
                    false);

            return v;
        }

        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mQSButtonList = this.getListView();
            ((TouchInterceptor) mQSButtonList).setDropListener(mDropListener);
            mQSButtonAdapter = new QSButtonAdapter(mContext);
            setListAdapter(mQSButtonAdapter);
        };

        @Override
        public void onDestroy() {
            ((TouchInterceptor) mQSButtonList).setDropListener(null);
            setListAdapter(null);
            super.onDestroy();
        }

        @Override
        public void onResume() {
            super.onResume();
            // reload our buttons and invalidate the views for redraw
            mQSButtonAdapter.reloadButtons();
            mQSButtonList.invalidateViews();
        }

        private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
            public void drop(int from, int to) {
                // get the current button list
                ArrayList<String> toggles = getQSTogglesStringArray(mContext);

                // move the button
                if (from < toggles.size()) {
                    String toggle = toggles.remove(from);

                    if (to <= toggles.size()) {
                        toggles.add(to, toggle);

                        // save our buttons
                        setQSTogglesFromStringArray(mContext, toggles);

                        // tell our adapter/listview to reload
                        mQSButtonAdapter.reloadButtons();
                        mQSButtonList.invalidateViews();
                    }
                }
            }
        };

        private class QSButtonAdapter extends BaseAdapter {
            private Context mContext;
            private Resources mSystemUIResources = null;
            private LayoutInflater mInflater;
            private ArrayList<Toggle> mToggles;

            public QSButtonAdapter(Context c) {
                mContext = c;
                mInflater = LayoutInflater.from(mContext);

                PackageManager pm = mContext.getPackageManager();
                if (pm != null) {
                    try {
                        mSystemUIResources = pm.getResourcesForApplication("com.android.systemui");
                    } catch (Exception e) {
                        mSystemUIResources = null;
                        Log.e(TAG, "Could not load SystemUI resources", e);
                    }
                }

                reloadButtons();
            }

            public void reloadButtons() {
                ArrayList<String> toggles = getQSTogglesStringArray(mContext);

                mToggles = new ArrayList<Toggle>();
                for (String toggle : toggles) {
                    mToggles.add(new Toggle(toggle, 0));
                }
            }

            public int getCount() {
                return mToggles.size();
            }

            public Object getItem(int position) {
                return mToggles.get(position);
            }

            public long getItemId(int position) {
                return position;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final View v;
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.order_power_widget_button_list_item, null);
                } else {
                    v = convertView;
                }

                Toggle toggle = mToggles.get(position);
                final TextView name = (TextView) v.findViewById(R.id.name);
                name.setText(toggle.getId());
                return v;
            }
        }

    }

    // Toggles preference subclass
    private class TogglesLayout extends ListFragment {

        private ListView mButtonList;
        private ButtonAdapter mButtonAdapter;
        private Context mContext;

        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            mContext = getActivity().getBaseContext();

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View v = inflater.inflate(R.layout.order_power_widget_buttons_activity, container,
                    false);

            return v;
        }

        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mButtonList = this.getListView();
            ((TouchInterceptor) mButtonList).setDropListener(mDropListener);
            mButtonAdapter = new ButtonAdapter(mContext);
            setListAdapter(mButtonAdapter);
        };

        @Override
        public void onDestroy() {
            ((TouchInterceptor) mButtonList).setDropListener(null);
            setListAdapter(null);
            super.onDestroy();
        }

        @Override
        public void onResume() {
            super.onResume();
            // reload our buttons and invalidate the views for redraw
            mButtonAdapter.reloadButtons();
            mButtonList.invalidateViews();
        }

        private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
            public void drop(int from, int to) {
                // get the current button list
                ArrayList<String> toggles = getTogglesStringArray(mContext);

                // move the button
                if (from < toggles.size()) {
                    String toggle = toggles.remove(from);

                    if (to <= toggles.size()) {
                        toggles.add(to, toggle);

                        // save our buttons
                       setTogglesFromStringArray(mContext, toggles);

                        // tell our adapter/listview to reload
                        mButtonAdapter.reloadButtons();
                        mButtonList.invalidateViews();
                    }
                }
            }
        };

        private class ButtonAdapter extends BaseAdapter {
            private Context mContext;
            private Resources mSystemUIResources = null;
            private LayoutInflater mInflater;
            private ArrayList<Toggle> mToggles;

            public ButtonAdapter(Context c) {
                mContext = c;
                mInflater = LayoutInflater.from(mContext);

                PackageManager pm = mContext.getPackageManager();
                if (pm != null) {
                    try {
                        mSystemUIResources = pm.getResourcesForApplication("com.android.systemui");
                    } catch (Exception e) {
                        mSystemUIResources = null;
                        Log.e(TAG, "Could not load SystemUI resources", e);
                    }
                }

                reloadButtons();
            }

            public void reloadButtons() {
                ArrayList<String> toggles = getTogglesStringArray(mContext);

                mToggles = new ArrayList<Toggle>();
                for (String toggle : toggles) {
                    mToggles.add(new Toggle(toggle, 0));
                }
            }

            public int getCount() {
                return mToggles.size();
            }
            public Object getItem(int position) {
                return mToggles.get(position);
            }

            public long getItemId(int position) {
                return position;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final View v;
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.order_power_widget_button_list_item, null);
                } else {
                    v = convertView;
                }

                Toggle toggle = mToggles.get(position);
               final TextView name = (TextView) v.findViewById(R.id.name);
                name.setText(toggle.getId());
                return v;
            }
        }

    }

    private class Toggle {
        private String mId;
        private int mTitleResId;

        public Toggle(String id, int titleResId) {
            mId = id;
            mTitleResId = titleResId;
        }

        public String getId() {
            return mId;
        }

        public int getTitleResId() {
            return mTitleResId;
        }
    }

    // Set quicksettings string
    private void setQSTogglesFromStringArray(Context c, ArrayList<String> newQSGoodies) {
        String newQSToggles = "";

        for (String s : newQSGoodies)
            newQSToggles += s + "|";

        // remote last |
        try {
            newQSToggles = newQSToggles.substring(0, newQSToggles.length() - 1);
        } catch (StringIndexOutOfBoundsException e) {
        }

        Settings.System.putString(c.getContentResolver(), Settings.System.QUICK_TOGGLES,
                newQSToggles);
    }

    // Set toggles string
    private void setTogglesFromStringArray(Context c, ArrayList<String> newGoodies) {
        String newToggles = "";

        for (String pP : newGoodies)
            newToggles += pP + "|";

        // remote last |
        try {
            newToggles = newToggles.substring(0, newToggles.length() - 1);
        } catch (StringIndexOutOfBoundsException e) {
        }

        Settings.System.putString(c.getContentResolver(), Settings.System.STATUSBAR_TOGGLES,
                newToggles);
    }

    private ArrayList<String> getQSTogglesStringArray(Context c) {
        String clusterfuck = Settings.System.getString(c.getContentResolver(),
                Settings.System.QUICK_TOGGLES);

        if (clusterfuck == null) {
            Log.e(TAG, "clusterfuck was null");
            // return null;
            clusterfuck = getResources().getString(R.string.toggle_default_entries);
        }

        String[] QSStringArray = clusterfuck.split("\\|");
        ArrayList<String> iloveyou = new ArrayList<String>();
        for (String s : QSStringArray) {
            if(s != null && s != "") {
                Log.e(TAG, "adding: " + s);
                iloveyou.add(s);
            }
        }
        return iloveyou;
    }

    private ArrayList<String> getTogglesStringArray(Context c) {
        String dingdong = Settings.System.getString(c.getContentResolver(),
                Settings.System.STATUSBAR_TOGGLES);

        if (dingdong == null) {
            Log.e(TAG, "clusterfuck dingdong was null");
            // return null;
            dingdong = "WIFI|BT|GPS|ROTATE|VIBRATE|SYNC|SILENT";
        }

        String[] togglesStringArray = dingdong.split("\\|");
        ArrayList<String> goldfish = new ArrayList<String>();
        for (String pP : togglesStringArray) {
            if(pP != null && pP !="") {
                Log.e(TAG, "adding: " + pP);
                goldfish.add(pP);
            }
        }

        return goldfish;
    } 
}
