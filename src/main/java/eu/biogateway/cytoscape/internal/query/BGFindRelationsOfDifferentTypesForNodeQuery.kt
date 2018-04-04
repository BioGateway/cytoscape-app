package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGRelationType
import eu.biogateway.cytoscape.internal.parser.BGReturnType

class BGFindRelationsOfDifferentTypesForNodeQuery(serviceManager: BGServiceManager, val relationTypes: Collection<BGRelationType>, val nodeUri: String, val direction: BGRelationDirection, val graphName: String? = null): BGQuery(serviceManager, BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {

        if (relationTypes.size < 1) throw Exception("No relationtypes given!")

        var typeFilter = ""
        for ((index, type) in relationTypes.withIndex()) {
            if (index > 0) typeFilter = typeFilter + ", "
            typeFilter = typeFilter + "<"+type.uri+">"
        }

        val graph = when (graphName != null) {
            true -> "<"+graphName+">"
            false -> "?graph"
        }
        return when (direction) {
            BGRelationDirection.TO -> generateToQueryString(graph, typeFilter)
            BGRelationDirection.FROM -> generateFromQueryString(graph, typeFilter)
        }
    }

    private fun generateToQueryString(graph: String, typeFilter: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "SELECT distinct ?a "+graph+" ?relation <"+nodeUri + ">" +
                "WHERE {\n" +
                "FILTER(?relation IN ("+typeFilter+"))\n" +
                "GRAPH "+graph+" {\n" +
                "?a ?relation <"+nodeUri+"> .\n" +
                "}}"
    }
    private fun generateFromQueryString(graph: String, typeFilter: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "SELECT distinct <"+nodeUri+"> "+graph+" ?relation ?b \n" +
                "WHERE {\n" +
                "FILTER(?relation IN ("+typeFilter+"))\n" +
                "GRAPH "+graph+" {\n" +
                "<"+nodeUri + "> ?relation ?b .\n" +
                "}}"
    }
}