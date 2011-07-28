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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;

/**
 * Rewrites HTML to turn POST forms to GET forms.
 * It also adds a hidden input element to each form with a random
 * name/value pair that can be used to determine whether a GET request
 * was once supposed to be a POST request.
 * @author cmg47
 *
 */
public class PostProcessor {
	
	// Random name/value pair variables
	private static int RANDOM_LENGTH = 32;
	private Random gen = new Random();
	private String randomName = null;
	private String randomValue = null;

	/**
	 * Construct a new PostProcessor.
	 */
	public PostProcessor() {
		randomName = getRandomString(RANDOM_LENGTH);
		randomValue = getRandomString(RANDOM_LENGTH);
	}
	
	/**
	 * Takes an InputStream and returns a new InputStream with any POST
	 * forms transformed to GET (plus an identifying hidden input element)
	 * @param in the source InputStream
	 * @return the new InputStream
	 * @throws IOException
	 */
	public InputStream rewriteIncoming(InputStream in) throws IOException {
		
		// Input/output readers/writers etc
		BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream));
		
		// State variables
		boolean inComment = false;
		boolean inCommentNearEnd = false;
		boolean inScript = false;
		boolean inTag = false;
		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;
		boolean inForm = false;
		boolean inMethod = false;
		boolean appendInput = false;
		
		char nextChar;
		int nextCharInt;
		while(true) {
			
			// Read in a character
			nextCharInt = inReader.read();
			if (nextCharInt == -1) break;
			nextChar = (char)nextCharInt;
			
			// Output it
			out.append(nextChar);
			
			if (!inTag) {
				if (nextChar == '<') {
					inTag = true;
					if (checkFor("!--", inReader, out)) {
						inComment = true;
					} else if (checkFor("script", inReader, out)) {
						inScript = true;
					} else if (checkFor("form", inReader, out)) {
						inForm = true;
					}
				}
			} else {
				// inside a tag
				
				if (inMethod) {
					if (Character.isWhitespace(nextChar)) {
						inMethod = false;
						continue;
					}
				}
				
				if (inCommentNearEnd) {
					if (nextChar == '>') {
						inTag = false;
						inComment = false;
						inCommentNearEnd = false;
						continue;
					} else if (Character.isWhitespace(nextChar)) {
						continue;
					} else {
						// We're back in to normal comment
						inCommentNearEnd = false;
					}
				}
					
				if (inComment) {
					if (nextChar == '-') {
						if (checkFor("-", inReader, out)) {
							inCommentNearEnd = true;
							continue;
						}
					}
					continue;
				}
				
				if (inScript) {
					if (checkFor("</script", inReader, out)) {
						inScript = false;
					}
					continue;
				}
				
				// Track quotes if in tag but not in script or comment
				if (inSingleQuote) {
					if (nextChar == '\'') {
						inSingleQuote = false;
					}
					continue;
				}
				
				if (inDoubleQuote) {
					if (nextChar == '"') {
						inDoubleQuote = false;
					}
					continue;
				}
				
				if (nextChar == '\'') {
					inSingleQuote = true;
					if (inMethod) {
						if (checkFor("post", inReader, null)) {
							out.write("GET");
							appendInput = true;
							inMethod = false;
							continue;
						}
					}
					continue;
				}
				
				if (nextChar == '"') {
					inDoubleQuote = true;
					if (inMethod) {
						if (checkFor("post", inReader, null)) {
							out.write("GET");
							appendInput = true;
							inMethod = false;
							continue;
						}
					}
					continue;
				}
				
				if (inForm) {
					if (checkFor("method=", inReader, out)) {
						if (checkFor("post", inReader, null)) {
							out.write("GET");
							appendInput = true;
							continue;
						}
						inMethod=true;
						continue;
					}
				}
				
				// Not in a special tag
				if (nextChar == '>') {
					if (inMethod) {
						inMethod = false;
					}
					if (appendInput) {
						// Output the identifying hidden field
						out.write("<input type='hidden' name='" + randomName + "' value='" + randomValue + "'/>");
						appendInput = false;
					}
					inTag = false;
				}
			}
		}
		
		// Return pipe the output stream into an input stream.
		out.flush();
		byte[] outArray = outStream.toByteArray();
		return new ByteArrayInputStream(outArray);
	}

	/**
	 * Looks for a string in the upcoming data. Outputs the string to o only
	 * if it is found. Otherwise resets the input back to where it started
	 * and doesn't touch o.
	 * @param str the string to look for
	 * @param b where to look
	 * @param o output to use if str is found. Pass null if no output is desired
	 * @return true if str found and outputed
	 */	
	private boolean checkFor(String str, BufferedReader b, BufferedWriter o) {
		try {
			// Set a mark so we can reset if required
			b.mark(str.length()+1);
			// Read an appropriate number of characters
			char[] buffer = new char[str.length()];
			int read = b.read(buffer);
			if (read == -1) {
				b.reset();
				return false;
			}
			// (Non-case-sensitive) check to see if strings match
			if (new String(buffer, 0, read).toLowerCase().equals(str.toLowerCase())) {
				if (o != null) o.write(buffer, 0, read);
				return true;
			} else {
				b.reset();
				return false;
			}
		} catch (IOException e) {
			try {
				b.reset();
			} catch (IOException e1) {
				// No mark
			}
			return false;
		}
	}
	
	/**
	 * Generates a random string alphanumeric of specified length.
	 * @param length number of characters desired
	 * @return random alphanumeric string
	 */
	private String getRandomString(int length) {
		StringBuilder b = new StringBuilder();
		int next;
		for (int i=0; i<length; i++) {
			next = gen.nextInt(62);
			if (next <26) b.append((char)(next + 65));
			else if (next < 52) b.append((char)((next-26) + 97));
			else b.append((char)((next-52) + 48));
		}
		return b.toString();
	}
	
	/**
	 * Determine whether the supplied mimetype can be processed by
	 * this class.
	 * @param type the mimetype to check
	 * @return true if this class can process this mimetype
	 */
	public static boolean canProcessMime(String type) {
		String t = type.toLowerCase();
		// Can process html/xml like content
		if (t.contains("text/html")) return true;
		if (t.contains("application/xhtml+xml")) return true;
		if (t.contains("text/xml")) return true;
		return false;
	}
	
	/**
	 * Checks if the supplied name/value pair is the identifying pair
	 * @param name the name of the field
	 * @param value the value of the field
	 * @return true if this name/value pair is the identifying pair
	 */
	public boolean isPostProcessorIdentifier(String name, String value) {
		if (name.equals(randomName) && value.equals(randomValue)) {
			return true;
		}
		return false;
	}
}
