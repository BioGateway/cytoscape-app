package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.InputStreamReader

class BGNodeFetchQuery(serviceManager: BGServiceManager, val nodeUri: String): BGQuery(serviceManager, BGReturnType.NODE_LIST_DESCRIPTION, false) {

    /// This is running synchronously and without the main HTTPClient.

    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            taskMonitor?.setTitle("Loading results...")
            val reader = BufferedReader(InputStreamReader(stream))
            returnData = parser.parseNodesToTextArray(reader, type)
            runCompletions()
        }
    }

    /*
    override fun generateQueryString(): String {
        return "{ \"returnType\": \"tsv\", \"uris\": [\""+nodeUri+"\"] }"
    }
*/

    override fun generateQueryString(): String {
        return "BASE   <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  \n" +
                "PREFIX term_id: <" + nodeUri + ">  \n" +
                "SELECT DISTINCT term_id: ?label ?description\n" +
                "WHERE {\n" +
                "term_id: skos:prefLabel ?label .\n" +
                "term_id: skos:definition ?description .\n" +
                "} \n"
    }
}