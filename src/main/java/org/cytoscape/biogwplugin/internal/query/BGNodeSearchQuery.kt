package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.StringReader

class BGNodeSearchQuery(serviceManager: BGServiceManager, override var queryString: String, returnType: BGReturnType, parser: BGParser): BGQuery(serviceManager, returnType, parser) {

    init {
        taskMonitorTitle = "Searching for nodes..."
        parseType = BGParsingType.TO_ARRAY
    }

    /*
    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            if (statusCode > 204) {
                throw Exception("Server connection failed with code "+ statusCode)
            }
            val data = EntityUtils.toString(response.entity)
            val reader = BufferedReader(StringReader(data))
            client.close()
            taskMonitor?.setTitle("Loading results...")
            parser.parseNodesToTextArray(reader, type) {
                returnData = it as? BGReturnNodeData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }

        /*
        stream = encodeUrl()?.openStream()
        stream?.let {
            parser.parseNodesToTextArray(it, type) {
                returnData = it as? BGReturnNodeData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
        */
    }
    */
}