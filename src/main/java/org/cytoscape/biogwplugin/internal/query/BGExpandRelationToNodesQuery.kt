package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGExpandRelationToNodesQuery(serviceManager: BGServiceManager, val fromNode: String, val toNode: String, val relationType: BGRelationType, returnType: BGReturnType): BGRelationQuery(serviceManager, returnType) {

    override var queryString: String
        get() = generateQueryString()
        set(value) {}

    enum class GraphType(val graphName: String) {
        GOA("goa"),
        TFTG("tf-tg"),
        INTACT("intact"),
        INVALID("")
    }

    private fun generateQueryString(): String {
        if (!relationType.expandable) throw Exception("Relation type is not expandable!")

        if (relationType.defaultGraphName == "goa") return generateQueryString("goa")
        if (relationType.defaultGraphName == "tf-tg") return generateQueryString("tf-tg")
        if (relationType.defaultGraphName == "intact") return generatePPIQueryString()

        throw Exception("Missing or invalid default graph name!")
    }

    private fun generateQueryString(graphName: String): String {
        return "BASE <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX object: <"+toNode+">\n" +
                "PREFIX subject: <"+fromNode+">\n" +
                "SELECT distinct ?a as ?a1 <goa> ?rel1 object: ?a as ?a2 <goa> ?rel2 subject:\n" +
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
                "FILTER (?count = 2)\n" +
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
}