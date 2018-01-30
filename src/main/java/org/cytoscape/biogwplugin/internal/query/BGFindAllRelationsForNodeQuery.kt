package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGFindAllRelationsForNodeQuery(serviceManager: BGServiceManager, val nodeUri: String, val direction: BGRelationDirection): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE) {


    override fun generateQueryString(): String {
        return when (direction) {
            BGRelationDirection.TO -> generateToQueryString()
            BGRelationDirection.FROM -> generateFromQueryString()
        }
    }

    private fun generateFromQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX fromNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT fromNode: ?graph ?relation ?toNode\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "fromNode: ?relation ?toNode .\n" +
                "}}"
    }

    private fun generateToQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX toNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT ?fromNode ?graph ?relation toNode:\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "?fromNode ?relation toNode: .\n" +
                "}}"
    }
}