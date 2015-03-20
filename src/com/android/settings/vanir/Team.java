package com.android.settings.vanir;
import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Team extends SettingsPreferenceFragment implements
Preference.OnPreferenceChangeListener {
private static final String TAG = "vanirTeam";
Preference mNuclearMistake;
Preference mDHOMD;
Preference mPrimeDirective;

@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
ContentResolver resolver = getActivity().getContentResolver();
// Load the preferences from an XML resource
addPreferencesFromResource(R.xml.vanir_team);
PreferenceScreen prefSet = getPreferenceScreen();
mNuclearMistake = prefSet.findPreference("vanir_NuclearMistake");
mDHOMD = prefSet.findPreference("vanir_DHOMD");
mPrimeDirective = prefSet.findPreference("vanir_PrimeDirective");

}
@Override
public void onResume() {
super.onResume();
}
@Override
public void onDestroy() {
super.onDestroy();
}
@Override
public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
ContentResolver resolver = getActivity().getContentResolver();
boolean value;
if (preference == mNuclearmistake) {
Toast.makeText(getActivity(), "Be nice to him he builds our nightlys",
Toast.LENGTH_LONG).show();
if (preference == mDHOMD) {
Toast.makeText(getActivity(), "One of the original Vanir devs, Maintainer for Commotio",
Toast.LENGTH_LONG).show();
if (preference == mPrimeDirective) {
Toast.makeText(getActivity(), "In house Java master, Maintainer for EXODUS, Doesnt except feature requests :P",
Toast.LENGTH_LONG).show();
} else {
return super.onPreferenceTreeClick(preferenceScreen, preference);
}
return true;
}
@Override
public boolean onPreferenceChange(Preference preference, Object objValue) {
ContentResolver resolver = getActivity().getContentResolver();
final String key = preference.getKey();
return true;
}
}
