package info.guardianproject.browser;

import android.content.Context;
import org.apache.http.HttpHost;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for setting WebKit proxy used by Android WebView
 *
 */
public class ProxySettings {

    /**
     * Override WebKit Proxy settings
     *
     * @param ctx Android ApplicationContext
     * @param host
     * @param port
     * @return  true if Proxy was successfully set
     */
    public static boolean setProxy(Context ctx, String host, int port) {
        boolean ret = false;
        try {
            Object requestQueueObject = getRequestQueue(ctx);
            if (requestQueueObject != null) {
                //Create Proxy config object and set it into request Q
                HttpHost httpHost = new HttpHost(host, port, "http");
                HttpHost httpsHost = new HttpHost(host, port, "https");
                
                setDeclaredField(requestQueueObject, "mProxyHost", httpHost);
                ret = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
}