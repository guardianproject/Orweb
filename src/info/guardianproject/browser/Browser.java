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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.torproject.android.OrbotHelper;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * The main browser activity
 * @author Connell Gauld
 *
 */

//public class Browser extends Activity implements UrlInterceptHandler,
	//	OnClickListener {
public class Browser extends SherlockActivity implements
	OnClickListener {

	// TorProxy service
	//private ITorProxyControl mControlService = null;
	
	//private final IntentFilter torStatusFilter = new IntentFilter(
		//	TorProxyLib.STATUS_CHANGE_INTENT);
	//private AnonProxy mAnonProxy = null;

	// UI elements
	private BrowserWebView mWebView = null;
//	private LinearLayout mNoTorLayout = null;
	private LinearLayout mWebLayout = null;
//	private Button mStartTor = null;
	private LinearLayout mCookieIcon = null;
//	private TextView mTorStatus = null;

	// Misc
	private Drawable mGenericFavicon = null;
	private boolean mInLoad = false;
	private Menu mMenu = null;
	private boolean mLastIsTorActive = true;
	private CookieManager mCookieManager = null;

	public static String DEFAULT_PROXY_HOST = "localhost";
	public static String DEFAULT_PROXY_PORT = "8118";
	public static String DEFAULT_SEARCH_ENGINE = "http://3g2upl4pq6kufc4m.onion/?q=";
	
	private static String ABOUT_URL = "https://guardianproject.info/apps/orweb";
	
	private Handler mLoadHandler = new Handler ()
	{

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			String url = msg.getData().getString("url");
			url = smartUrlFilter(url);
			
			mWebView.loadUrl(url);
		}
		
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		// Set up title bar of window
		//requestWindowFeature(Window.FEATURE_LEFT_ICON);
		//requestWindowFeature(Window.FEATURE_RIGHT_ICON);
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		//this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
		//this.requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		
		
		// Allow search to start by just typing
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.main);

		// TODO - properly handle initial Intents
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		setProxy();		

		try
		{
				
			
			mCookieManager = CookieManager.getInstance(this);
			
	
			// Grab UI elements
			mWebView = (BrowserWebView) findViewById(R.id.WebView);
			mWebView.setOnTouchListener(new OnTouchListener () {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					
					if (mWebView.getScrollY() > 10)
						Browser.this.getSherlock().getActionBar().hide();
					else
						Browser.this.getSherlock().getActionBar().show();
					
					return false;
				}
			
			});
			//mNoTorLayout = (LinearLayout) findViewById(R.id.NoTorLayout);
			mWebLayout = (LinearLayout) findViewById(R.id.WebLayout);
			//mStartTor = (Button) findViewById(R.id.StartTor);
			
			mCookieIcon = (LinearLayout) findViewById(R.id.CookieIcon);
			//mTorStatus = (TextView) findViewById(R.id.torStatus);
	
			// Set up UI elements
			//mStartTor.setOnClickListener(this);
			mWebView.setWebViewClient(mWebViewClient);
			mWebView.setWebChromeClient(mWebViewChrome);
			
			mWebView.getSettings().setLoadsImagesAutomatically(true);
			mWebView.setBlockedCookiesView(mCookieIcon);
			
			mWebView.clearFormData();
			mWebView.clearCache(true);
			mWebView.clearHistory();
			
			mCookieIcon.setOnClickListener(this);
			
			resetUserAgent(prefs);
			
			// Misc
			mGenericFavicon = getResources().getDrawable(
					R.drawable.app_web_browser_sm);
	
			String starturl = prefs.getString(getString(R.string.pref_homepage),
					getString(R.string.default_homepage));
			mCookieManager.setBehaviour(prefs.getString("pref_cookiebehaviour",
					"whitelist"));
			

			// mWebView.getSettings().setLoadsImagesAutomatically(prefs.getBoolean(
			// getString(R.string.pref_images), false));
			mWebView.getSettings().setJavaScriptEnabled(
					prefs.getBoolean(getString(R.string.pref_javascript), true));
			mCookieManager.setBehaviour(prefs.getString("pref_cookiebehaviour",
					"whitelist"));
//			mAnonProxy
	//				.setSendReferrer(prefs.getBoolean("pref_sendreferrer", false));

			
			Intent intent = getIntent();
			if (intent != null) {
				String action = intent.getAction();
				if (Intent.ACTION_SEARCH.equals(action)) {
					// Navigate to the URL
					String url = intent.getStringExtra(SearchManager.QUERY);
					Message msg = new Message();
					msg.getData().putString("url", url);
					mLoadHandler.sendMessage(msg);
					return;
				} else if (Intent.ACTION_VIEW.equals(action)) {
					// Navigate to the URL
					String url = intent.getDataString();
					Message msg = new Message();
					msg.getData().putString("url", url);
					mLoadHandler.sendMessage(msg);
					return;
				}
				
			}

	
			if (savedInstanceState != null)
			      mWebView.restoreState(savedInstanceState);
			else
			{
				Message msg = new Message();
				msg.getData().putString("url", starturl);
				mLoadHandler.sendMessage(msg);
			}
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Error configuring Tor proxy settings... exiting", Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	private void resetUserAgent (SharedPreferences prefs)
	{

		String ua = prefs.getString("pref_user_agent", mWebView.getSettings().getUserAgentString());
		if (!ua.equals("-1")) // -1 means use the default Android
			mWebView.getSettings().setUserAgentString(ua);
		
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
		
		this.getSherlock().getActionBar().setIcon(d);
		
	}

	/**
	 * Yoinked from android source
	 *
	 * @param url
	 * @return
	 */
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
	}

	static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)"
			+ // switch on case insensitive matching
			"("
			+ // begin group for schema
			"(?:http|https|file):\\/\\/"
			+ "|(?:data|about|content|javascript):" + ")" + "(.*)");

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
			url = DEFAULT_SEARCH_ENGINE + url;
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
		String urlTitle = "";

		if (url != null) {
			String titleUrl = buildTitleUrl(url);

			if (title != null && 0 < title.length()) {
				if (titleUrl != null && 0 < titleUrl.length()) {
					urlTitle = titleUrl + ": " + title;
				} else {
					urlTitle = title;
				}
			} else {
				if (titleUrl != null) {
					urlTitle = titleUrl;
				}
			}
		}

		return urlTitle;
	}

	/*
	 * Intercept the HTTP requests and tunnel over AnonProxy
	 *
	 * @see android.webkit.UrlInterceptHandler#getPluginData(java.lang.String,
	 * java.util.Map)
	 */
	/*
	public PluginData getPluginData(String url, Map<String, String> headers) {

		//Log.i("Shadow", "Getting: " + url);

		// Intercept internal urls
		String internalWebUrl = getString(R.string.internal_web_url);
		if (url.startsWith(internalWebUrl)) {
			return getFromAsset(url.substring(internalWebUrl.length()));
		}

		// Intercept HTTPS since it doesn't work
		// if (url.toLowerCase().startsWith("https://"))
		// return getFromAsset("sslerror.htm");

		try {
			return mAnonProxy.get(url, headers);
		} catch (UnknownHostException e) {
			return getFromAsset("unknownhosterror.htm");
		} catch (ClientProtocolException e) {
			// Not much we can do except output error page
			// TODO: implement exception specific error page
			e.printStackTrace();
			return getErrorPage();
		} catch (InterruptedIOException e) {
			return stringToPluginData("", 200);
		} catch (Exception e) {
			// Not much we can do except output error page
			// TODO: implement exception specific error page
			e.printStackTrace();
			return getErrorPage();
		}
	}
*/
	/**
	 * Fetches an asset as if it were an HTTP request.
	 *
	 * @param path
	 *            the path of the asset to get
	 * @return the PluginData structure containing the asset
	 */
	/*
	private PluginData getFromAsset(String path) {
		InputStream in;
		try {
			// Fetch an InputStream of the asset
			in = this.getAssets().open("internal_web/" + path);
		} catch (IOException e) {
			return stringToPluginData("An error has occurred: " + e.toString(),
					200);
		}

		return new PluginData(in, 0L, new HashMap<String, String[]>(), 200);
	}
*/
	/**
	 * Returns a PluginData object filled with HTML from a string
	 *
	 * @param s
	 *            the string containing HTML
	 * @param statuscode
	 *            the HTTP status code for the object
	 * @return an appropriate PluginData object
	 */
	/*
	private PluginData stringToPluginData(String s, int statuscode) {

		// Default error if can't convert provided string
		byte[] err = { 68, 111, 104 }; // Doh
		try {
			err = s.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			// Oh dear. Not much we can do if UTF-8 isn't supported
			// except go with "Doh"
			e.printStackTrace();
		}

		ByteArrayInputStream b = new ByteArrayInputStream(err);
		PluginData p = new PluginData(b, err.length,
				new HashMap<String, String[]>(), statuscode);
		return p;
	}

	public PluginData getErrorPage() {
		return getFromAsset("error.htm");
	}

	@Override
	public CacheResult service(String arg0, Map<String, String> arg1) {
		// Deprecated; do nothing. Isn't even called any more.
		Log.i("Browser","cache result service called");
		return null;
	}
*/
	@Override
	public boolean onOptionsItemSelected(MenuItem arg0) {

		switch (arg0.getItemId()) {

		case android.R.id.home:
		case R.id.menu_go:
			onSearchRequested();
			return true;
/*
		case R.id.menu_forward:
			mWebView.goForward();
			return true;
*/
		case R.id.menu_stop_reload:
			if (mInLoad) {
				stopLoading();
			} else {
				mWebView.reload();
			}
			return true;

		case R.id.menu_settings:
			startActivity(new Intent(this, EditPreferences.class));
			return true;

		case R.id.menu_about:
		
			Message msg = new Message();
			msg.getData().putString("url", ABOUT_URL);
			mLoadHandler.sendMessage(msg);
		
			return true;
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
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main, menu);

		getSherlock().getActionBar().setHomeButtonEnabled(true);
		
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

		boolean updated = ((OrwebApp)getApplication()).checkLocale();
		
		if (updated)
		{
			finish();
			startActivity(getIntent());
			
		}
	}
	
	private void setProxy ()
	{
		boolean proxyWorked = false;
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		try { 
			proxyWorked = OrbotHelper.setProxy(this, prefs.getString("pref_proxy_host", DEFAULT_PROXY_HOST),Integer.parseInt(prefs.getString("pref_proxy_port", DEFAULT_PROXY_PORT)));
		}
		catch (Exception e)
		{
			proxyWorked = false;
		}
		
		
		//boolean proxyWorked = ProxySettings.setProxy(this, prefs.getString("pref_proxy_host", DEFAULT_PROXY_HOST),Integer.parseInt(prefs.getString("pref_proxy_port", DEFAULT_PROXY_PORT)));
		
		if (!proxyWorked)
		{
			Toast.makeText(this, "Orweb is unable to configure proxy settings on your device.", Toast.LENGTH_LONG).show();
			
		}
		else
		{
			
			boolean orbotInstalled = isAppInstalled("org.torproject.android");

			if (!orbotInstalled)
			{
				Toast.makeText(this, getString(R.string.torProxyNotInstalled), Toast.LENGTH_LONG).show();
						
			}
	
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
			mWebLayout.setVisibility(View.VISIBLE);
			if (mLastIsTorActive == false)
				mWebView.reload();
			mLastIsTorActive = true;
			return;
		}

		mWebLayout.setVisibility(View.GONE);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.CookieIcon:
			CookiesBlockedDialog d = new CookiesBlockedDialog(this);
			d.show();
			break;
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
	private void updateSettingsPerUrl(String url) {
		if (url.startsWith(getString(R.string.internal_web_url))) {
			// This built-in home page should always show images
			mWebView.getSettings().setLoadsImagesAutomatically(true);
			mWebView.getSettings().setJavaScriptEnabled(true);
		} else {
			// Just a normal page so use the user's preference
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			mWebView.getSettings().setLoadsImagesAutomatically(
					prefs.getBoolean(getString(R.string.pref_images), false));
			mWebView.getSettings()
					.setJavaScriptEnabled(
							prefs.getBoolean(
									getString(R.string.pref_javascript), true));
			
			resetUserAgent(prefs);
		}
	}

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
			
		}

		/*
		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view,
				String url) {
			//Log.d("Orweb", "loading resource: " + url);
			return super.shouldInterceptRequest(view, url);
		}*/

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//Log.i("Shadow", "Page started");
			mCookieManager.clearBlockedCookies();
			// Update image loading settings
			updateSettingsPerUrl(url);
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
			
			//setProgressBarVisibility (Boolean.FALSE);
			
			updateInLoadMenuItems();
			view.clearFormData();
			super.onPageFinished(view, url);
		}
	

		@Override
		public void onLoadResource(WebView view, String url) {
			//Log.i("Shadow", "OnLoad");
			mInLoad = true;
			
			 super.onLoadResource(view, url);
			
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			
		//	setProgressBarVisibility (Boolean.FALSE);
		}

		@Override
		public void onTooManyRedirects(WebView view, Message cancelMsg,
				Message continueMsg) {
			super.onTooManyRedirects(view, cancelMsg, continueMsg);
			
			//setProgressBarVisibility (Boolean.FALSE);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			
			return false;
			
			
			
		}
		
		

	};

	@Override
	protected void onNewIntent(Intent intent) {

		// The user has probably entered a URL into "Go"

		String action = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(action)) {
			// Navigate to the URL
			String url = intent.getStringExtra(SearchManager.QUERY);
			Message msg = new Message();
			msg.getData().putString("url", url);
			mLoadHandler.sendMessage(msg);
			
			
		} else if (Intent.ACTION_VIEW.equals(action)) {
			// Navigate to the URL
			String url = intent.getDataString();
			url = smartUrlFilter(url);
			
			Message msg = new Message();
			msg.getData().putString("url", url);
			mLoadHandler.sendMessage(msg);
		}
	}

	/**
	 * Navigate back on the browser
	 *
	 * @return whether the browser navigated back
	 */
	/*
	public void onBackPressed() {
		if (mWebView.canGoBack()) {
			mWebView.goBack();
			//return true;
		} else {
			//return false;
		}
	}*/

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mWebView.canGoBack()) {
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
		public void onProgressChanged(WebView view, int newProgress) {

			// Update the progress bar of the activity
			//setSupportProgress(newProgress * 100);
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

			if (mCookieManager.hasBlockedCookies()) {
				mWebView.setBlockedCookies(true);
			} else {
				mWebView.setBlockedCookies(false);
			}
			super.onProgressChanged(view, newProgress);
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			updateTitle(view.getOriginalUrl(), title);
			super.onReceivedTitle(view, title);
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

}