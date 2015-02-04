/**
 * Orweb - anonymous, secure, privacy-oriented browser for Android devices
 *
 * Based heavily upon:
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


import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpHeaders;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;

/**
 * The main browser activity
 * @author Connell Gauld
 *
 */

//public class Browser extends Activity implements UrlInterceptHandler,
	//	OnClickListener {
public class Browser extends ActionBarActivity implements
	OnClickListener {

	
	// UI elements
	private BrowserWebView mWebView = null;
	private LinearLayout mCookieIcon = null;

	// Misc
	private Drawable mGenericFavicon = null;
	private boolean mInLoad = false;
	private Menu mMenu = null;
	private boolean mLastIsTorActive = true;
	private CookieDomainManager mCookieManager = null;

	private boolean mShowReferrer = false;
	private boolean mClearHistory = false;
	private boolean mDoJavascript = false;
	private boolean mShowImages = false;
	
	private OrbotHelper mOrbotHelper = null;
	
	private SharedPreferences mPrefs = null;
	
	public static String DEFAULT_PROXY_HOST = "localhost";
	public static String DEFAULT_PROXY_PORT = "8118";
	public static String DEFAULT_PROXY_TYPE = "HTTP";
	
	private String mCharSet = "utf-8";
	private String mLanguage = "en-US";
	
	private Map<String,String> mHeaderOverride;
	
	private String mProxyHost = DEFAULT_PROXY_HOST;
	private int mProxyPort = Integer.parseInt(DEFAULT_PROXY_PORT);
	private String mProxyType = DEFAULT_PROXY_TYPE;
	
	public static String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0";
	public static String DEFAULT_SEARCH_ENGINE = "http://3g2upl4pq6kufc4m.onion/?q=";
	public static String DEFAULT_SEARCH_ENGINE_NOJS = "https://duckduckgo.com/html?q=";
	
	public static String DEFAULT_HEADER_ACCEPT = "text/html, */* ISO-8859-1,utf-8;q=0.7,*;q=0.7 gzip,deflate en-us,en;q=0.5";
		
	private static String ABOUT_URL = "https://guardianproject.info/apps/orweb";
	
	private Handler mLoadHandler = new Handler ()
	{

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			String url = msg.getData().getString("url");
	
			if (url != null)
			{
				
				url = smartUrlFilter(url);
				
				mWebView.setBlockedCookies(mCookieManager.hasBlockedCookies());
				
				mWebView.loadUrl(url, mHeaderOverride);
				
			}
			else if (msg.what == 1) //install Orbot
			{
				mOrbotHelper.promptToInstall(Browser.this);
			}
			else if (msg.what == 2) //start Orbot
			{
				mOrbotHelper.requestOrbotStart(Browser.this);
			}
		}

	};
	
	/**
	private void doLoadDataOldStyle (String url) throws IOException
	{

		 String mimeType = null;
		 String encoding = null;
		 InputStream isData = null;
		 
		 HttpGet hget = new HttpGet(url);
			
		 initHeaders(hget);
			
		 mHttpClient = getHttpClient();
		mHttpClient.useProxy(true,mProxyType,mProxyHost, mProxyPort);
		
		HttpResponse response = mHttpClient.execute(hget);

		
		HttpEntity respEntity = response.getEntity();
		if (response.getStatusLine().getStatusCode() == 200)
		{
			if (respEntity.getContentType() != null)
			{
     			 mimeType = respEntity.getContentType().getValue();
			 
			    if (!mDoJavascript && mimeType.contains("javascript"))
				{
			    	return;
				}
			    else if (!mShowImages && mimeType.contains("image"))
				{
			    	return;
				}
			}
			
			 if (respEntity.getContentEncoding() != null)
				 encoding = respEntity.getContentEncoding().getValue();
			 
			  String mimeParts[] = mimeType.split(";");
				
			 if (mimeParts.length>1)
			 {
				 mimeType = mimeParts[0].trim();
				 mCharSet = mimeParts[1].trim().split("=")[1];								 
			 }
		 

			 if (encoding == null)
				 encoding = mCharSet;
			 
			 isData = respEntity.getContent();
	
			 mWebView.loadDataWithBaseURL(url, removeUnproxyeableElements(isData, mCharSet), mimeType, encoding, url);
		}
	
		
	}*/
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		super.onCreate(savedInstanceState);
		
		mPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		//check if Orbot is installed running
		mOrbotHelper = new OrbotHelper(this.getApplicationContext());

		// Allow search to start by just typing
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.main);

		/*
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.icon_orweb_small);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		*/
		
		mPrefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener ()
		{

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				
				initSettings();
				
			}
			
		});
		

		initUI();
		initSettings();

		CacheManager.getCacheManager().setBrowser(this);

		setProxy();	
		
		boolean handled = handleIntent(getIntent());
		
		if (!handled)
		{
			Message msg = new Message();

			String starturl = mPrefs.getString(getString(R.string.pref_homepage),
					getString(R.string.default_homepage));

			msg.getData().putString("url", starturl);
			mLoadHandler.sendMessage(msg);
		}
		
		
	}

	
	private void initUI ()
	{
		// Grab UI elements
		mWebView = (BrowserWebView) findViewById(R.id.WebView);
		
		mWebView.setOnTouchListener(new OnTouchListener () {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				/*
				if (mWebView.getScrollY() > 10)
					Browser.this.getSupportActionBar().hide();
				else
					Browser.this.getSupportActionBar().show();
				*/
				
				return false;
			}
		
		});
		//mNoTorLayout = (LinearLayout) findViewById(R.id.NoTorLayout);
		//mStartTor = (Button) findViewById(R.id.StartTor);
		
		mCookieIcon = (LinearLayout) findViewById(R.id.CookieIcon);
		//mTorStatus = (TextView) findViewById(R.id.torStatus);
		mCookieIcon.setOnClickListener(this);

		
		// Misc
		mGenericFavicon = getResources().getDrawable(
				R.drawable.app_web_browser_sm);

		
		// Set up UI elements
		//mStartTor.setOnClickListener(this);
		mWebView.setWebViewClient(mWebViewClient);
		mWebView.setWebChromeClient(mWebViewChrome);
		
		mWebView.setDownloadListener(new DownloadListener() {
	        @Override
	        public void onDownloadStart(final String url, String userAgent,
	                String contentDisposition, final String mimetype,
	                long contentLength) {
	        
	        	final AlertDialog.Builder downloadDialog = new AlertDialog.Builder(Browser.this);
		        downloadDialog.setTitle(info.guardianproject.browser.R.string.title_download_manager);
		        downloadDialog.setMessage(getString(info.guardianproject.browser.R.string.prompt_would_you_like_to_download_this_file_) + '\n' + mimetype + '\n' + url);
		        downloadDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialogInterface, int i) {
		            	 
		            	doProxiedDownload (url, mimetype);
		            	
		        		 dialogInterface.dismiss();
		            }
		        });
		        downloadDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialogInterface, int i) {
		            }
		        });
		        
		        downloadDialog.show();
		         
		      
	        }
	      }
		);

		mWebView.clearFormData();
		mWebView.clearCache(true);
		mWebView.clearHistory();
		
		
	}
	
		private void doProxiedDownload (String url, String mimetype)
		{
			   
        
    		Uri uriOriginal = Uri.parse(url);
    		
    		String filename = java.util.UUID.randomUUID().toString(); //generate random name
    		
    		String[] fileparts = uriOriginal.getEncodedPath().split("\\.");
    		if (fileparts.length > 0)
    		{
    			filename += "." + fileparts[fileparts.length-1];
    		}
    		
    		String newUrl = "http://localhost:9999/" + filename + "?url=" + URLEncoder.encode(url);
    		Uri uriDownload = Uri.parse(newUrl);
    		
    		boolean doStream = false;
    		
    		 try
    		 {

    			initDownloadManager();
    				
    			 if (doStream)
    			 {
    				 Intent intent = new Intent(Intent.ACTION_VIEW);
    			    	//String metaMime = mimeType.substring(0,mimeType.indexOf("/")) + "/*";
    			    	intent.setDataAndType(uriDownload, mimetype);
    			   // 	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    			   	 	startActivity(intent);
    			 }
    			 else
    			 {
    				 doDownloadManager(uriDownload, mimetype, getString(R.string.app_name) + ": " + uriOriginal.getLastPathSegment());
    			 }
    		 }
    		 catch (Exception e)
    		 {
    			 Log.e("Orweb","problem downloading: " + uriOriginal,e);
    		 }
        	
        
	}
	
	
	private ValueCallback<Uri> mUploadMessage;
	private final static int FILECHOOSER_RESULTCODE = 1;

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent intent) {
		
		if (mWebView != null)
			mWebView.reload();
		
		
	    if (requestCode == FILECHOOSER_RESULTCODE) {
	        if (null == mUploadMessage)
	            return;
	        Uri result = intent == null || resultCode != RESULT_OK ? null
	                : intent.getData();
	        mUploadMessage.onReceiveValue(result);
	        mUploadMessage = null;

	    }
	}

	@SuppressLint("NewApi")
	public void initSettings ()
	{

		if (mCookieManager == null)
		{
			CookieSyncManager.createInstance(this).stopSync();
			mCookieManager = CookieDomainManager.getInstance(this);
		}
		
		mShowReferrer = mPrefs.getBoolean("pref_sendreferrer", false);
		mClearHistory = mPrefs.getBoolean("pref_clearhistory", false);

		mWebView.getSettings().setPluginState(PluginState.OFF);
		
	
		mShowImages = mPrefs.getBoolean("pref_images", true);
		
		mWebView.getSettings().setLoadsImagesAutomatically(mShowImages);
		
		mWebView.setBlockedCookiesView(mCookieIcon);			

		String ua = mPrefs.getString("pref_user_agent", DEFAULT_USER_AGENT);
		mWebView.getSettings().setUserAgentString(ua);
		mCookieManager.setBehaviour(mPrefs.getString("pref_cookiebehaviour",
				"accept"));
		
		mDoJavascript = 
				mPrefs.getBoolean(getString(R.string.pref_javascript), false);
		mWebView.getSettings().setJavaScriptEnabled(mDoJavascript);
		
		
		if (Build.VERSION.SDK_INT >= 16)
		{

			mWebView.getSettings().setAllowFileAccessFromFileURLs(false);
			mWebView.getSettings().setAllowUniversalAccessFromFileURLs(false);
			
		}
		
		if (Build.VERSION.SDK_INT >= 17)
		{
			mWebView.getSettings().setMediaPlaybackRequiresUserGesture(true);
			
			
		}

		mHeaderOverride = new HashMap<String,String>();

		mHeaderOverride.put("User-Agent",DEFAULT_USER_AGENT);
		mHeaderOverride.put("Accept", DEFAULT_HEADER_ACCEPT);
		

		if (!mShowReferrer)
			mHeaderOverride.put("Referer","");
		
		mHeaderOverride.put("Accept-Language", mLanguage);
		mHeaderOverride.put("Language", mLanguage);
		
		mHeaderOverride.put("x-requested-with","");
		mHeaderOverride.put("Authentication","");
		mHeaderOverride.put("Signature","");
		
		
		
	}
	


	
	

	@Override
	protected void onDestroy() {
		
		
		unloadDownloadManager ();

		clearCachedData();
		
		// mWebView.destroy();
		 
		 super.onDestroy();
	}
	
	public void clearCachedData ()
	{


		mWebView.clearFormData();
		mWebView.clearCache(true);
		mWebView.clearHistory();
		
		
		CookieManager.getInstance().removeAllCookie();
		mCookieManager.clearAllCookies();
		
		deleteDatabase("webview.db");
		deleteDatabase("webviewCache.db");
		deleteDatabase("webviewCookies.db");
		
		deleteDatabase("webviewCookiesChromium.db");
		
		
		try {
		
			ArrayList<String> cmds = new ArrayList<String>();
			
			cmds.add("rm -rf /data/data/info.guardianproject.browser/cache/webviewCacheChromium");
			cmds.add("rm -rf /data/data/info.guardianproject.browser/cache/webviewCacheChromiumStaging");
			cmds.add("rm -rf /data/data/info.guardianproject.browser/cache/webviewCache");
			
			doCmds(cmds);
		} catch (Exception e) {
			Log.e("Orweb","error clearing cache data",e);
		}
		

	}
	
	public static void doCmds(List<String> cmds) throws Exception {
	    Process process = Runtime.getRuntime().exec("sh");
	    DataOutputStream os = new DataOutputStream(process.getOutputStream());

	    for (String tmpCmd : cmds) {
	            os.writeBytes(tmpCmd+"\n");
	    }

	    os.writeBytes("exit\n");
	    os.flush();
	    os.close();

	    process.waitFor();
	}    

	/*
	 * Set the title bar icon to the supplied bitmap. Yoinked from the Android
	 * browser
	 */
	private void setFavicon(Bitmap icon) {
		Drawable[] array = new Drawable[2];
		PaintDrawable p = new PaintDrawable(Color.WHITE);
		p.setCornerRadius(3f);
		array[0] = p;
		if (icon == null) {
			array[1] = mGenericFavicon;
		} else {
			array[1] = new BitmapDrawable(icon);
		}
		LayerDrawable d = new LayerDrawable(array);
		d.setLayerInset(1, 2, 2, 2, 2);
		//getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON, d);
		
		//getSupportActionBar().setIcon(d);
	
	}

	/**
	 * Yoinked from android source
	 *
	 * @param url
	 * @return
	 */
	/*
	private static String buildTitleUrl(String url) {
		String titleUrl = null;

		if (url != null) {
			try {
				// parse the url string
				URL urlObj = new URL(url);
				if (urlObj != null) {
					titleUrl = "";

					String protocol = urlObj.getProtocol();
					String host = urlObj.getHost();

					if (host != null && 0 < host.length()) {
						titleUrl = host;
						if (protocol != null) {
							// if a secure site, add an "https://" prefix!
							if (protocol.equalsIgnoreCase("https")) {
								titleUrl = protocol + "://" + host;
							}
						}
					}
				}
			} catch (MalformedURLException e) {
			}
		}

		return titleUrl;
	}*/

	static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)"
			+ // switch on case insensitive matching
			"("
			+ // begin group for schema
			"(?:http|https):\\/\\/"
			+ "|(?:data|about|javascript):" + ")" + "(.*)");

	private String smartUrlFilter(String url) {

		String inUrl = url.trim();
		boolean hasSpace = inUrl.indexOf(' ') != -1;

		Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl);
		if (matcher.matches()) {
			if (hasSpace) {
				inUrl = inUrl.replace(" ", "%20");
			}
			// force scheme to lowercase
			String scheme = matcher.group(1);
			String lcScheme = scheme.toLowerCase();
			if (!lcScheme.equals(scheme)) {
				return lcScheme + matcher.group(2);
			}
			return inUrl;
		}
		else if (url.indexOf(' ') != -1 || url.indexOf('.') == -1)
		{
			try
			{
				if (mDoJavascript)
					url = DEFAULT_SEARCH_ENGINE + URLEncoder.encode(url, mCharSet);
				else
					url = DEFAULT_SEARCH_ENGINE_NOJS + URLEncoder.encode(url, mCharSet);
			}
			catch (UnsupportedEncodingException ue)
			{
				if (mDoJavascript)
					url = DEFAULT_SEARCH_ENGINE + url.replace(' ','+');
				else
					url = DEFAULT_SEARCH_ENGINE_NOJS + url.replace(' ','+');
				
			}
			
			return url;
		}
		else
			return "http://" + url;
	}

	/**
	 * Yoinked from android browser. Builds and returns the page title, which is
	 * some combination of the page URL and title.
	 *
	 * @param url
	 *            The URL of the site being loaded.
	 * @param title
	 *            The title of the site being loaded.
	 * @return The page title.
	 */
	private String buildUrlTitle(String url, String title) {
		
		String urlTitle = url;// buildTitleUrl(url);

		if (title != null && title.length() > 0) {
			
			urlTitle = title + " | " + url;
			
		}
		return urlTitle;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem arg0) {
		
		if (arg0.getItemId() == R.id.menu_go)
		{
			onSearchRequested();

		}
		else if (arg0.getItemId() == R.id.menu_stop_reload)
		{
			if (mInLoad) {
				stopLoading();
			} else {
				mWebView.reload();
			}
		}
		else if (arg0.getItemId() == R.id.menu_settings)
		{
			startActivity(new Intent(this, EditPreferences.class));

			
		}
		else if (arg0.getItemId() == R.id.menu_homepage)
		{

			Message msg = new Message();
			
			String starturl = mPrefs.getString(
					getString(R.string.pref_homepage),
					getString(R.string.default_homepage));
			msg.getData().putString("url", starturl);
			mLoadHandler.sendMessage(msg);
		}
		
		
		return super.onOptionsItemSelected(arg0);
	}

	/**
	 * Stops the webview from loading a page
	 */
	private void stopLoading() {
		mInLoad = false;
		mWebView.stopLoading();
		//mAnonProxy.stop();
		setProgressBarVisibility (Boolean.FALSE);
		
		mWebViewClient.onPageFinished(mWebView, mWebView.getUrl());
	}

	@Override
	public boolean onSearchRequested() {
		// Open up the search/go dialog
		startSearch(mWebView.getOriginalUrl(), true, null, false);
		return true;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.orweb_main, menu);
	
		return true;
	}

	

	@Override
	protected void onPause() {

		// Registered in onResume so unregister here
		//unregisterReceiver(mBroadcastReceiver);
		//unbindService(mSvcConn);

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		/*
		boolean updated = ((OrwebApp)getApplication()).checkLocale();
		
		if (updated)
		{
			finish();
			startActivity(getIntent());
			
		}*/
		
		initSettings();
	}
	
	private void setProxy ()
	{
		mProxyHost = mPrefs.getString("pref_proxy_host", DEFAULT_PROXY_HOST);
		mProxyPort = Integer.parseInt(mPrefs.getString("pref_proxy_port", DEFAULT_PROXY_PORT));
		mProxyType = mPrefs.getString("pref_proxy_type", DEFAULT_PROXY_TYPE);
				
		new checkTorTask().execute("");
		
		boolean proxyWorked = false;
		
		//enable the proxy whether Tor is running or not		
		try { 
			
			proxyWorked = WebkitProxy.setProxy(getApplicationContext().getApplicationInfo().className,getApplicationContext(), mWebView, mProxyHost,mProxyPort);
		}
		catch (Exception e)
		{
			Log.e("Orweb","error setting proxy",e);
			proxyWorked = false;
		}
		
				
	}
	
	 private class checkTorTask extends AsyncTask<String, Integer, Long> {
	     protected Long doInBackground(String... urls) {
	         
	    		
	 		
	 		if (mProxyHost.equalsIgnoreCase("localhost") && mProxyPort == 8118)
	 		{
	 			
	 			if (!mOrbotHelper.isOrbotInstalled())
	 			{
	 			//	oh.promptToInstall(this);
	 				mLoadHandler.sendEmptyMessage(1);
	 				
	 			}
	 			else if (!mOrbotHelper.isOrbotRunning())
	 			{
	 			//	oh.requestOrbotStart(this);
	 				mLoadHandler.sendEmptyMessage(2);
	 				
	 			}
	 			
	 		}
	    	 
	         return 1l;
	     }

	 }
	

	 private boolean isAppInstalled(String uri) {
	 PackageManager pm = getPackageManager();
	 boolean installed = false;
	 try {
	 pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
	 installed = true;
	 } catch (PackageManager.NameNotFoundException e) {
	 installed = false;
	 }
	 return installed;
	 }

	private void updateTorStatus() {
		int status = 1;
		
/*
		if (mControlService != null) {
			try {
				status = mControlService.getStatus();
			} catch (RemoteException e) {
				// Can't do much here except leave active=false
				e.printStackTrace();
			}
		}
*/
		// Make the "Anonymous connection not available" page appear
		// if Tor is not active
	//	if (status == TorProxyLib.STATUS_ON) {
		if (true)
		{
			
			//mNoTorLayout.setVisibility(View.GONE);
			if (mLastIsTorActive == false)
				mWebView.reload();
			mLastIsTorActive = true;
			return;
		}
		//mNoTorLayout.setVisibility(View.VISIBLE);
		mLastIsTorActive = false;
/*
		if (status == TorProxyLib.STATUS_CONNECTING) {
			mTorStatus.setText(getString(R.string.torConnecting));
			return;
		}
		if (status == TorProxyLib.STATUS_REQUIRES_DEMAND) {
			try {
				mControlService.registerDemand();
				mTorStatus.setText(getString(R.string.torConnecting));
				return;
			} catch (RemoteException e) {
			}
		}*/
		
		//mTorStatus.setText(getString(R.string.torInactive));
	}

	public void onClick(View v) {
		
		if (v.getId() == R.id.CookieIcon) {
		
			CookiesBlockedDialog d = new CookiesBlockedDialog(this);
			d.show();
		}
	}

	/**
	 * Sets the title of the Activity
	 *
	 * @param url
	 *            the string to set the title to
	 */
	private void updateTitle(String url, String title) {

		setTitle(buildUrlTitle(url, title));
		
	}

	/**
	 * Sets the webview settings given a visited URL. Internal pages should show
	 * images and run javascript even if the load images option is set to off.
	 *
	 * @param url
	 *            the URL of the current page
	 */
	/*
	private void updateSettingsPerUrl(String url) {
		if (url.startsWith(getString(R.string.internal_web_url))) {
			// This built-in home page should always show images
			mWebView.getSettings().setLoadsImagesAutomatically(true);
			mWebView.getSettings().setJavaScriptEnabled(true);
		} else {
			// Just a normal page so use the user's preference
			SharedPreferences mPrefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			mWebView.getSettings().setLoadsImagesAutomatically(
					mPrefs.getBoolean(getString(R.string.pref_images), false));
			mWebView.getSettings()
					.setJavaScriptEnabled(
							mPrefs.getBoolean(
									getString(R.string.pref_javascript), true));
			
			String ua = mPrefs.getString("pref_user_agent", mWebView.getSettings().getUserAgentString());
			mWebView.getSettings().setUserAgentString(ua);
			
		}
	}*/

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Enable/disable the "Forward" menu item as appropriate
		boolean canGoForward = mWebView.canGoForward();
		//menu.findItem(R.id.menu_forward).setEnabled(canGoForward);
		updateInLoadMenuItems();
		return super.onPrepareOptionsMenu(menu);
	}

	private final WebViewClient mWebViewClient = new WebViewClient() {

		@Override
		public void doUpdateVisitedHistory(WebView view, String url,
				boolean isReload) {
		//	super.doUpdateVisitedHistory(view, url, isReload);
			
			//no history please
		}

		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			super.onReceivedSslError(view, handler, error);
			
			String msg = "SSL error: " + error.getPrimaryError();
			Log.w("SSLError",msg);

			Toast.makeText(Browser.this,msg,Toast.LENGTH_LONG).show();
			
		}

		
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//Log.i("Shadow", "Page started");
			mCookieManager.clearBlockedCookies();
			// Update image loading settings

			boolean sendCookies = mCookieManager.sendCookiesFor(url);
			mCookieManager.setAcceptsCookies(sendCookies);
			mWebView.setBlockedCookies(mCookieManager.hasBlockedCookies());
			
			// Turn on the progress bar and set it to 10%
		//	 getWindow().requestFeature(Window.FEATURE_PROGRESS);

			setProgressBarVisibility (Boolean.TRUE);
//			setFavicon(favicon);

			updateInLoadMenuItems();
			updateTitle(url, null);
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			
			updateInLoadMenuItems();
			view.clearFormData();
			view.clearCache(true);
			
			if (mClearHistory)
				view.clearHistory();
			
			super.onPageFinished(view, url);
		}
	

		@Override
		public void onLoadResource(WebView view, String url) {
			
			mInLoad = true;
			
			//Log.d("orweb","load request: " + url);

			
			
			super.onLoadResource(view, url);
			
			mInLoad = false;
			
		}
		
		
		
		@SuppressLint("NewApi")
		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view,
				String url) {
			
		//	Log.d("orweb","resource request: " + url);
			
			return super.shouldInterceptRequest(view, url);
		}

		public boolean shouldOverrideUrlLoading(WebView view, String url){
		    // handle by yourself
		    return false;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			
			Toast.makeText(Browser.this, "Error " + errorCode + ": " + description + "; " + failingUrl,Toast.LENGTH_LONG).show();
			
		}

		@Override
		public void onTooManyRedirects(WebView view, Message cancelMsg,
				Message continueMsg) {
			super.onTooManyRedirects(view, cancelMsg, continueMsg);
			
			
		}
		
		/**
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			
			Message msg = new Message();	
			msg.getData().putString("url", url);
			mLoadHandler.sendMessage(msg);
			
			return false;
			
		}
		*/
	
	
	};

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}
	
	private boolean handleIntent(Intent intent)
	{
		
		if (intent != null)
		{
			String action = intent.getAction();
			if (Intent.ACTION_SEARCH.equals(action)) {
				// Navigate to the URL
				String url = intent.getStringExtra(SearchManager.QUERY);
				Message msg = new Message();
				msg.getData().putString("url", url);
				mLoadHandler.sendMessage(msg);
				
				return true;
				
			} else if (Intent.ACTION_VIEW.equals(action)) {
				// Navigate to the URL
				String url = intent.getDataString();
				url = smartUrlFilter(url);
				
				Message msg = new Message();
				msg.getData().putString("url", url);
				mLoadHandler.sendMessage(msg);
				
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Navigate back on the browser
	 *
	 * @return whether the browser navigated back
	 */
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mWebView.canGoBack())
			{
				mWebView.goBack();
				return true;
			}
			
			break;
				
		}
		return super.onKeyDown(keyCode, event);
	}


	/**
	 * Ensures that the menu items are consistent with the page loading state
	 * (ie stop/refresh).
	 */
	private void updateInLoadMenuItems() {
		if (mMenu == null) {
			return;
		}
		MenuItem src = mInLoad ? mMenu.findItem(R.id.menu_stop) : mMenu
				.findItem(R.id.menu_reload);
		MenuItem dest = mMenu.findItem(R.id.menu_stop_reload);
		dest.setIcon(src.getIcon());
		dest.setTitle(src.getTitle());
	}

	private WebChromeClient mWebViewChrome = new WebChromeClient() {
		
		
		
		
		@Override
		public Bitmap getDefaultVideoPoster() {
			// TODO Auto-generated method stub
			return super.getDefaultVideoPoster();
		}

		@Override
		public View getVideoLoadingProgressView() {
			// TODO Auto-generated method stub
			return super.getVideoLoadingProgressView();
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {

			// Update the progress bar of the activity
			//setSupportProgandroid:launchMode="singleTop"ress(newProgress * 100);
		     Browser.this.setProgress(newProgress * 100);

			//setProgressBarIndeterminateVisibility (Boolean.TRUE);
			//getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
				//newProgress * 100);
					
			if (newProgress == 100) {
				if (mInLoad) {
					mInLoad = false;
					updateInLoadMenuItems();
				}
			}
			super.onProgressChanged(view, newProgress);
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			updateTitle(view.getOriginalUrl(), title);
			super.onReceivedTitle(view, title);
		}

		 // The undocumented magic method override
	    // Eclipse will swear at you if you try to put @Override here
	    public void openFileChooser(ValueCallback<Uri> uploadMsg) {

	        mUploadMessage = uploadMsg;
	        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	        i.addCategory(Intent.CATEGORY_OPENABLE);
	        i.setType("*/*");
	        Browser.this.startActivityForResult(
	                Intent.createChooser(i, "File Browser"),
	                FILECHOOSER_RESULTCODE);
	    }
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	 protected void onSaveInstanceState(Bundle outState) {
		 mWebView.saveState(outState);
	  }
	 
	 private DownloadManager mgr;
		private long lastDownload = -1L;
		
		private synchronized void initDownloadManager () throws IOException
		{
			if (mgr == null)
			{
			  
			  Thread thread = new Thread ()
			  {
				
				  public void run ()
				  {
					  try
					  {  
						  mFileServer.start();
					  }
					  catch (Exception e)
					  {
						  Log.e("Orweb","problem starting file server",e);
					  }
				  }
			  };
			  
			  thread.start();
			  
			  mgr=(DownloadManager)getApplicationContext().getSystemService(DOWNLOAD_SERVICE);
			  getApplicationContext().registerReceiver(onComplete,
			                     new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
			  getApplicationContext().registerReceiver(onNotificationClick,
			                     new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
			}
		}
		
		public void showFile (String pathDownload, String mimeType)
		{

			File fileDownload = new File (pathDownload);
			if (!fileDownload.exists())
			{
				fileDownload = new File(pathDownload.substring(5));// remove file:
				
				if (!fileDownload.exists())
					return;
			}
			
			Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
			
			newIntent.setDataAndType(Uri.fromFile(fileDownload),mimeType);
			newIntent.setFlags(newIntent.FLAG_ACTIVITY_NEW_TASK);
			try {
			    startActivity(newIntent);
			} catch (android.content.ActivityNotFoundException e) {
			    Toast.makeText(this, "No handler for this type of file.", 4000).show();
			}
		}
		
		private String fileExt(String url) {
		    if (url.indexOf("?")>-1) {
		        url = url.substring(0,url.indexOf("?"));
		    }
		    if (url.lastIndexOf(".") == -1) {
		        return null;
		    } else {
		        String ext = url.substring(url.lastIndexOf(".") );
		        if (ext.indexOf("%")>-1) {
		            ext = ext.substring(0,ext.indexOf("%"));
		        }
		        if (ext.indexOf("/")>-1) {
		            ext = ext.substring(0,ext.indexOf("/"));
		        }
		        return ext.toLowerCase();

		    }
		}
		
		private void unloadDownloadManager ()
		{
			if (mgr != null)
			{
				mFileServer.stop();
				getApplicationContext().unregisterReceiver(onComplete);
				getApplicationContext().unregisterReceiver(onNotificationClick);
				mgr = null;
			}
		}
		
		private void doDownloadManager (Uri uri, String mimeType, String title) throws IOException
		{
			
		    lastDownload=
		      mgr.enqueue(new DownloadManager.Request(uri)
		                  .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
		                                          DownloadManager.Request.NETWORK_MOBILE)
		                  .setAllowedOverRoaming(false)
		                  .setVisibleInDownloadsUi(true)
		                  .setMimeType(mimeType)
		                  .setShowRunningNotification(true)
		                  .setTitle(title)
		                  .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.getLastPathSegment()));
		}
		
		
		 BroadcastReceiver onComplete=new BroadcastReceiver() {
			    public void onReceive(Context ctxt, Intent intent) {
			    	
			    	 String action = intent.getAction();
			    	 
		                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
		                    long downloadId = intent.getLongExtra(
		                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
		                    Query query = new Query();
		                    query.setFilterById(downloadId);
		                    Cursor c = mgr.query(query);
		                    if (c.moveToFirst()) {
		                        int columnIndex = c
		                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
		                        if (DownloadManager.STATUS_SUCCESSFUL == c
		                                .getInt(columnIndex)) {
		 
		                        	final String uriString = c
		                                    .getString(c
		                                            .getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
		                        
		                        	final String mimetype = c
		                                    .getString(c
		                                            .getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
		                        
		                        	
		                        	if (uriString != null)
		                        	{
		                        		
		                        		final AlertDialog.Builder downloadDialog = new AlertDialog.Builder(Browser.this);
		                		        downloadDialog.setTitle(info.guardianproject.browser.R.string.title_download_manager);
		                		        downloadDialog.setMessage(getString(R.string.prompt_do_you_want_to_open_this_file_for_viewing_) + '\n' + mimetype + '\n' + uriString);
		                		        downloadDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		                		            public void onClick(DialogInterface dialogInterface, int i) {
		                		            	 
				                        		showFile (uriString,mimetype);
				                        		
		                		        		 dialogInterface.dismiss();
		                		            }
		                		        });
		                		        downloadDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
		                		            public void onClick(DialogInterface dialogInterface, int i) {
		                		            }
		                		        });
		                		        
		                		        downloadDialog.show();
		                        		
		                        	}
									
		                        }
		                    }
		                }
			    }
		  };
		  
		  
		  BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
		    public void onReceive(Context ctxt, Intent intent) {
		     
		    	Intent i = new Intent();
		        i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
		        Browser.this.startActivity(i);
		    }
		  };
		  

			private StrongHttpsClient mHttpClient = null;
			
			private synchronized StrongHttpsClient getHttpClient ()
			{
				if (mHttpClient == null)
				{
					mHttpClient = new StrongHttpsClient(Browser.this);

				}
				
				
				mHttpClient.useProxy(true,mProxyType,mProxyHost, mProxyPort);
				
			
				return mHttpClient;
			}

			
		NanoHTTPD mFileServer = new NanoHTTPD ("localhost",9999)
		{
			
			@Override
			public Response serve(String uri, Method method,
					Map<String, String> header, Map<String, String> parms,
					Map<String, String> files) {
				
				String url = parms.get("url");
				
				mHttpClient = getHttpClient ();
				HttpGet hget = new HttpGet(url);
				
				for (String key : mHeaderOverride.keySet())
					hget.addHeader(key, mHeaderOverride.get(key));
					
				try
				{
					mHttpClient.useProxy(true,mProxyType,mProxyHost, mProxyPort);
					
					HttpResponse response = mHttpClient.execute(hget);
					
					HttpEntity respEntity = response.getEntity();
					String mimeType = respEntity.getContentType().getValue();
					
					DataInputStream bis = new DataInputStream(respEntity.getContent());			
							
					Response resp = new Response(Status.OK,mimeType,bis);
					resp.addHeader("Content-Length", respEntity.getContentLength()+"");
					//resp.addHeader("Content-Type", respEntity.getContentType().getValue());
					
					return resp;
				}
				catch (Exception e)
				{
					Log.e("Orweb","unable to proxy download",e);
					return new Response(Status.FORBIDDEN,"text/plain",e.getLocalizedMessage());
				}
			}
			
			
			@Override
			public void stop() {
				super.stop();
				
			}
			
			
		};
		
		private String removeUnproxyeableElements (InputStream is, String encoding) throws IOException
		{
			
			BufferedReader r = new BufferedReader(new InputStreamReader(is,encoding));
			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
			    total.append(line);
			}
			
			String pageData = total.toString();
			pageData = pageData.replaceAll("(?i)<video[^>]*>", "");
			pageData = pageData.replaceAll("(?i)<audio[^>]*>", "");
			
			return pageData;
			
			
		}
		
		private void initHeaders (HttpGet hget)
		{
			String ua = mPrefs.getString("pref_user_agent", DEFAULT_USER_AGENT);
			
			hget.setHeader(HttpHeaders.USER_AGENT, ua);
			hget.setHeader(HttpHeaders.ACCEPT,DEFAULT_HEADER_ACCEPT);
			
			hget.setHeader(HttpHeaders.REFERER,"");
			hget.setHeader(HttpHeaders.ACCEPT_CHARSET,mCharSet);
			
			hget.setHeader(HttpHeaders.ACCEPT_LANGUAGE,mLanguage);
			
			
			
			
		}

}
