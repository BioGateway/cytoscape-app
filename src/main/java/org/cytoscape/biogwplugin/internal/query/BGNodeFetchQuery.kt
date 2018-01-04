package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.InputStreamReader

class BGNodeFetchQuery(serviceManager: BGServiceManager, val nodeUri: String, parser: BGParser, type: BGReturnType): BGQuery(serviceManager, type, parser) {

    /// This is running synchronously and without the main HTTPClient.
    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            taskMonitor?.setTitle("Loading results...")
            val reader = BufferedReader(InputStreamReader(stream))
            parser.parseNodesToTextArray(reader, type) {
                returnData = it ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
    }

    override var queryString = "BASE   <http://www.semantic-systems-biology.org/> \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  \n" +
            "PREFIX term_id: <"+ nodeUri +">  \n" +
            "SELECT DISTINCT term_id: ?label ?description\n" +
            "WHERE {\n"+
            "term_id: skos:prefLabel ?label .\n" +
            "term_id: skos:definition ?description .\n" +
            "} \n"
}