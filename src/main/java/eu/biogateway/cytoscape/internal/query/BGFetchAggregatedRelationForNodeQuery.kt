package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.parser.BGReturnType

/// Returns the relation described by the expanded relation node.
class BGFetchAggregatedRelationForNodeQuery(val node: BGNode, val relationIdentifier: String): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
        val graphName = node.type.metadataGraph ?: throw Exception("Collapsing of this type is not supported!")

        var relationIdentifierParts = relationIdentifier.split("::")
        var graphUri: String = relationIdentifierParts.getOrElse(0) { "" }
        val relationUri = relationIdentifierParts.getOrElse(1) {"http://ssb.biogateway.eu/unknown"}

        if (graphUri.isEmpty()) {
            graphUri = "?graph"
        }

        /*
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX node: <"+node.uri+">\n" +
                "PREFIX graph: <"+graphName+">  \n" +
                "SELECT distinct ?agent graph: ?predicate ?target \n" +
                "WHERE {  \n" +
                " GRAPH graph: {  \n" +
                "  node: rdf:type ?sen . \n"+
                "  ?sen <http://semanticscience.org/resource/SIO_000139> ?agent .\n" +
                "  ?sen <http://semanticscience.org/resource/SIO_000291> ?target .\n" +
                "  ?agent ?predicate ?target . \n"+
                "}}"
        */

        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX sio: <http://semanticscience.org/resource/>\n" +
                "PREFIX node: <${node.uri}> \n" +
                "PREFIX graph: <$graphUri> \n" +
                "SELECT distinct ?subject graph: ?predicate ?object \n" +
                "WHERE {  \n" +
                " GRAPH graph: {  \n" +
                "  node: rdf:subject ?subject .\n" +
                "  node: rdf:object ?object .\n" +
                "  node: rdf:predicate ?predicate .\n" +
                "}}"

//        return "BASE <http://www.semantic-systems-biology.org/> \n" +
//                "PREFIX node: <${node.uri}>\n" +
//                "PREFIX graph: <$graphUri>  \n" +
//                "SELECT distinct ?from graph: <$relationUri> ?to \n" +
//                "WHERE {  \n" +
//                " GRAPH graph: {  \n" +
//                "  node: rdf:type ?int . \n"+
//                "  ?from <http://semanticscience.org/resource/SIO_000062> ?int .\n" +
//                "  ?int <http://semanticscience.org/resource/SIO_000291> ?to .\n" +
//                "}}"
    }
    }


class BGFetchAggregatedTFTGRelationForNodeQuery(val node: BGNode): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
        val graphName = node.type.metadataGraph ?: throw Exception("Collapsing of this type is not supported!")


        return "BASE <http://www.semantic-systems-biology.org/> \n" +
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