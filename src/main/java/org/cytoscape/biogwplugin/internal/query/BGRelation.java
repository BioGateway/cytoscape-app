package org.cytoscape.biogwplugin.internal.query;

public class BGRelation {
	public BGNode fromNode;
	public BGNode toNode;
	public String URI;

	public BGRelation(BGNode fromNode, BGNode toNode, String URI) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.URI = URI;
	}
}
