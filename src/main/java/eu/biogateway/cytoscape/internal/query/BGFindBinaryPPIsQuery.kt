package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGReturnType

class BGFindBinaryPPIsQuery(val nodeUri: String): BGRelationQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    init {
        taskMonitorTitle = "Searching for binary protein interactions..."
    }

   override fun generateQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
                "PREFIX fromNode: <"+nodeUri+">\n" +
                "SELECT DISTINCT ?toNode <intact> <http://purl.obolibrary.org/obo/RO_0002436> fromNode: \n" +
                "WHERE {\n" +
                "FILTER (?count = 2)\n" +
                "FILTER (fromNode: != ?toNode)\n" +
                "GRAPH <intact> {\n" +
                "?ppi has_agent: ?toNode } \n" +
                "{ SELECT ?ppi COUNT(?node) AS ?count\n" +
                "WHERE {\n" +
                "GRAPH <intact> {\n" +
                "?ppi has_agent: ?node .\n" +
                "?ppi has_agent: fromNode: .\n" +
                "}}}}"
    }
}

