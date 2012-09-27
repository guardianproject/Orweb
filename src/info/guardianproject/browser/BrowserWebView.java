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

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;

/**
 * A custom webview that overlays the cookies blocked message
 * @author Connell Gauld
 *
 */
public class BrowserWebView extends WebView implements AnimationListener {
	
	private static final int ANIMATION_DURATION = 200; // 200ms
	private static final int HIDE_BLOCKED_DELAY = 3000; // 3 seconds
	
	private ViewGroup mBlockedCookiesView = null;
	private boolean mBlockedCookies = false;
	private boolean mBlockedCookiesViewShowing = false;
	
	private Handler mBlockedCookiesHandler = new Handler();
	
	private Runnable mBlockedCookiesHider = new Runnable() {
		public void run() {
			setBlockedCookiesViewShowing(false);
		}
	};

	public BrowserWebView(Context context) {
		super(context);
		initView ();
	}
	
	@SuppressLint({ "NewApi", "NewApi", "NewApi" })
	private void initView ()
	{
		getSettings().setAllowFileAccess(false);
		
		getSettings().setBuiltInZoomControls(true);
		getSettings().setSupportZoom(true);
		
		getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		
		getSettings().setSaveFormData(false);
		getSettings().setSavePassword(false);
		getSettings().setGeolocationEnabled(false);
		
		getSettings().setAppCacheEnabled(false);
		getSettings().setDatabaseEnabled(false);
		
		getSettings().setPluginState(PluginState.ON_DEMAND);
		
		getSettings().setSupportMultipleWindows(false);
		
		setHapticFeedbackEnabled(true);
		setLongClickable(true);
		
		
		if (Build.VERSION.SDK_INT > 11)
		{
			getSettings().setDisplayZoomControls(true);
			getSettings().setAllowContentAccess(false);
			getSettings().setEnableSmoothTransition(true);
			
		}
		
	}

	public BrowserWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView ();
	}

	public BrowserWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView ();
	}
	
	public void selectAndCopyText() {
	    try {
	        Method m = WebView.class.getMethod("emulateShiftHeld", null);
	        m.invoke(this, null);
	    } catch (Exception e) {
	        e.printStackTrace();
	        // fallback
	        KeyEvent shiftPressEvent = new KeyEvent(0,0,
	             KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_SHIFT_LEFT,0,0);
	        shiftPressEvent.dispatch(this);
	    }
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// If the user is moving on the page update the
		// onMove display
		if (ev.getAction() == MotionEvent.ACTION_MOVE)
			updateBlockedCookiesView();
		
		return super.onTouchEvent(ev);
	}
	
	/**
	 * Set the view that will be hidden or shown as appropriate
	 * @param g the viewgroup
	 */
	public void setBlockedCookiesView(ViewGroup g) {
		this.mBlockedCookiesView = g;
	}
	
	/**
	 * Hide or show the cookies blocked dialog
	 */
	public void updateBlockedCookiesView() {
		if (mBlockedCookies) {
			if (!mBlockedCookiesViewShowing) {
				setBlockedCookiesViewShowing(true);
				mBlockedCookiesHandler.postDelayed(mBlockedCookiesHider, HIDE_BLOCKED_DELAY);
			}
		}
	}
	
	private synchronized void setBlockedCookiesViewShowing(boolean show) {
		
		if (mBlockedCookiesView == null) return;
		
		Animation a = null;
		if (show) {
			mBlockedCookiesViewShowing = true;
			mBlockedCookiesView.setVisibility(View.VISIBLE);
			a = new AlphaAnimation(0.0f, 1.0f);
		} else {
			mBlockedCookiesViewShowing = false;
			a = new AlphaAnimation(1.0f, 0.0f);
			a.setAnimationListener(this);
		}
		
		a.setDuration(ANIMATION_DURATION);
		mBlockedCookiesView.startAnimation(a);
	}
	
	/**
	 * Tell the webview if there are blocked cookies
	 * @param cookies
	 */
	public void setBlockedCookies(boolean cookies) {
		this.mBlockedCookies = cookies;
		updateBlockedCookiesView();
	}

	@Override
	public void onAnimationEnd(Animation arg0) {
		// The view has finished fading out
		// so hide it
		mBlockedCookiesView.setVisibility(View.GONE);
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// Nothing to do
	}

	@Override
	public void onAnimationStart(Animation animation) {
		// Nothing to do
	}

	
	
	
	
}
