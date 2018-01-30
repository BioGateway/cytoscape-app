package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

abstract class BGCallableQuery(val serviceManager: BGServiceManager, var type: BGReturnType, val taskMonitorTitle: String = "Running query..."): AbstractTask(), Runnable {

    abstract fun createQueryString(): String

    var returnFuture = CompletableFuture<BGReturnData>()

    var taskMonitor: TaskMonitor? = null
    var client = serviceManager.httpClient
    var parseType = BGParsingType.PARSE_FUNCTION
    val parser = serviceManager.server.parser

    var parseFunction: ((BufferedReader, BGReturnType) -> BGReturnData)? = null

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    override fun run() {
        taskMonitor?.setTitle(taskMonitorTitle)

        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode

            val data = EntityUtils.toString(response.entity)
            if (statusCode > 204) {
                throw Exception("Server connection failed with code "+statusCode+": \n\n"+data)
            }
            val stringReader = StringReader(data)
            val reader = BufferedReader(stringReader)


            client.close()
            taskMonitor?.setTitle("Loading results...")
            when (parseType) {
                BGParsingType.TO_ARRAY -> returnFuture.complete(parser.parseNodesToTextArray(reader, type))
                BGParsingType.RELATIONS -> {
                    val returnData = parser.parseRelations(reader, type, taskMonitor)
                    taskMonitor?.setTitle("Loading results...")
                    returnFuture.complete(returnData)
                }
                BGParsingType.PARSE_FUNCTION -> {
                    val function = parseFunction ?: throw Exception("Parse function not set!")
                    val returnData = function(reader, type)
                    taskMonitor?.setTitle("Loading results...")
                    returnFuture.complete(returnData)
                }
                else -> {
                    returnFuture.cancel(true)
                }
            }
        }
        returnFuture.cancel(true)
    }

    override fun cancel() {
        client.close()
        serviceManager.server.parser.cancel()
        super.cancel()
        throw Exception("Cancelled.")
    }

    private fun encodeUrl(): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"
        val queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(createQueryString(), "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)
        return queryURL
    }

}