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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;

/**
 * Represents an individual item in the cache.
 * Intended to be in compliance with the HTTP/1.1 spec:
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
 * @author Connell Gauld
 *
 */
public class CacheObject {

	private HashMap<String, String[]> headers = null;
	private byte[] content = null;
	private int status = 0;
	private Date requestTime = null;
	private Date responseTime = null;
	private Date dateHeader = null;
	private Date expiresHeader = null;
	private long ageHeader = 0;
	private long maxageHeader = -1;
	private boolean cache = true;
	private String url = null;
	
	public CacheObject(String url, HashMap<String, String[]> headers,
					byte[] content, int status, Date requestTime, Date responseTime) {
		this.url = url;
		this.headers = headers;
		this.content = content;
		this.status = status;
		this.requestTime = requestTime;
		this.responseTime = responseTime;
		parseHeaders();
	}
	
	public String getUrl() {
		return url;
	}
	
	public boolean isStale(Date now) {
		
		if (cache == false) return true;
		
		long freshness_lifetime;
		if (maxageHeader != -1) {
			freshness_lifetime = maxageHeader;
		} else {
			// If missing headers don't cache
			if ((expiresHeader == null) || (dateHeader == null)) return true;
			freshness_lifetime = (expiresHeader.getTime() - dateHeader.getTime())/1000;
		}
		
		return (getAge(now) > freshness_lifetime);
	}
	
	public String[] getConditionalHeader() {
		return new String[]{"If-Modified-Since", DateUtils.formatDate(requestTime)};
	}
	
	private long getAge(Date now) {
		
		// Calculated in line with HTTP/1.1 specification:
		// http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.2.3
		
		long apparent_age = 0;
		
		if (dateHeader != null) {
			apparent_age =(responseTime.getTime() - dateHeader.getTime()) / 1000;
			if (apparent_age < 0) apparent_age = 0;
		}
		
		long corrected_received_age;
		if (apparent_age > ageHeader) corrected_received_age = apparent_age;
		else corrected_received_age = ageHeader;
		
		long response_delay = (responseTime.getTime() - requestTime.getTime())/1000;
		long corrected_initial_age = corrected_received_age + response_delay;
		
	    long resident_time = (now.getTime() - responseTime.getTime()) / 1000;
	    return corrected_initial_age + resident_time;

	}
	
	public long getContentLength() {
		if (content != null) return content.length;
		else return 0;
	}
	
	public Map<String, String[]> getHeaders() {
		return headers;
	}
	
	public InputStream getNewInputStream() {
		return new ByteArrayInputStream(content);
	}
	
	public int getStatus() {
		return status;
	}
	
	public long getSize() {
		if (content == null) return 0;
		else return content.length;
	}
	
	private void parseHeaders() {
		// See HTTP/1.1 spec
		// http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
		if (headers.containsKey("expires")) {
			try {
				expiresHeader = DateUtils.parseDate(headers.get("expires")[1]);
			} catch (DateParseException e) {}
		}
		
		if (headers.containsKey("date")) {
			try {
				dateHeader = DateUtils.parseDate(headers.get("date")[1]);
			} catch (DateParseException e) {}
		}
		
		if (headers.containsKey("age")) {
			try {
				ageHeader = Integer.parseInt(headers.get("age")[1]);
			} catch (NumberFormatException e) {}
		}
		
		if (headers.containsKey("cache-control")) {
			String rhs = headers.get("cache-control")[1];
			int pos = rhs.indexOf("max-age=");
			if (pos != -1) {
				String maxageStr = rhs.substring(pos + "max-age=".length());
				try {
					maxageHeader = Integer.parseInt(maxageStr);
				} catch (NumberFormatException e) {}
			} else {
				// Probably Cache-Control: no-cache
				cache = false;
			}
		}
	}
}
