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

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the cache of CacheObjects.
 * @author Connell Gauld
 *
 */
public class CacheManager {

	private static CacheManager mCacheManagerInstance = null;

	public static CacheManager getCacheManager() {
		if (mCacheManagerInstance == null) mCacheManagerInstance = new CacheManager();
		return mCacheManagerInstance;
	}
	
	private CacheManager() {
		// Nothing to be done
	}
	
	private static final float CACHE_SIZE_PROP = 0.75f;
	private static final long MAX_CACHE_SIZE = 6 * 1024 * 1024; // 6 MB

	HashMap<String, CacheObject> mCacheObjects = new HashMap<String, CacheObject>();

	// LRU stack for cache objects
	ConcurrentLinkedQueue<CacheObject> mLeastUsedIndex = new ConcurrentLinkedQueue<CacheObject>();

	private long mCacheSize = 0;

	/**
	 * Returns a cached object with the given url.
	 *
	 * @param url
	 *            the url to get
	 * @return the CacheObject or null if the object with the given url is not
	 *         in the cache
	 */
	public synchronized CacheObject getCacheObject(String url) {
		CacheObject o = mCacheObjects.get(url);
		if (o != null) {
			// Remove from wherever it is in the queue
			mLeastUsedIndex.remove(o);
			// Add it to the end
			mLeastUsedIndex.add(o);
		}
		return o;
	}

	/**
	 * Returns the amount of space available for caching.
	 * @return available space in bytes
	 */
	private long getAvailableCache() {

		long availableToAllocate = (long) ((float) Runtime.getRuntime()
				.freeMemory() * CACHE_SIZE_PROP);
		long availableBeforeMax = MAX_CACHE_SIZE - mCacheSize;

		// Return the smaller of the two
		if (availableBeforeMax > availableToAllocate)
			return availableToAllocate;
		else
			return availableBeforeMax;
	}

	/**
	 * Add an object to the cache.
	 * @param url the url corresponding to the object
	 * @param c the object itself
	 */
	public synchronized void addCacheObject(String url, CacheObject c) {

		long availableCache = getAvailableCache();

		long size = c.getSize();
		// If the cache is full then clear some items
		while (size > availableCache) {
			if (!clearOneItemFromCache())
				return; // Clear item failed - don't cache this obj
		}

		// Put the object in the hashmap and the LRU queue
		mCacheObjects.put(url, c);
		mLeastUsedIndex.add(c);
		mCacheSize += c.getSize();
		//Log.d("CacheManager", "Cache now at size: " + mCacheSize + " bytes");
		//Log.d("CacheManager", "Remaining available cache: " + availableCache
		//		+ " bytes");
	}

	/**
	 * Removes the least recently used item from the cache.
	 * @return true if an item was successfully cleared otherwise false
	 */
	private boolean clearOneItemFromCache() {
		try {
			CacheObject o = mLeastUsedIndex.poll();
			if (o == null)
				return false;
			//Log.d("CacheManager", "Cache too large. Clearing one item: "
			//		+ o.getUrl());
			mCacheSize -= o.getSize();
			mCacheObjects.remove(o.getUrl());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Empties the cache.
	 */
	public synchronized void clearCache() {
		mCacheObjects.clear();
		mCacheSize = 0;
		mLeastUsedIndex.clear();
		//Log.i("CacheManager", "Cache cleared");
	}
}
