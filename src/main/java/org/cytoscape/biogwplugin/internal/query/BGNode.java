package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.model.CyNode;

public class BGNode {
	public String URI;
	public CyNode cyNode; // Keep a handy reference to the CyNode associated with this node.
	public String commonName;

	public BGNode(String uRI) {
		super();
		URI = uRI;
	}
}
