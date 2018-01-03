package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

open class BGMultiRelationsQuery(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGQuery(serviceManager, returnType, parser) {

    init {
        taskMonitorTitle = "Searching for relations..."
        parsingBlock = {
            parser.parseRelations(it, returnType, taskMonitor) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                taskMonitor?.setTitle("Loading results...")
                runCompletions()
            }
        }
    }

    // TODO: Verify that this override isn't needed.
    /*
    override fun run() {
        taskMonitor?.setTitle("Searching for relations...")
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
            parser.parseRelations(reader, returnType, taskMonitor) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                taskMonitor?.setTitle("Loading results...")
                runCompletions()
            }
        }
    }
    */
}