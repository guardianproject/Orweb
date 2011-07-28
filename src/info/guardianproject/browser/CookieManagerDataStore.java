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

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A data store for the CookieManager
 * @author Connell Gauld
 *
 */
public class CookieManagerDataStore extends SQLiteOpenHelper {
	
	private static final String DB_NAME = "shadow_cookiemanager.db";
	private static final int DB_VERSION = 1;
	private SQLiteDatabase db = null;

	public CookieManagerDataStore(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.db = this.getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE whitelist (_id INTEGER PRIMARY KEY, domain TEXT NOT NULL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE whitelist");
		onCreate(db);
	}

	public ArrayList<String> getWhitelist() {
		ArrayList<String> domains = new ArrayList<String>();
		Cursor c = db.query("whitelist", new String[]{"domain"},
				null, null, null, null, null);
		int rows = c.getCount();
		c.moveToFirst();
		for (int i=0; i<rows; i++) {
			String domain = c.getString(0);
			domains.add(domain);
		}
		c.close();
		return domains;
	}
	
	public void addToWhitelist(String domain) {
		ContentValues v = new ContentValues();
		v.put("domain", domain);
		db.insert("whitelist", null, v);
	}
	
	public void removeFromWhitelist(String domain) {
		db.execSQL("DELETE FROM whitelist WHERE domain=?", new Object[]{domain});
	}
}
