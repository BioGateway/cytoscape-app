package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

/// Performs a fetch query for the values with the given relation URI for the given node, in the given graph.
class BGFetchAttributeValuesQuery(serviceManager: BGServiceManager, val nodeUri: String, val relationUri: String, var graphName: String, val direction: BGRelationDirection) : BGQuery(serviceManager, BGReturnType.METADATA_FIELD) {



    override fun generateQueryString(): String {
        if (!graphName.startsWith("?")) graphName = "<"+graphName+">"

        return when (direction) {
            BGRelationDirection.TO -> "BASE <http://www.semantic-systems-biology.org/> \n" +
                    "SELECT ?value\n" +
                    "WHERE {  \n" +
                    " GRAPH "+graphName+" {\n" +
                    "\t ?value <"+relationUri+"> <"+nodeUri+">\n" +
                    "}}"
            BGRelationDirection.FROM -> "BASE <http://www.semantic-systems-biology.org/> \n" +
                    "SELECT ?value\n" +
                    "WHERE {  \n" +
                    " GRAPH "+graphName+" {\n" +
                    "\t <"+nodeUri+"> <"+relationUri+"> ?value\n" +
                    "}}"
        }
    }
}