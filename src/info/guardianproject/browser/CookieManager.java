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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.cookie.Cookie;

import android.content.Context;
import android.widget.Toast;

public class CookieManager {
	
	public static final int ACCEPT_ALL = 1;
	public static final int WHITELIST = 2;
	public static final int BLOCK_ALL = 3;
	
	
	private class BlockedCookie {
		public String domain;
		public Cookie cookie;
		public String header;
		public String url;
	}

	// Singleton
	private static CookieManager mCookieManager = null;
	
	// Member variables
	private ArrayList<String> mDomainWhitelist = new ArrayList<String>();
	private ArrayList<BlockedCookie> mBlockedCookies = new ArrayList<BlockedCookie>();
	private ArrayList<String> mBlockedCookiesDomains = new ArrayList<String>();
	
	private int mBehaviour = WHITELIST;
	
	private CookieManagerDataStore mDataStore = null;
	
	private CookieManager() {
		// Do nothing
	}
	
	private void openDataStore(Context c) {
		mDataStore = new CookieManagerDataStore(c);
		mDomainWhitelist = mDataStore.getWhitelist();
	}
	
	/**
	 * Get an instance of the cookie manager
	 * @param c application context
	 * @return the cookie manager
	 */
	public static CookieManager getInstance(Context c) {
		CookieManager m = CookieManager.getInstance();
		if (m.mDataStore == null) {
			m.openDataStore(c);
		}
		return mCookieManager;
	}

	/**
	 * Get an instance of the CookieManager.
	 * *Must* have used getInstance(Context c) once with a valid
	 * context before using this version.
	 * @return the cookie manager
	 */
	public static CookieManager getInstance() {
		if (mCookieManager == null) mCookieManager = new CookieManager();
		return mCookieManager;
	}
	
	/**
	 * Set the behaviour of the cookie manager.
	 * @param behaviour one of ACCEPT_ALL, WHITELIST, BLOCK_ALL
	 */
	public void setBehaviour(int behaviour) {
		mBehaviour = behaviour;
	}
	
	/**
	 * Set the behaviour of the cookie manager.
	 * @param behaviour one of "accept", "whitelist", or "block"
	 */
	public void setBehaviour(String behaviour) {
		if ("accept".equals(behaviour)) mBehaviour = ACCEPT_ALL;
		else if ("whitelist".equals(behaviour)) mBehaviour = WHITELIST;
		else mBehaviour = BLOCK_ALL;
	}
	
	/**
	 * Add a site to the cookie whitelist
	 * @param s the site to add
	 * @param c the application context
	 * @throws URISyntaxException
	 */
	public synchronized void addToWhitelist(String s, Context c) throws URISyntaxException {
		
		String host = s;
		
		if (host.length() == 0) throw new URISyntaxException(s, "No host");
		
		if (mDomainWhitelist.contains(host)) return;
		mDomainWhitelist.add(host);
		
		if (mDataStore != null) mDataStore.addToWhitelist(host);
		
		if (c != null) {
			Toast.makeText(c, "Site added to cookie whitelist", Toast.LENGTH_SHORT).show();
		}
		
	}
	
	/**
	 * Return an array of the sites in the whitelist
	 * @return ArrayList<String> of sites
	 */
	public synchronized ArrayList<String> getWhitelist() {
		return new ArrayList<String>(mDomainWhitelist);
	}
	
	/**
	 * Remove a site from the whitelist
	 * @param s the site to remove
	 */
	public synchronized void removeFromWhitelist(String s) {
		mDomainWhitelist.remove(s);
		if (mDataStore != null) mDataStore.removeFromWhitelist(s);
	}
	
	/**
	 * Determine whether to allow a cookie to be set for a domain
	 * @param domain the domain to check
	 * @return true if cookie should be set otherwise false
	 */
	public synchronized boolean setCookieForDomain(String domain) {
		if (mBehaviour == BLOCK_ALL) return false;
		if (mBehaviour == ACCEPT_ALL) return true;
		return mDomainWhitelist.contains(domain);
	}
	
	/**
	 * Register that a cookie has been blocked
	 * @param c the cookie that was blocked
	 * @param header the set-cookie header of the cookie
	 * @param url the address that added the cookie
	 */
	public synchronized void cookieBlocked(Cookie c, String header, String url) {
		
		String domain = c.getDomain();
		
		//Log.i("CookieManager", "Cookie blocked from: " + domain);
		
		BlockedCookie b = new BlockedCookie();
		b.cookie = c;
		b.header = header;
		b.domain = domain;
		b.url = url;
		mBlockedCookies.add(b);
		
		if (!mBlockedCookiesDomains.contains(domain))
			mBlockedCookiesDomains.add(domain);
	}
	
	/**
	 * Determine if there are any currently blocked cookies
	 * @return true if there are currently blocked cookies
	 */
	public synchronized boolean hasBlockedCookies() {
		if (mBlockedCookies.size()>0) return true;
		else return false;
	}
	
	/**
	 * Clear all currently blocked cookies
	 */
	public synchronized void clearBlockedCookies() {
		mBlockedCookies.clear();
		mBlockedCookiesDomains.clear();
	}
	
	/**
	 * Get a list of the domains of the currently blocked cookies
	 * @return a List<String> of the domains
	 */
	public synchronized List<String> getBlockedCookiesDomains() {
		return new ArrayList<String>(mBlockedCookiesDomains);
	}
	
	/**
	 * Accept all currently blocked cookies for a domain
	 * @param domain the domain to accept blocked cookies for
	 */
	public synchronized void acceptBlockedCookies(String domain) {
		
		//Log.d("CookieManager", "Accepting blocked cookies for: " + domain);
		
		android.webkit.CookieManager c = android.webkit.CookieManager.getInstance();
		
		int size = mBlockedCookies.size();
		for (int i=0; i<size; i++) {
			BlockedCookie thisCookie = mBlockedCookies.get(i);
			if (thisCookie.domain.equals(domain))
				c.setCookie(thisCookie.url, thisCookie.header);
		}
	}
	
	/**
	 * Determine whether to send cookies to a URL
	 * @param url the URL to check
	 * @return true if cookies should be sent
	 */
	public synchronized boolean sendCookiesFor(String url) {
		
		if (mBehaviour == BLOCK_ALL) return false;
		if (mBehaviour == ACCEPT_ALL) return true;
		
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			return false;
		}
		String host = u.getHost();
		
		int size = mDomainWhitelist.size();
		for (int i=0; i<size; i++) {
			String whitelist = mDomainWhitelist.get(i);
			if (host.endsWith(whitelist)) return true;
		}
		return false;
	}
	
	/**
	 * Clear all cookies
	 */
	public synchronized void clearAllCookies() {
		android.webkit.CookieManager c = android.webkit.CookieManager.getInstance();
		c.removeAllCookie();
	}
}
