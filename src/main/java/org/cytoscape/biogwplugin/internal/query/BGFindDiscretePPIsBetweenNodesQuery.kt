package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGFindDiscretePPIsBetweenNodesQuery(serviceManager: BGServiceManager, val fromNode: String, val toNode: String): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE, serviceManager.server.parser) {
    override var queryString: String = ""

    init {
        taskMonitorTitle = "Expanding PPI..."
        queryString = generateQueryString(fromNode, toNode)
    }

    private fun generateQueryString(fromNode: String, toNode: String): String {

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