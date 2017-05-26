package org.cytoscape.biogwplugin.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

class BGRelationType(val uri: String, val description: String)

class BGRelation(val fromNode: BGNode, val relationType: BGRelationType, val toNode: BGNode)