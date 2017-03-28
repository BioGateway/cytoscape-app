package org.cytoscape.biogwplugin.internal.util;

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
}
