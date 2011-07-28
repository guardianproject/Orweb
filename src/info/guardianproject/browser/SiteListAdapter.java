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
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * List adapter for sites in the blocked cookies dialog
 * @author cmg47
 *
 */
public class SiteListAdapter extends ArrayAdapter<String> implements OnCheckedChangeListener {

	private Context mContext = null;
	private CookieManager mCookieManager = CookieManager.getInstance();
	
	public SiteListAdapter(Context context, List<String> objects) {
		super(context, R.id.siteCheckbox, objects);
		this.mContext = context;
	}

	public View getView(int position, View convertView, android.view.ViewGroup parent) {
		
		View row = convertView;
		
		if (row == null) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			row = inflater.inflate(R.layout.sitelist_item, null);
		}
		
		CheckBox c = (CheckBox)row.findViewById(R.id.siteCheckbox);
		String site = getItem(position);
		
		if (mCookieManager.setCookieForDomain(site)) {
			c.setChecked(true);
		} else {
			c.setChecked(false);
		}
		c.setText(site);
		c.setTag(site);
		c.setOnCheckedChangeListener(this);
		
		return row;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		String site = (String)buttonView.getTag();
		if (isChecked) {
			try {
				mCookieManager.addToWhitelist(site, mContext);
				mCookieManager.acceptBlockedCookies(site);
			} catch (URISyntaxException e) {
				// Nothing we can do
			}
		} else mCookieManager.removeFromWhitelist(site);
	}
}
