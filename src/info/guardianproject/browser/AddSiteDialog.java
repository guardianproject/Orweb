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

import java.net.URISyntaxException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.EditText;

/**
 * Dialog for adding a site to the cookie whitelist
 * @author Connell Gauld
 *
 */
public class AddSiteDialog extends AlertDialog implements OnClickListener {

	public static final int SAVE_BUTTON = BUTTON1;
	
	private EditText mUrl = null;
	
	public AddSiteDialog(Context context) {
		super(context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		setInverseBackgroundForced(true);
		setView(getLayoutInflater().inflate(R.layout.dialog_addsite, null));
		setTitle("Add site to cookie whitelist");
		
		setButton("Save", this);
		setButton2("Cancel", this);
		
		super.onCreate(savedInstanceState);
		
		mUrl = (EditText)findViewById(R.id.site_entry);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == SAVE_BUTTON) {
			try {
				CookieManager.getInstance().addToWhitelist(mUrl.getText().toString(), getContext());
			} catch (URISyntaxException e) {
				AlertDialog.Builder b = new Builder(getContext());
				b.setTitle("Invalid site");
				b.setMessage("The text you entered wasn't a valid site");
				b.setNeutralButton("OK", null);
				b.create().show();
				return;
			}
		}
	}

}
