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
import android.app.Dialog;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Preference for selecting Yes or No
 * @author Connell Gauld
 *
 */
public abstract class BrowserYesNoPreference extends DialogPreference implements OnClickListener {
	
	private Button mButtonOk = null;
	private Button mButtonCancel = null;

	public BrowserYesNoPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}


	public BrowserYesNoPreference(Context context, AttributeSet attrs) {
		super(context, attrs, R.style.BrowserYesNoPreference);
		setDialogLayoutResource(R.layout.yesnopreference_dialog);
	}

	@Override
	protected View onCreateDialogView() {
		View v = super.onCreateDialogView();
		
		mButtonOk = (Button)v.findViewById(R.id.buttonOK);
		mButtonCancel = (Button)v.findViewById(R.id.buttonCancel);
		
		mButtonOk.setOnClickListener(this);
		mButtonCancel.setOnClickListener(this);
		
		return v;
	}

	@Override
	public void onClick(View v) {
		Dialog d = this.getDialog();
		if (d == null) return;
		
		switch (v.getId()) {
		case R.id.buttonOK:
			onOk();
			d.dismiss();
			break;
		case R.id.buttonCancel:
			d.cancel();
			break;
		}
		return;
	}

	public abstract void onOk();
}
