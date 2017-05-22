package org.cytoscape.biogwplugin.internal.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Utility {
	// TODO: Identify a set of legal characters.
		public static String sanitize(String input) {
			return input.replaceAll("\"", "");
		}
		
		public static String sanitizeParameter(String parameter) {
			
			parameter = parameter.replace("\"", "");
			parameter = parameter.replace(" ", "");
			
			return parameter;
		}

		public static boolean validateURI(String uri) {

			try {
				URL url = new URL(uri);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setRequestProperty("User-Agent", "Mozilla/5.0");
				int responseCode = connection.getResponseCode();

				if (responseCode == 200) {
					return true;
				} else {
					return false;
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
}
