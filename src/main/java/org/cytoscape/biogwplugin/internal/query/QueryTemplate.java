package org.cytoscape.biogwplugin.internal.query;

import java.util.ArrayList;

public class QueryTemplate {
	String name;
	String description;
	String sparqlString;
	ArrayList<QueryParameter> parameters;
	public QueryTemplate(String name, String description, String sparqlString) {
		super();
		this.name = name;
		this.description = description;
		this.sparqlString = sparqlString;
		this.parameters = new ArrayList<>();
	}
	
	public void addParameter(QueryParameter p) {
		parameters.add(p);
	}
	
}