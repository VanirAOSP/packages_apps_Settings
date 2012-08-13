/**
 * Shamelessly based on the work of syaoran12
 */

package com.android.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;

public class AboutSettings extends SettingsPreferenceFragment {

        Preference mVanirGithub;
        Preference mVanirIrc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_settings);
        
        mVanirGithub = findPreference("aboutvanir_github");
        mVanirIrc = findPreference("aboutvanir_irc");
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mVanirGithub) {
            gotoUrl("https://github.com/VanirAOSP/");
        } else if (preference == mVanirIrc) {
            gotoUrl("http://webchat.freenode.net/?channels=vanir");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void gotoUrl(String url) {
        Uri page = Uri.parse(url);
        Intent internet = new Intent(Intent.ACTION_VIEW, page);
        getActivity().startActivity(internet);
    }
}
