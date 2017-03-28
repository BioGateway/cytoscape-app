package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.model.CyNode;

public class BGRelation {
	BGNode fromNode;
	BGNode toNode;
	String relation;

	public BGRelation(BGNode fromNode, BGNode toNode, String relation) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.relation = relation;
	}
}
