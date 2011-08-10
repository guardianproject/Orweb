package org.torproject.android;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.http.HttpHost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

public class OrbotHelper {

	private final static String DEFAULT_HOST = "127.0.0.1";
	private final static int DEFAULT_PORT = 8118;
	private final static int DEFAULT_SOCKET_PORT = 9050;
	
	private final static int REQUEST_CODE = 0;
	
	private final static String TAG = "OrbotHelpher";

	public static void setProxy (Context ctx)
	{
		setProxy (ctx, DEFAULT_HOST, DEFAULT_PORT);
	}
	
	public static void setProxy (Context ctx, String host, int port)
	{
		setSystemProperties (host, port);
		setWebkitProxy(ctx, host, port);
	}

	private static void setSystemProperties (String host, int port)
	{

		System.setProperty("http.proxyHost", host);
		System.setProperty("http.proxyPort", port + "");
		
		System.setProperty("https.proxyHost", host);
		System.setProperty("https.proxyPort", port + "");
		
		System.setProperty("socks.proxyHost", host);
		System.setProperty("socks.proxyPort", port + "");
		
	}
	  /**
     * Override WebKit Proxy settings
     *
     * @param ctx Android ApplicationContext
     * @param host
     * @param port
     * @return  true if Proxy was successfully set
     */
    private static boolean setWebkitProxy(Context ctx, String host, int port) {
        boolean ret = false;
        try {
            Object requestQueueObject = getRequestQueue(ctx);
            if (requestQueueObject != null) {
                //Create Proxy config object and set it into request Q
                HttpHost httpHost = new HttpHost(host, port, "http");
               // HttpHost httpsHost = new HttpHost(host, port, "https");

                setDeclaredField(requestQueueObject, "mProxyHost", httpHost);
                ret = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "error setting up webkit proxying", e);
        }
        return ret;
    }

    public static void resetProxy(Context ctx) throws Exception {
        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null) {
            setDeclaredField(requestQueueObject, "mProxyHost", null);
        }
    }

    public static Object getRequestQueue(Context ctx) throws Exception {
        Object ret = null;
        Class networkClass = Class.forName("android.webkit.Network");
        if (networkClass != null) {
            Object networkObj = invokeMethod(networkClass, "getInstance", new Object[]{ctx}, Context.class);
            if (networkObj != null) {
                ret = getDeclaredField(networkObj, "mRequestQueue");
            }
        }
        return ret;
    }

    private static Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        //System.out.println(obj.getClass().getName() + "." + name + " = "+ out);
        return out;
    }

    private static void setDeclaredField(Object obj, String name, Object value)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object invokeMethod(Object object, String methodName, Object[] params, Class... types) throws Exception {
        Object out = null;
        Class c = object instanceof Class ? (Class) object : object.getClass();
        if (types != null) {
            Method method = c.getMethod(methodName, types);
            out = method.invoke(object, params);
        } else {
            Method method = c.getMethod(methodName);
            out = method.invoke(object);
        }
        //System.out.println(object.getClass().getName() + "." + methodName + "() = "+ out);
        return out;
    }

    public static Socket getSocket (Context context, String proxyHost, int proxyPort) throws IOException
    {
    	Socket sock = new Socket();

		sock.connect(new InetSocketAddress(proxyHost, proxyPort), 10000);

		return sock;
    }

    public static Socket getSocket (Context context) throws IOException
    {
    	return getSocket (context, DEFAULT_HOST, DEFAULT_SOCKET_PORT);

    }

    public static AlertDialog initOrbot(Activity activity,
            CharSequence stringTitle,
            CharSequence stringMessage,
            CharSequence stringButtonYes,
            CharSequence stringButtonNo,
            CharSequence stringDesiredBarcodeFormats) {
Intent intentScan = new Intent("org.torproject.android.START_TOR");
intentScan.addCategory(Intent.CATEGORY_DEFAULT);


try {
activity.startActivityForResult(intentScan, REQUEST_CODE);
return null;
} catch (ActivityNotFoundException e) {
return showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes, stringButtonNo);
}
}

    private static AlertDialog showDownloadDialog(final Activity activity,
            CharSequence stringTitle,
            CharSequence stringMessage,
            CharSequence stringButtonYes,
            CharSequence stringButtonNo) {
				AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
				downloadDialog.setTitle(stringTitle);
				downloadDialog.setMessage(stringMessage);
				downloadDialog.setPositiveButton(stringButtonYes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int i) {
				Uri uri = Uri.parse("market://search?q=pname:org.torproject.android");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				activity.startActivity(intent);
				}
				});
				downloadDialog.setNegativeButton(stringButtonNo, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int i) {}
				});
				return downloadDialog.show();
				}
}
