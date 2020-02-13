package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.parser.BGReturnType

/// Returns the relation described by the expanded relation node.
class BGFetchAggregatedRelationForNodeQuery(val node: BGNode, val relationIdentifier: String): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
        val relationIdentifierParts = relationIdentifier.split("::")
        var graphUri: String = relationIdentifierParts.getOrElse(0) { "" }

        graphUri = if (graphUri.isEmpty()) {
            "?graph"
        } else {
            "<$graphUri>"
        }

        return  "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX sio: <http://semanticscience.org/resource/>\n" +
                "SELECT DISTINCT ?subject $graphUri ?predicate ?object \n" +
                "WHERE {\n" +
                "GRAPH $graphUri {\n" +
                "<${node.uri}> rdf:subject ?subject .\n" +
                "<${node.uri}> rdf:object ?object .\n" +
                "<${node.uri}> rdf:predicate ?predicate .\n" +
                "}}"
    }}

class BGFetchAggregatedUndirectedRelationForNodeQuery(val node: BGNode, val relationIdentifier: String, val fromUri: String, val toUri: String): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
        val relationIdentifierParts = relationIdentifier.split("::")
        var graphUri: String = relationIdentifierParts.getOrElse(0) { "" }

        graphUri = if (graphUri.isEmpty()) {
            "?graph"
        } else {
            "<$graphUri>"
        }

        return "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX sio: <http://semanticscience.org/resource/>\n" +
                "SELECT DISTINCT ?subject $graphUri ?relationUri ?object \n" +
                "WHERE {\n" +
                "GRAPH $graphUri {\n" +
                "?statement rdfs:seeAlso? <${node.uri}> .\n" +
                "?statement rdf:subject ?subject .\n" +
                "?statement rdf:object ?object .\n" +
                "?statement rdf:predicate ?relationUri .\n"+
                "}\n" +
                "FILTER (?subject IN (<$fromUri>, <$toUri>)) \n" +
                "FILTER (?object IN (<$fromUri>, <$toUri>)) \n" +
                "FILTER (?subject != ?object) }\n"
    }
}


class BGFetchAggregatedTFTGRelationForNodeQuery(val node: BGNode): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
        val graphName = node.type.metadataGraph ?: throw Exception("Collapsing of this type is not supported!")


        return "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX node: <"+node.uri+">\n" +
                "PREFIX graph: <"+graphName+">  \n" +
                "SELECT distinct ?tf graph: <http://semanticscience.org/resource/SIO_000001> ?tg \n" +
                "WHERE {  \n" +
                " GRAPH graph: {  \n" +
                "  node: rdf:type ?int . \n"+
                "  ?tf <http://semanticscience.org/resource/SIO_000062> ?int .\n" +
                "  ?int <http://semanticscience.org/resource/SIO_000291> ?tg .\n" +
                "}}"
    }
}


class BGFetchAggregatedPPIRelationForNodeQuery(val nodeUri: String, val nodeFilter: Collection<String>): BGRelationQuery(BGReturnType.RELATION_TRIPLE_CONFIDENCE) {

    override fun generateQueryString(): String {

        var filter = ""
        if (nodeFilter.isNotEmpty()) {
            val nodelist = nodeFilter.map { "<"+it+">" }.reduce { acc, uri -> acc+", "+uri }
            filter += "FILTER(?a IN("+nodelist+"))\n"
            filter += "FILTER(?b IN("+nodelist+"))\n"
        }

        return "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "PREFIX ppi: <"+nodeUri+">\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
                "PREFIX has_participant: <http://semanticscience.org/resource/SIO_000132>\n" +
                "SELECT DISTINCT ?a <intact> <http://purl.obolibrary.org/obo/RO_0002436> ?b ?confidence \n" +
                "WHERE {  \n" +
                "FILTER (?a != ?b)\n" + filter +
                " GRAPH <intact> {\n" +
                "?s has_agent: ?a .\n" +
                "?s has_agent: ?b .\n" +
                "?meta rdf:subject ?a .\n" +
                "?meta rdf:predicate <http://purl.obolibrary.org/obo/RO_0002436> .\n" +
                "?meta rdf:object ?b .\n" +
                "?meta rdfs:label ?confidence ." +
                "}}"
    }
}
