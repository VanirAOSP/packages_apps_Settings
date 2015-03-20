package com.android.settings.candykat;
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
private static final String TAG = "CandyRomsTeam";
Preference mMatthew0776;
Preference mMar5hal;
Preference mCyberScopes;
Preference mGimmeitorilltell;
Preference mBMP7777;
Preference mTr1gg3r84;
Preference mRc420head;
Preference mFlashalot;
Preference mVenomtester;
Preference mCuzz1369;
Preference mCannondaleV2000;
Preference mDarknites;
Preference mRapier;
@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
ContentResolver resolver = getActivity().getContentResolver();
// Load the preferences from an XML resource
addPreferencesFromResource(R.xml.candykat_team);
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
if (preference == mNuclearMistake) {
Toast.makeText(getActivity(), "Be nice to him he builds our l5 nightlys. Keywords 'FFS'",
Toast.LENGTH_LONG).show();
} else if (preference == mDHOMD) {
Toast.makeText(getActivity(), "One of the founding Devs on Vanir, Maintainer of Commotio",
Toast.LENGTH_LONG).show();
} else if (preference == mPrimeDirective) {
Toast.makeText(getActivity(), "In house Java Dev. No feature requests. Keywords 'ATTN citizens'. Maintainer of EXODUS",
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
