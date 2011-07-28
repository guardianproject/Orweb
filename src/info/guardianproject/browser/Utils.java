/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */


package info.guardianproject.browser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {

	 public static String readBase64 (InputStream stream)
	    {
	    	
	    	String result = null;
	    	
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	
	    	try {
		    	
	    		int b = -1;

				while ((b = stream.read()) != -1)
				{
					baos.write(b);
				}
				
				result = Base64.encodeBytes( baos.toByteArray() );
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return result;
	    }
	
	 public static String readString (InputStream stream)
	    {
	    	String line = null;
	
	    	StringBuffer out = new StringBuffer();
	    	
	    	try {
		    	BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

				while ((line = reader.readLine()) != null)
				{
					out.append(line);
					out.append('\n');
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return out.toString();
	    	
	    }
	/*
	 * Load the log file text
	 */
	 public static String loadTextFile (String path)
	    {
	    	String line = null;
	
	    	StringBuffer out = new StringBuffer();
	    	
	    	try {
		    	BufferedReader reader = new BufferedReader((new FileReader(new File(path))));

				while ((line = reader.readLine()) != null)
				{
					out.append(line);
					out.append('\n');
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return out.toString();
	    	
	    }
	

		/*
		 * Load the log file text
		 */
		 public static boolean saveTextFile (String path, String contents)
		    {
			 	
		    	try {
		    		
		    		 FileWriter writer = new FileWriter( path, false );
                     writer.write( contents );

                     writer.close();


		    		
		    		return true;
			    	
				} catch (IOException e) {
				//	Log.i(TAG, "error writing file: " + path, e);
						e.printStackTrace();
					return false;
				}
				
				
		    	
		    }
	


}
