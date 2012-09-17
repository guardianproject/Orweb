package info.guardianproject.browser;

import java.util.Locale;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class OrwebApp extends Application
{

	private Locale locale;
	private final static String DEFAULT_LOCALE = "en";
	private SharedPreferences settings;
	public final static boolean SHOW_LOCALE_CHOOSER = false;
	public final static String PREF_DEFAULT_LOCALE = "pref_default_locale";
	
	@Override
    public void onCreate() {
        super.onCreate();
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        checkLocale();
	}
	
	public boolean checkLocale ()
	{
        Configuration config = getResources().getConfiguration();

        String lang = settings.getString(PREF_DEFAULT_LOCALE, DEFAULT_LOCALE);
        
    	if (lang.equals("xx"))
        {
        	locale = Locale.getDefault();        
        }
        else
        	locale = new Locale(lang);
    	
    	boolean updated = false;
    	
    	if (!config.locale.getLanguage().equals(lang))
    	{
    		config.locale = locale;
    		getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    		updated = true;
    	}
    
        return updated;
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        checkLocale();

       
    }
}
