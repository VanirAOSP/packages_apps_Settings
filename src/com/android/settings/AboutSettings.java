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
		Preference mDonateSonic;
		Preference mDonateDho;
		Preference mDonateNuke;
		Preference mDonatePd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_settings);
        
        mVanirGithub = findPreference("aboutvanir_github");
        mVanirIrc = findPreference("aboutvanir_irc");
		mDonateSonic = findPreference("aboutvanir_sonicxml");
		mDonateDho = findPreference("aboutvanir_dho");
		mDonateNuke = findPreference("aboutvanir_nuke");
		mDonatePd = findPreference("aboutvanir_pd");
      
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mVanirGithub) {
            gotoUrl("https://github.com/VanirAOSP/");
        } else if (preference == mVanirIrc) {
            gotoUrl("http://webchat.freenode.net/?channels=vanir");
        } else if (preference == mDonateSonic) {
			gotoUrl("http://goo.gl/8ym3R");
        } else if (preference == mDonateDho) {
			gotoUrl("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=9Z79J3J6JFQ4N&lc=US&item_name=DHO&item_number=HYBRYD&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted");
        } else if (preference == mDonateNuke) {
			gotoUrl("https://www.paypal.com/us/cgi-bin/webscr?cmd=_flow&SESSION=-Qh6zFwtaaYbi6CfgwQxLNagH-1EuyEkTYOvFWPS3CPqOImH17L6BqC92EW&dispatch=5885d80a13c0db1f8e263663d3faee8da6a0e86558d6153d8812cd76bf2fd83f");
        } else if (preference == mDonatePd) {
			gotoUrl("https://www.paypal.com/us/cgi-bin/webscr?cmd=_flow&SESSION=XYjAOnnFhYJGGwLlJRMQ1RNXOxeoy0EZQ6Do8ntwBWV8I6IaWgyAYSm9hyq&dispatch=5885d80a13c0db1f8e263663d3faee8d7283e7f0184a5674430f290db9e9c846");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void gotoUrl(String url) {
        Uri page = Uri.parse(url);
        Intent internet = new Intent(Intent.ACTION_VIEW, page);
        getActivity().startActivity(internet);
    }
}
