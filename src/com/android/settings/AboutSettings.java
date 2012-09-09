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

public class AboutSettings extends SettingsPreferenceFragment {

	private Activity mActivity;
        Preference mVanirGerrit;
        Preference mVanirGithub;
        Preference mVanirIrc;
        Preference mVanirChangelog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_settings);
        
		mVanirGerrit = findPreference("aboutvanir_gerrit");
        mVanirGithub = findPreference("aboutvanir_github");
        mVanirIrc = findPreference("aboutvanir_irc");
        mVanirChangelog = findPreference("aboutvanir_changelog");
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mVanirGerrit) {
            gotoUrl("http://vaniraosp.goo.im");
        } else if (preference == mVanirGithub) {
            gotoUrl("https://github.com/VanirAOSP/");
        } else if (preference == mVanirIrc) {
            gotoUrl("http://webchat.freenode.net/?channels=vanir");
        } else if (preference == mVanirChangelog) {
		showChangelog();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void gotoUrl(String url) {
        Uri page = Uri.parse(url);
        Intent internet = new Intent(Intent.ACTION_VIEW, page);
        getActivity().startActivity(internet);
    }

    private void showChangelog() {
        mActivity = getActivity();
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.aboutvanir_changelog_title)
			.setMessage(R.string.aboutvanir_changelog)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create().show();
    }

}
