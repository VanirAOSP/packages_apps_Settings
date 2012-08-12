package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;


import com.android.settings.R;

public class SonicSettings extends SettingsPreferenceFragment {

	private Activity mActivity;
    Preference mSonicMe;
    Preference mSonicTwitter;
	Preference mSonicDonate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sonic_settings);
        
        mSonicMe = findPreference("aboutvanir_me");
        mSonicTwitter = findPreference("aboutvanir_twitter");
		mSonicDonate = findPreference("aboutvanir_donate");
      
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mSonicMe) {
			showAboutMeDialog();
        } else if (preference == mSonicTwitter) {
            gotoUrl("https://twitter.com/sonicxml1");
        } else if (preference == mSonicDonate) {
			gotoUrl("http://goo.gl/8ym3R");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void gotoUrl(String url) {
        Uri page = Uri.parse(url);
        Intent internet = new Intent(Intent.ACTION_VIEW, page);
        getActivity().startActivity(internet);
    }

    private void showAboutMeDialog() {
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.aboutvanir_me)
			.setMessage(R.string.aboutsonic)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create().show();
    }
}
