package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;

public class ChevyCam94Settings extends SettingsPreferenceFragment {

	private Activity mActivity;
	Preference mMe;
	Preference mDonate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.chevy_settings);
        
        mMe = findPreference("aboutvanir_me");
	mDonate = findPreference("aboutvanir_donate");

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mMe) {
			showAboutMeDialog();
        } else if (preference == mDonate) {
			gotoUrl("http://goo.gl/F6UNf");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void gotoUrl(String url) {
        Uri page = Uri.parse(url);
        Intent internet = new Intent(Intent.ACTION_VIEW, page);
        getActivity().startActivity(internet);
    }

    private void showAboutMeDialog() {
        mActivity = getActivity();
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.aboutvanir_me)
			.setMessage(R.string.aboutchevy)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create().show();
    }
}
