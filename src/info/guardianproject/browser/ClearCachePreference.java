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

import android.content.Context;
import android.util.AttributeSet;

/**
 * Clear the cache preference
 * @author Connell Gauld
 *
 */
public class ClearCachePreference extends BrowserYesNoPreference {

	public ClearCachePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	

	@Override
	public void onOk() {
		CacheManager.getCacheManager().clearCache();
	}

}
