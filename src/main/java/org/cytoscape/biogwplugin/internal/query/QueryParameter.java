package org.cytoscape.biogwplugin.internal.query;

import java.util.HashMap;

public class QueryParameter {
	
	public enum ParameterType {
		TEXT, CHECKBOX, COMBOBOX, UNIPROT_ID, ONTOLOGY, OPTIONAL_URI
	}
	
//	public class Option {
//		String name;
//		String value;
//		
//		public Option(String name, String value) {
//			super();
//			this.name = name;
//			this.value = value;
//		}
//		
//	}
	
	String id;
	String name;
	ParameterType type;
	//ArrayList<Option> options;
	String value = null;
	HashMap<String, String> options;
	
	public QueryParameter(String id, String name, ParameterType type) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.options = new HashMap<>();
	}
	
	public void addOption(String name, String value) {
		options.put(name, value);
	}
}
