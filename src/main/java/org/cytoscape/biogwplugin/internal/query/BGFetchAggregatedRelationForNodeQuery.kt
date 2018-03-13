package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

/// Returns the relation described by the expanded relation node.
class BGFetchAggregatedRelationForNodeQuery(serviceManager: BGServiceManager, val node: BGNode): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
        val graphName = when (node.type) {
            BGNodeType.PPI -> "intact"
            BGNodeType.GOA -> "goa"
            BGNodeType.TFTG -> "tf-tg"
            else -> {
                throw Exception("Collapsing of this type is not supported!")
            }
        }

        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX node: <"+node.uri+">\n" +
                "PREFIX graph: <"+graphName+">  \n" +
                "SELECT distinct ?subject graph: ?predicate ?object \n" +
                "WHERE {  \n" +
                " GRAPH graph: {  \n" +
                "  node: rdf:subject ?subject .\n" +
                "  node: rdf:object ?object .\n" +
                "  node: rdf:predicate ?predicate .\n" +
                "}}"
    }
}

class BGFetchAggregatedPPIRelationForNodeQuery(serviceManager: BGServiceManager, val nodeUri: String): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE_CONFIDENCE) {

    override fun generateQueryString(): String {

        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX ppi: <"+nodeUri+">\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
                "SELECT DISTINCT ?a <intact> <http://purl.obolibrary.org/obo/RO_0002436> ?b ?confidence \n" +
                "WHERE {  \n" +
                "FILTER (?a != ?b)\n" +
                " GRAPH <intact> {\n" +
                "\t ppi: has_agent: ?a .\n" +
                "\t ppi: has_agent: ?b .\n" +
                "\t?meta rdf:subject ?a .\n" +
                "\t?meta rdf:predicate <http://purl.obolibrary.org/obo/RO_0002436> .\n" +
                "\t?meta rdf:object ?b .\n" +
                "\t?meta rdfs:label ?confidence ." +
                "}}"
    }

}