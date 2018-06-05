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


        //if (graphUri.contains("gnex")) return generateTFTGQueryString(graphUri)

        return when (relationType.directed) {
            true -> generateQueryString(graphUri)
            false -> generateUndirectedQueryString(graphUri)
        }

//        return when (uri) {
//            "http://ssb.biogateway.eu/graph/gnex" -> generateTFTGQueryString(uri)
//            "genex" -> generateTFTGQueryString(uri)
//            "intact" -> generatePPIQueryString2()
//            else -> {
//                generateQueryString(uri)
//            }
//        }

//        if (relationType.defaultGraphURI == "goa") return generateQueryString("goa")
//        if (relationType.defaultGraphURI == "tf-tg") return generateQueryString("tf-tg")
//        if (relationType.defaultGraphURI == "genex") return generateTFTGQueryString()
//        if (relationType.defaultGraphURI == "intact") return generatePPIQueryString2()


    }

    private fun generateUndirectedQueryString(graphUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "PREFIX has_participant: <http://semanticscience.org/resource/SIO_000132>\n"+
                "SELECT distinct ?a as ?a1 <$graphUri> has_participant: object: ?a as ?a2 <$graphUri> has_participant: subject:\n" +
                "WHERE {  \n" +
                " GRAPH <$graphUri> {  \n" +
                "\t ?a has_participant: subject: .\n" +
                "\t ?a has_participant: object: .\n" +
                " }\n" +
                "}"
    }

    private fun generateQueryString(graphName: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX is_participant_in: <http://semanticscience.org/resource/SIO_000062> \n"+
                "PREFIX has_target: <http://semanticscience.org/resource/SIO_000291> \n"+
                "PREFIX object: <"+toNode+">\n" +
                "PREFIX subject: <"+fromNode+">\n" +
                "SELECT distinct subject: <"+graphName+"> is_participant_in: ?a as ?a1 ?a as ?a2 <"+graphName+"> has_target: object:\n" +
                "WHERE {  \n" +
                " GRAPH <"+graphName+"> {  \n" +
                "\t subject: is_participant_in: ?sentence .\n" +
                "\t ?sentence has_target: object: .\n" +
                "\t ?a rdf:type ?sentence . \n" +
                " }\n" +
                "}"
    }

    /*
    private fun generateTFTGQueryString(graphName: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX gene: <"+toNode+">\n" +
                "PREFIX protein: <"+fromNode+">\n" +
                "SELECT distinct protein: <"+graphName+"> <http://semanticscience.org/resource/SIO_000062> ?a as ?a1 ?a as ?a2 <"+graphName+"> <http://semanticscience.org/resource/SIO_000291> gene: \n" +
                "WHERE {  \n" +
                " GRAPH <"+graphName+"> {  \n" +
                "protein: <http://semanticscience.org/resource/SIO_000062> ?int .\n" +
                "?int <http://semanticscience.org/resource/SIO_000291> gene: .\n" +
                "?a rdf:type ?int .\n" +
                " }\n" +
                "}"
    }

    private fun generateTFTGQueryString(): String {
        return ""
    }

    private fun generatePPIQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "SELECT DISTINCT ?ppi <intact> has_agent: ?node \n" +
                "WHERE {\n" +
            //    "FILTER (?count = 2)\n" +
                "GRAPH <intact> {\n" +
                "?ppi has_agent: ?node . }\n" +
                "{\n" +
                "SELECT ?ppi count(?node) as ?count\n" +
                "WHERE {  \n" +
                "GRAPH <intact> {  \n" +
                "?ppi has_agent: <" + fromNode + "> .\n" +
                "?ppi has_agent: <" + toNode + "> .\n" +
                "?ppi has_agent: ?node .\n" +
                "}}}}"

    }

    private fun generatePPIQueryString2(graphUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "PREFIX has_participant: <http://semanticscience.org/resource/SIO_000132>\n"+
                "SELECT distinct ?a as ?a1 <$graphUri> has_participant: object: ?a as ?a2 <$graphUri> has_participant: subject:\n" +
                "WHERE {  \n" +
                " GRAPH <intact> {  \n" +
                "\t ?a has_participant: subject: .\n" +
                "\t ?a has_participant: object: .\n" +
                " }\n" +
                "}"
    }
    */
}