package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGFetchPubmedIdQuery(serviceManager: BGServiceManager, val fromNodeUri: String, val relationUri: String, val toNodeUri: String): BGQuery(serviceManager, BGReturnType.PUBMED_ID, serviceManager.server.parser) {
    override var queryString: String = ""
        get() = generateQueryString(fromNodeUri, relationUri, toNodeUri) //To change initializer of created properties use File | Settings | File Templates.

    init {
        parsingBlock = {
            parser.parsePubmedIdsToTextArray(it, type) {
                taskMonitor?.setTitle("Loading results...")
                returnData = it
                runCompletions()
            }
        }
    }

    /*
    override fun run() {
        val stream = encodeUrl()?.openStream()
        if (stream != null) {

        }
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode

            val data = EntityUtils.toString(response.entity)
            if (statusCode > 204) {
                throw Exception("Server connection failed with code "+statusCode+": \n\n"+data)
            }
            //print(data)
            val reader = BufferedReader(StringReader(data))
            client.close()


            taskMonitor?.setTitle("Loading results...")
            parser.parsePubmedIdsToTextArray(reader, type) {
                taskMonitor?.setTitle("Loading results...")
                returnData = it
                runCompletions()
            }
        }
    }
    */

    fun generateQueryString(fromNodeUri: String, relationUri: String, toNodeUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX has_source: <http://semanticscience.org/resource/SIO_000253> \n" +
                "SELECT DISTINCT ?pubmedId\n" +
                "WHERE {\n" +
                "GRAPH ?graph {  \n" +
                "?triple rdf:subject <"+fromNodeUri+"> .  \n" +
                "?triple rdf:predicate <"+relationUri+"> .  \n" +
                "?triple rdf:object <"+toNodeUri+"> . \n" +
                "?triple has_source: ?pubmedId .\n" +
                "}}"
    }
}