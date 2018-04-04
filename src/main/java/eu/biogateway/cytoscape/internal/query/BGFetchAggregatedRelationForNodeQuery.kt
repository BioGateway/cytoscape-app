package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import org.w3c.dom.traversal.NodeFilter

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

class BGFetchAggregatedPPIRelationForNodeQuery(serviceManager: BGServiceManager, val nodeUri: String, val nodeFilter: Collection<String>): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE_CONFIDENCE) {

    override fun generateQueryString(): String {

        var filter = ""
        if (nodeFilter.isNotEmpty()) {
            val nodelist = nodeFilter.map { "<"+it+">" }.reduce { acc, uri -> acc+", "+uri }
            filter += "FILTER(?a IN("+nodelist+"))\n"
            filter += "FILTER(?b IN("+nodelist+"))\n"
        }


        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX ppi: <"+nodeUri+">\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
                "SELECT DISTINCT ?a <intact> <http://purl.obolibrary.org/obo/RO_0002436> ?b ?confidence \n" +
                "WHERE {  \n" +
                "FILTER (?a != ?b)\n" + filter +
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