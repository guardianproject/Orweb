/**
 * Shadow - Anonymous web browser for Android devices
 * Copyright (C) 2009 Connell Gauld
 *
 * Thanks to University of Cambridge,
 * 		Alastair Beresford and Andrew Rice
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package info.guardianproject.browser;

import info.guardianproject.browser.R;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
/**
 * Activity for showing the preferences
 * @author Connell Gauld
 *
 */
public class EditPreferences extends PreferenceActivity implements OnDismissListener, OnClickListener {

	PreferenceCategory mWhitelist = null;
	Preference mAddSite = null;
	WhitelistItemPreference mLastSelected = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		mWhitelist = (PreferenceCategory) this.findPreference("pref_whitelist");
		mAddSite = (Preference) this.findPreference("pref_addsite");
		
		populateWhitelist();
	}
	
	private void populateWhitelist() {
		CookieManager c = CookieManager.getInstance();
		
		mWhitelist.removeAll();
		ArrayList<String> v = c.getWhitelist();
		
		int size = v.size();
		for (int i=0; i<size; i++) {
			String site = v.get(i);
			WhitelistItemPreference p = new WhitelistItemPreference(this);
			p.setSite(site);
			p.setTitle(site);
			mWhitelist.addPreference(p);
		}
		
	}
	

	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		
		super.onPreferenceTreeClick(preferenceScreen, preference);
		
		if (preference == mAddSite) {
			AddSiteDialog asd = new AddSiteDialog(this);
			asd.setOnDismissListener(this);
			asd.show();
		} else if (preference instanceof WhitelistItemPreference) {
			mLastSelected = (WhitelistItemPreference)preference;
			AlertDialog.Builder b = new Builder(this);
			b.setTitle(mLastSelected.getSite())
				.setPositiveButton("Delete", this)
				.setNeutralButton("Cancel", null)
				.show();
		}
		
		return false;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		populateWhitelist();
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			if (mLastSelected != null) {
				CookieManager.getInstance().removeFromWhitelist(mLastSelected.getSite());
				populateWhitelist();
			}
		}
		
	}
}
