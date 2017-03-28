package org.cytoscape.biogwplugin.internal.query;


public class RelationType {
	
	private String name;
	private String propertyURI;
	
	public RelationType(String propertyURI, String name) {
		super();
		this.name = name;
		this.propertyURI = propertyURI;
	}
	
	public String getName() {
		return name;
	}
	public String getPropertyURI() {
		return propertyURI;
	}
	
	// TODO: Move some of the server interaction code to here, so the type classes contain their own code?
}
