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


        //if (graphUri.contains("gnex")) return generateTFTGQueryStringOld(graphUri)

        return when (relationType.directed) {
            true -> generateQueryString(graphUri)
            false -> generateUndirectedQueryStringNew(graphUri)
        }

//        return when (graphUri) {
//            "genex" -> generateTFTGQueryStringOld(graphUri)
//            "intact" -> generatePPIQueryString(graphUri)
//            else -> {
//                generateQueryStringOld(graphUri)
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
                "GRAPH <$graphUri> {  \n" +
                " ?a has_participant: subject: .\n" +
                " ?a has_participant: object: .\n" +
                " }\n" +
                "}"
    }

    private fun generateUndirectedQueryStringNew(graphUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "SELECT distinct ?instance as ?a1 <$graphUri> has_agent: object: ?instance as ?a2 <$graphUri> has_agent: subject:\n" +
                "WHERE {  \n" +
                "GRAPH <$graphUri> {  \n" +
                "?statement has_agent: subject: .\n" +
                "?statement has_agent: object: .\n" +
                "?instance rdf:type ?statement .\n"+
                "}}"
    }

    private fun generateQueryStringOld(graphName: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <"+toNode+">\n" +
                "PREFIX subject: <"+fromNode+">\n" +
                "SELECT distinct ?a as ?a1 <"+graphName+"> ?rel1 object: ?a as ?a2 <"+graphName+"> ?rel2 subject:\n" +
                "WHERE {  \n" +
                " GRAPH <"+graphName+"> {  \n" +
                "\t ?a ?rel2 subject: .\n" +
                "\t ?a ?rel1 object: .\n" +
                " }\n" +
                "}"
    }

    private fun generateTFTGQueryStringOld(graphName: String): String {
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

    private fun generateQueryString(graphName: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "PREFIX has_target: <http://semanticscience.org/resource/SIO_000291> \n"+
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "SELECT distinct ?instance as ?i1 <$graphName> has_agent: subject: ?instance as ?i2 <$graphName> has_target: object:\n" +
                "WHERE {  \n" +
                "GRAPH <$graphName> {  \n" +
                " ?sentence has_agent: subject: .\n" +
                " ?sentence has_target: object: .\n" +
                " ?instance rdf:type ?sentence . \n" +
                " }\n" +
                "}"
    }


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

    private fun generatePPIQueryString(graphUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <$toNode>\n" +
                "PREFIX subject: <$fromNode>\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "SELECT distinct ?a as ?a1 <$graphUri> has_agent: object: ?a as ?a2 <$graphUri> has_agent: subject:\n" +
                "WHERE {  \n" +
                " GRAPH <$graphUri> {  \n" +
                "\t ?a has_agent: subject: .\n" +
                "\t ?a has_agent: object: .\n" +
                " }\n" +
                "}"
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

}