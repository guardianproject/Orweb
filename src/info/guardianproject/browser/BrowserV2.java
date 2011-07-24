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

import info.guardianproject.browser.R;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.HttpAuthHandler;
import android.webkit.PluginData;
import android.webkit.UrlInterceptHandler;
import android.webkit.UrlInterceptRegistry;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CacheManager.CacheResult;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The main browser activity for 2.0 device
 * @author Nathan Freitas
 * @author Connell Gauld
 * 
 *
 */
public class BrowserV2 extends Activity implements OnClickListener {

	private AnonProxy mAnonProxy = null;

	// UI elements
	private BrowserWebView mWebView = null;
	private LinearLayout mNoTorLayout = null;
	private LinearLayout mWebLayout = null;
	private Button mStartTor = null;
	private LinearLayout mCookieIcon = null;
	private TextView mTorStatus = null;

	// Misc
	private Drawable mGenericFavicon = null;
	private boolean mInLoad = false;
	private Menu mMenu = null;
	private boolean mLastIsTorActive = true;
	private CookieManager mCookieManager = null;
	private final static String TAG = "BrowserV2";
	
	private static BrowserV2 _browser;
	
	public static BrowserV2 getInstance ()
	{
		return _browser;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		_browser = this;
		
		// Set up title bar of window
		this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
		this.requestWindowFeature(Window.FEATURE_RIGHT_ICON);
		this.requestWindowFeature(Window.FEATURE_PROGRESS);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Allow search to start by just typing
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.main);

		
		mCookieManager = CookieManager.getInstance(this);
		mAnonProxy = new AnonProxy();

		// Grab UI elements
		mWebView = (BrowserWebView) findViewById(R.id.WebView);
		mNoTorLayout = (LinearLayout) findViewById(R.id.NoTorLayout);
		mWebLayout = (LinearLayout) findViewById(R.id.WebLayout);
		mStartTor = (Button) findViewById(R.id.StartTor);
		mCookieIcon = (LinearLayout) findViewById(R.id.CookieIcon);
		mTorStatus = (TextView) findViewById(R.id.torStatus);

		// Set up UI elements
		mStartTor.setOnClickListener(this);
		mWebView.setWebViewClient(mWebViewClient);
		mWebView.setWebChromeClient(mWebViewChrome);
		mWebView.getSettings().setBuiltInZoomControls(true);
		
		mWebView.getSettings().setLoadsImagesAutomatically(false);
		mWebView.getSettings().setJavaScriptEnabled(false);
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		
		
		mWebView.setBlockedCookiesView(mCookieIcon);
		mCookieIcon.setOnClickListener(this);

		
		
		// Misc
		mGenericFavicon = getResources().getDrawable(
				R.drawable.app_web_browser_sm);

		// TODO - properly handle initial Intents
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		String starturl = prefs.getString(getString(R.string.pref_homepage),
				getString(R.string.default_homepage));
		mCookieManager.setBehaviour(prefs.getString("pref_cookiebehaviour",
				"whitelist"));

		Intent i = getIntent();
		if (i != null) {
			if (Intent.ACTION_VIEW.equals(i.getAction())) {
				onNewIntent(i);
				return;
			}
		}
		loadUrl(starturl);
	}

	// Service connection to TorProxy service
	private ServiceConnection mSvcConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
		//	mControlService = ITorProxyControl.Stub.asInterface(service);
			updateTorStatus();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		//	mControlService = null;
			updateTorStatus();
		}

	};

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
		getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON, d);
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

	

	@Override
	public boolean onOptionsItemSelected(MenuItem arg0) {

		switch (arg0.getItemId()) {

		case R.id.menu_go:
			onSearchRequested();
			return true;

		case R.id.menu_forward:
			mWebView.goForward();
			return true;

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
			loadUrl(getString(R.string.internal_web_url) + "about.htm");
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
		mAnonProxy.stop();
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
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Navigate to a web address
	 * 
	 * @param url
	 *            the address to navigate to
	 */
	private void loadUrl(String url) {
		mAnonProxy.stop();
		
		String baseUrl = "x-data://base";
		String mimeType = "text/html";
		String encoding = "UTF-8";
		String failUrl = null;

		
		String data = null;
		
		try
		{
			
			
			// Intercept internal urls
			String internalWebUrl = getString(R.string.internal_web_url);
			if (url.startsWith(internalWebUrl)) {
				
				
				InputStream in;
				try {
					// Fetch an InputStream of the asset
					in = this.getAssets().open("internal_web/" + url.substring(internalWebUrl.length()));
					
					data = Utils.readString(in);
				} catch (IOException e) {
					
				}
			}
			else
			{
		
				HttpResponse resp = mAnonProxy.getHttpResponse(url, null);
				
				
				
				if (resp.getEntity().getContentType()!=null)
				{
					mimeType = resp.getEntity().getContentType().getValue();
				
					if (mimeType.indexOf(";")!=-1)
					{
						encoding = mimeType.substring(mimeType.indexOf(";")+1).trim();
						mimeType = mimeType.substring(0,mimeType.indexOf(";")).trim();
				
						
					}
				}
				
				
				if (resp.getEntity().getContentEncoding()!=null)
					encoding = resp.getEntity().getContentEncoding().getValue().trim();
				
				
				
				
				if (!mimeType.startsWith("text"))
					data = "data:" + mimeType + ";base64," + Utils.readBase64(resp.getEntity().getContent());
				else
				{
					data = Utils.readString(resp.getEntity().getContent());
				}
			}
				//data:image/png;base64,
			mWebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl);
			
		}
		catch (Exception e)
		{
			Log.w(TAG, "unable to load url: " + url + "; " + e.getLocalizedMessage(),e);
			
			mimeType = "text/html";
			data = "unable to load url: " + url + "; " + e.getLocalizedMessage();
			mWebView.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl);
		}
		
		
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

		// Register to receive Tor status update broadcasts
		//registerReceiver(mBroadcastReceiver, torStatusFilter);

		// Bind to the TorProxy control service
		//bindService(new Intent().setComponent(new ComponentName(
			//	TorProxyLib.CONTROL_SERVICE_PACKAGE,
				//TorProxyLib.CONTROL_SERVICE_CLASS)), mSvcConn, BIND_AUTO_CREATE);
		updateTorStatus();

		// Update preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// mWebView.getSettings().setLoadsImagesAutomatically(prefs.getBoolean(
		// getString(R.string.pref_images), false));
		mWebView.getSettings().setJavaScriptEnabled(
				prefs.getBoolean(getString(R.string.pref_javascript), true));
		mCookieManager.setBehaviour(prefs.getString("pref_cookiebehaviour",
				"whitelist"));
		mAnonProxy
				.setSendReferrer(prefs.getBoolean("pref_sendreferrer", false));

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
			
			mNoTorLayout.setVisibility(View.GONE);
			mWebLayout.setVisibility(View.VISIBLE);
			if (mLastIsTorActive == false)
				mWebView.reload();
			mLastIsTorActive = true;
			return;
		}

		mWebLayout.setVisibility(View.GONE);
		mNoTorLayout.setVisibility(View.VISIBLE);
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
		
		mTorStatus.setText(getString(R.string.torInactive));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.StartTor:
			// The "Open preferences" button clicked so start TorProxySettings
			try {
			//	Intent i = new Intent().setComponent(new ComponentName(
				//		TorProxyLib.SETTINGS_ACTIVITY_PACKAGE,
					//	TorProxyLib.SETTINGS_ACTIVITY_CLASS));
				//startActivity(i);
			} catch (ActivityNotFoundException a) {
				AlertDialog.Builder b = new AlertDialog.Builder(this);
				b.setMessage(getString(R.string.torProxyNotInstalled));
				b.setPositiveButton("OK", null);
				b.show();
			}
			break;
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
		getWindow().setTitle(buildUrlTitle(url, title));
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
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Enable/disable the "Forward" menu item as appropriate
		boolean canGoForward = mWebView.canGoForward();
		menu.findItem(R.id.menu_forward).setEnabled(canGoForward);
		updateInLoadMenuItems();
		return super.onPrepareOptionsMenu(menu);
	}

	private final WebViewClient mWebViewClient = new WebViewClient() {

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#doUpdateVisitedHistory(android.webkit.WebView, java.lang.String, boolean)
		 */
		@Override
		public void doUpdateVisitedHistory(WebView view, String url,
				boolean isReload) {
			// TODO Auto-generated method stub
			super.doUpdateVisitedHistory(view, url, isReload);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onFormResubmission(android.webkit.WebView, android.os.Message, android.os.Message)
		 */
		@Override
		public void onFormResubmission(WebView view, Message dontResend,
				Message resend) {
			// TODO Auto-generated method stub
			super.onFormResubmission(view, dontResend, resend);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onReceivedError(android.webkit.WebView, int, java.lang.String, java.lang.String)
		 */
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			// TODO Auto-generated method stub
			super.onReceivedError(view, errorCode, description, failingUrl);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onReceivedHttpAuthRequest(android.webkit.WebView, android.webkit.HttpAuthHandler, java.lang.String, java.lang.String)
		 */
		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			// TODO Auto-generated method stub
			super.onReceivedHttpAuthRequest(view, handler, host, realm);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onScaleChanged(android.webkit.WebView, float, float)
		 */
		@Override
		public void onScaleChanged(WebView view, float oldScale, float newScale) {
			// TODO Auto-generated method stub
			super.onScaleChanged(view, oldScale, newScale);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onTooManyRedirects(android.webkit.WebView, android.os.Message, android.os.Message)
		 */
		@Override
		public void onTooManyRedirects(WebView view, Message cancelMsg,
				Message continueMsg) {
			// TODO Auto-generated method stub
			super.onTooManyRedirects(view, cancelMsg, continueMsg);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onUnhandledKeyEvent(android.webkit.WebView, android.view.KeyEvent)
		 */
		@Override
		public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
			// TODO Auto-generated method stub
			super.onUnhandledKeyEvent(view, event);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#shouldOverrideKeyEvent(android.webkit.WebView, android.view.KeyEvent)
		 */
		@Override
		public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
			// TODO Auto-generated method stub
			return super.shouldOverrideKeyEvent(view, event);
		}

		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// TODO Auto-generated method stub
			Log.i(TAG,"trying to load: " + url);
			
			loadUrl (url);
			
			return true;
			
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//Log.i("Shadow", "Page started");
			mCookieManager.clearBlockedCookies();
			// Update image loading settings
			updateSettingsPerUrl(url);
			// Turn on the progress bar and set it to 10%
			getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 1000);
			setFavicon(favicon);

			updateInLoadMenuItems();
			updateTitle(url, null);
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// Set the progress bar to 100%
			getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
			updateInLoadMenuItems();
			super.onPageFinished(view, url);
		}

		@Override
		public void onLoadResource(WebView view, String url) {
			//Log.i("Shadow", "OnLoad");
			mInLoad = true;
			//super.onLoadResource(view, url);
		}

	};

	@Override
	protected void onNewIntent(Intent intent) {

		// The user has probably entered a URL into "Go"

		String action = intent.getAction();
		if (Intent.ACTION_SEARCH.equals(action)) {
			// Navigate to the URL
			String url = intent.getStringExtra(SearchManager.QUERY);
			url = smartUrlFilter(url);
			loadUrl(url);
		} else if (Intent.ACTION_VIEW.equals(action)) {
			// Navigate to the URL
			String url = intent.getDataString();
			url = smartUrlFilter(url);
			loadUrl(url);
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
			getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
					newProgress * 100);
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

	/*private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//if (TorProxyLib.STATUS_CHANGE_INTENT.equals(intent.getAction())) {
				// TorProxy has broadcast a Tor status update
				//updateTorStatus();
		//	}
		}

	};*/
}