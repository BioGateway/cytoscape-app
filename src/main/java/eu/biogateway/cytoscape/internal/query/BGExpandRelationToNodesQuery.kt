package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGRelationType
import eu.biogateway.cytoscape.internal.parser.BGReturnType

class BGExpandRelationToNodesQuery(val fromNode: String, val toNode: String, val relationType: BGRelationType, returnType: BGReturnType): BGRelationQuery(returnType) {

    enum class GraphType(val graphName: String) {
        GOA("goa"),
        TFTG("tf-tg"),
        INTACT("intact"),
        INVALID("")
    }

    override fun generateQueryString(): String {
        if (!relationType.expandable) throw Exception("Relation type is not expandable!")

        val graphUri = relationType.defaultGraphURI ?: throw Exception("Missing or invalid default graph name!")

        return when (relationType.directed) {
            true -> generateQueryString(graphUri)
            false -> generateUndirectedQueryStringNew(graphUri)
        }
    }


    private fun generateUndirectedQueryStringNew(graphUri: String): String {
        return "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX protein1: <$toNode>\n" +
                "PREFIX protein2: <$fromNode>\n" +
                "SELECT distinct ?shownInstance as ?a1 <$graphUri> ?subjectRel protein1: ?shownInstance as ?a2 <$graphUri> ?objectRel protein2:\n" +
                "WHERE {  \n" +
                "{ SELECT ?statement \n" +
                "WHERE {\n" +
                "GRAPH <$graphUri> {\n" +
                "{  \n" +
                "?statement rdf:subject protein2: .\n" +
                "?statement rdf:object protein1: .\n" +
                "} UNION {\n" +
                "?statement rdf:object protein2: .\n" +
                "?statement rdf:subject protein1: .\n" +
                "}\n" +
                "}}}\n" +
                "GRAPH <$graphUri> {  \n" +
                "?statement rdf:predicate <${relationType.uri}> .\n"+
                "?statement rdfs:seeAlso? ?shownInstance . \n" +
                "?statement ?objectRel protein1: .\n" +
                "?statement ?subjectRel protein2: .\n" +
                "}}"

    }

    /*private fun generateUndirectedQueryStringNew(graphUri: String): String {
        return "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "SELECT distinct ?instance as ?a1 <$graphUri> has_agent: object: ?instance as ?a2 <$graphUri> has_agent: subject:\n" +
                "WHERE {  \n" +
                "GRAPH <$graphUri> {  \n" +
                "?sentence has_agent: subject: .\n" +
                "?sentence has_agent: object: .\n" +
                "?instance rdf:type ?sentence .\n"+
                "}}"
    }*/


    private fun generateQueryString(graphName: String): String {
        return "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "SELECT distinct ?statement as ?i1 <$graphName> rdf:subject subject: ?statement as ?i2 <$graphName> rdf:object object:\n" +
                "WHERE {  \n" +
                "GRAPH <$graphName> {  \n" +
                " ?statement rdf:subject subject: .\n" +
                " ?statement rdf:object object: .\n" +
                " ?statement rdf:predicate <${relationType.uri}> .\n"+
                " }}\n"
    }

/*    private fun generateQueryString(graphName: String): String {
        return "BASE <http://rdf.biogateway.eu/graph/> \n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "PREFIX has_target: <http://semanticscience.org/resource/SIO_000291> \n"+
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "SELECT distinct ?instance as ?i1 <$graphName> has_agent: subject: ?instance as ?i2 <$graphName> has_target: object:\n" +
                "WHERE {  \n" +
                "GRAPH <$graphName> {  \n" +
                " ?statement rdf:subject subject: .\n" +
                " ?statement rdf:object object: .\n" +
                " ?statement rdf:predicate <${relationType.uri}> .\n"+
                " ?instance rdf:type ?statement . \n" +
                " }\n" +
                "}"
    }*/
}