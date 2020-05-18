package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.parser.BGReturnType

/// Returns the relation described by the expanded relation node.
class BGFetchRelationForBridgeQuery(val node: BGNode, val relationIdentifier: String): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

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

class BGFetchUndirectedRelationForBridgeQuery(val node: BGNode, val relationIdentifier: String, val fromUri: String, val toUri: String): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

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