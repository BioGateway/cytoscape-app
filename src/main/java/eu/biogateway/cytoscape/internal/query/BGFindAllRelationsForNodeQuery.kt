package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGReturnType

class BGFindAllRelationsForNodeQuery(val nodeUri: String, val direction: BGRelationDirection): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {


    override fun generateQueryString(): String {
        return when (direction) {
            BGRelationDirection.TO -> generateToQueryString()
            BGRelationDirection.FROM -> generateFromQueryString()
        }
    }

    private fun generateFromQueryString(): String {
        return "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT <$nodeUri> ?graph ?relation ?toNode\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "<$nodeUri> ?relation ?toNode .\n" +
                "}}"
    }

    private fun generateToQueryString(): String {
        return "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT ?fromNode ?graph ?relation <$nodeUri>\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "?fromNode ?relation <$nodeUri> .\n" +
                "}}"
    }
}