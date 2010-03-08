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

package guardianproject.browser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.webkit.PluginData;

/**
 * Provides HTTP request functionality for the Shadow browser.
 * Performs requests through a SOCKS proxy.
 * @author cmg47
 *
 */
public class AnonProxy {
	
	private int mPort = 0;
	private DefaultHttpClient mClient = null;
	
	// The PostProcessor is used to rewrite POST forms as GET
	private PostProcessor mPostProcessor = new PostProcessor();
	private CacheManager mCacheManager = CacheManager.getCacheManager();
	private BrowserCompatSpec mCookieSpec = new BrowserCompatSpec();
	private CookieManager mCookieManager = CookieManager.getInstance();
	
	// Settings
	private boolean mSendReferrer = true;
	
	private ArrayList<HttpRequestBase> mLatestRequests = new ArrayList<HttpRequestBase>();
	
	/**
	 * Set the port for the HTTP proxy
	 * @param port
	 */
	public void setPort(int port) {
		
		if (this.mPort != port) {
		
			// Proxy port has changed so set up appropriate HttpClient
			this.mPort = port;
			
			/*
			HttpHost proxy = new HttpHost("127.0.0.1", 8118, "http");
			
			//mClient.getHostConfiguration().setProxy("myproxyhost", 8080);
			 
			SchemeRegistry supportedSchemes = new SchemeRegistry();
			
			 supportedSchemes.register(new Scheme("http", 
		                PlainSocketFactory.getSocketFactory(), 80));
		        supportedSchemes.register(new Scheme("https", 
		                SSLSocketFactory.getSocketFactory(), 443));
		     
			
	        // prepare parameters
	        HttpParams params = new BasicHttpParams();
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, "UTF-8");
	        HttpProtocolParams.setUseExpectContinue(params, true);

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);
			*/
			
	        mClient = new DefaultHttpClient();
	        
	       /// mClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	        mClient.setRoutePlanner(new HttpRoutePlanner() {

	            public HttpRoute determineRoute(
	                    HttpHost target, 
	                    HttpRequest request, 
	                    HttpContext context) throws HttpException {
	                try {
						return new HttpRoute(target, InetAddress.getLocalHost(),  new HttpHost("127.0.0.1", 8118), 
						        "https".equalsIgnoreCase(target.getSchemeName()));
						        //RouteInfo.TunnelType.PLAIN, RouteInfo.LayerType.PLAIN);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return null;
					}
	            }
	            
	        });

		}
	}
	
	/**
	 * Perform an HTTP request
	 * @param url the URL to get
	 * @param headers the request headers
	 * @return structure containing the response
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public PluginData get(String url, Map<String, String> headers) throws Exception {
		
		//if (true) throw new Exception("Aaaaahh");
		//Log.w("AnonProxy", "Using port " + mPort);
		// If the port hasn't been set don't allow any requests to be made
		if (mClient == null) throw new IOException();
		
		boolean isPost = false;
		HttpRequestBase g = null;
		// POST processing
		try {
			boolean makePost = false;
			URI u = new URI(url);
			String query = u.getQuery();
			
			if (query != null) {
				// There is a querystring. Search for magic POST identifier
				String[] pairs = query.split("&");
				for (int i=0; i<pairs.length; i++) {
					String[] thisPair = pairs[i].split("=");
					if (thisPair.length == 2) {
						if (mPostProcessor.isPostProcessorIdentifier(thisPair[0], thisPair[1])) {
							makePost = true;
							break;
						}
					}
				}
				// If this was supposed to be a POST, turn it into one
				if (makePost) {
					HttpPost p = new HttpPost(url);
					p.setEntity(new StringEntity(query));
					g = p;
					isPost = true;
				}
			}
		} catch (URISyntaxException e1) {
			// Not much we can do but just send the request...
		}
		
		CacheObject cacheObj = null;
		
		Date requestTime = new Date();
		
		// If we're not doing a POST, we're doing a GET
		if (g == null) {
			// Check if this is in the cache
			// Never cache POST requests
			cacheObj = mCacheManager.getCacheObject(url);
			if (cacheObj != null) {
				if (!cacheObj.isStale(requestTime)) {
					// Can serve directly from cache
					//Log.i("AnonProxy", "Served directly from cache" + url);
					return new PluginData(cacheObj.getNewInputStream(), 
							cacheObj.getContentLength(), 
							cacheObj.getHeaders(),
							cacheObj.getStatus());	
				}
			}
			g = new HttpGet(url);
		}
		
		synchronized(mLatestRequests) {
			mLatestRequests.add(g);
		}
		// Check cookie sending
		boolean acceptCookies = mCookieManager.sendCookiesFor(url);
		
		// Add headers
		if (headers != null) {
			Iterator<Map.Entry<String, String>> i = headers.entrySet().iterator();
			while(i.hasNext()) {
				Map.Entry<String, String> entry = i.next();
				String key = entry.getKey();
				String lowercaseKey = key.toLowerCase();
				if (lowercaseKey.equals("cookie")) {
					if (!acceptCookies) {
						//Log.i("AnonProxy", "Not sending cookie: " + entry.getValue());
						break;
					}
				} else if (lowercaseKey.equals("referer")) {
					if (!mSendReferrer) {
						//Log.d("AnonProxy", "Referrer stripped");
						break;
					}
				}
				//Log.d("AnonProxy", entry.getKey() + ": " + entry.getValue());
				g.setHeader(entry.getKey(), entry.getValue());
			}
		}
		
		// Set conditional headers if required by the cache
		if ((!isPost) && (cacheObj != null)) {
			String[] conditionalHeader = cacheObj.getConditionalHeader();
			g.setHeader(conditionalHeader[0], conditionalHeader[1]);
		}
		
		HttpResponse r;
		mClient.getCookieStore().clear();

		r = mClient.execute(g);
		//Log.d("AnonProxy", "Execution done");
		
		URI requestUri = g.getURI();
		
		
		int port = requestUri.getPort();
		if (port == -1) port = 80;
		// TODO fix last parameter for HTTPS
		CookieOrigin origin = new CookieOrigin(requestUri.getHost(), port, requestUri.getPath(), false);
		
		// Package up the response headers for PluginData
		HashMap<String, String[]> rpHeadersMap = new HashMap<String, String[]>();
		Header[] rpHeaders = r.getAllHeaders();
		for (int i=0; i<rpHeaders.length; i++) {
			Header c = rpHeaders[i];
			String[] value = new String[2];
			value[0] = c.getName();
			value[1] = c.getValue();
			
			String lowerCaseHeader = value[0].toLowerCase();
			
			boolean returnThisHeader = true;
			if ((lowerCaseHeader.equals("set-cookie"))
					||(lowerCaseHeader.equals("set-cookie2"))) {
				try {
					List<Cookie> cookies = mCookieSpec.parse(c, origin);
					int size = cookies.size();
					for (int z = 0; z<size; z++) {
						Cookie cookie = cookies.get(z);
						if (!mCookieManager.setCookieForDomain(cookie.getDomain())) {
							returnThisHeader = false;
							mCookieManager.cookieBlocked(cookie, value[1], url);
						}
					}
				} catch (MalformedCookieException e1) {
					returnThisHeader = false;
				}
			}
			if (returnThisHeader)
				rpHeadersMap.put(lowerCaseHeader, value);
		}
		
		StatusLine stat = r.getStatusLine();
		//Log.d("AnonProxy", "Statusline got");
		
		if (stat.getStatusCode() == 304) {
			//Log.i("AnonProxy", "Not modified so serving from cache: " + url);
			// Not modified so serve from cache
			return new PluginData(cacheObj.getNewInputStream(), 
					cacheObj.getContentLength(), 
					cacheObj.getHeaders(),
					cacheObj.getStatus());
		}
		
		HttpEntity e = r.getEntity();
		
		InputStream content = null;
		Header type = null;
		long contentLength = 0;
		if (e != null) {
			
			type = e.getContentType();
			content = e.getContent();
			
			// Perform POST rewriting, if appropriate
			if (type != null) {
				if (PostProcessor.canProcessMime(type.getValue())) {
					content = mPostProcessor.rewriteIncoming(content);
				}
			}

			ByteArrayOutputStream outS = new ByteArrayOutputStream();
			InputStream inS = content;
			byte[] buffer = new byte[498];
			int read = 0;
			while (read != -1) {
				outS.write(buffer, 0, read);
				read = inS.read(buffer);
			}
			// Grab back all of the data as an array
			buffer = outS.toByteArray();
			
			contentLength = buffer.length;
			Date responseTime = new Date();
			// Let's cache it
			//Log.i("AnonProxy", "Adding to cache: " + url);
			cacheObj = new CacheObject(url, rpHeadersMap, buffer, stat.getStatusCode(), requestTime, responseTime);
			content = cacheObj.getNewInputStream();
			mCacheManager.addCacheObject(url, cacheObj);
		}
		
		return new PluginData(content, contentLength, rpHeadersMap, stat.getStatusCode());
	}
	
	public void stop() {
		synchronized(mLatestRequests) {
			int size = mLatestRequests.size();
			for (int i=0; i<size; i++) {
				try {
					mLatestRequests.get(i).abort();
				} catch (Exception e) {
					// Well, we tried
				}
			}
			mLatestRequests.clear();
		}
	}
	
	public void setSendReferrer(boolean value) {
		this.mSendReferrer = value;
	}
	

}
