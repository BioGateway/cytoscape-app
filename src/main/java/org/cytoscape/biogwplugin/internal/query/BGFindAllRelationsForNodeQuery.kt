package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGFindAllRelationsForNodeQuery(serviceManager: BGServiceManager, val nodeUri: String, val direction: BGRelationDirection): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE_NAMED) {


    override var queryString: String = ""
        get() = when (direction) {
            BGRelationDirection.TO -> generateToQueryString()
            BGRelationDirection.FROM -> generateFromQueryString()
        }

    private fun generateFromQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX fromNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT fromNode: ?fromNodeName ?graph ?relation ?toNode ?toNodeName\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "fromNode: ?relation ?toNode .\n" +
                "?toNode skos:prefLabel|skos:altLabel ?toNodeName .\n" +
                "fromNode: skos:prefLabel|skos:altLabel ?fromNodeName .\n" +
                "}}"
    }

    private fun generateToQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX toNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT ?fromNode ?fromNodeName ?graph ?relation toNode: ?toNodeName\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "?fromNode ?relation toNode: .\n" +
                "toNode: skos:prefLabel|skos:altLabel ?toNodeName .\n" +
                "?fromNode skos:prefLabel|skos:altLabel ?fromNodeName .\n" +
                "}}"
    }
}