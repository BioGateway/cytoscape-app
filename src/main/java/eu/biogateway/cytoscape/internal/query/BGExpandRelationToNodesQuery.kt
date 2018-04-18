package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
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

        if (relationType.defaultGraphName == "goa") return generateQueryString("goa")
        if (relationType.defaultGraphName == "tf-tg") return generateQueryString("tf-tg")
        if (relationType.defaultGraphName == "intact") return generatePPIQueryString2()

        throw Exception("Missing or invalid default graph name!")
    }

    private fun generateQueryString(graphName: String): String {
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

    private fun generatePPIQueryString2(): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <"+toNode+">\n" +
                "PREFIX subject: <"+fromNode+">\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n"+
                "SELECT distinct ?a as ?a1 <intact> has_agent: object: ?a as ?a2 <intact> has_agent: subject:\n" +
                "WHERE {  \n" +
                " GRAPH <intact> {  \n" +
                "\t ?a has_agent: subject: .\n" +
                "\t ?a has_agent: object: .\n" +
                " }\n" +
                "}"
    }
}