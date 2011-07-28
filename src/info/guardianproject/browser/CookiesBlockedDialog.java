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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ListView;

/**
 * A dialog for listing the blocked cookies
 * @author Connell Gauld
 *
 */
public class CookiesBlockedDialog extends AlertDialog implements OnClickListener {
	
	private ListView mSiteList = null;

	protected CookiesBlockedDialog(Context context) {
		super(context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setInverseBackgroundForced(true);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
		String behaviour = prefs.getString("pref_cookiebehaviour", "whitelist");
		
		setTitle("Cookies Blocked");
		setButton(BUTTON_NEUTRAL, "Close", this);
		
		if (behaviour.equals("block")) {
			setView(getLayoutInflater().inflate(R.layout.dialog_cookiesblocked_block, null));
			super.onCreate(savedInstanceState);
		} else {
			setView(getLayoutInflater().inflate(R.layout.dialog_cookiesblocked_whitelist, null));
			super.onCreate(savedInstanceState);
			
			mSiteList = (ListView)findViewById(R.id.cookieSiteList);
			SiteListAdapter adapter = new SiteListAdapter(getContext(), CookieManager.getInstance().getBlockedCookiesDomains());
			mSiteList.setAdapter(adapter);
		}
		
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// Nothing to be done
	}

}
