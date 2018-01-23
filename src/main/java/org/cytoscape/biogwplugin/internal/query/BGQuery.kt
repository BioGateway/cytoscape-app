package org.cytoscape.biogwplugin.internal.query
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGRelationDirection {
    TO, FROM
}

enum class BGParsingType {
    TO_ARRAY, RELATIONS, MULTI, PARSING_BLOCK
}

@Suppress("LocalVariableName")
abstract class BGQuery(val serviceManager: BGServiceManager, var type: BGReturnType): AbstractTask(), Runnable {
    var completionBlocks: ArrayList<(BGReturnData?) -> Unit> = ArrayList()
    var returnData: BGReturnData? = null
    var client = HttpClients.createDefault()!!
    var taskMonitor: TaskMonitor? = null
    open var taskMonitorTitle = "Searching..."
    var parsingBlock: ((BufferedReader) -> Unit)? = null
    var parseType = BGParsingType.PARSING_BLOCK
    val parser = serviceManager.server.parser

    abstract var queryString: String

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
            val reader = BufferedReader(StringReader(data))
            client.close()
            taskMonitor?.setTitle("Loading results...")
            when (parseType) {
                BGParsingType.TO_ARRAY -> parseToTextArray(reader)
                BGParsingType.RELATIONS -> {
                    parser.parseRelations(reader, type, taskMonitor) {
                    returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                    taskMonitor?.setTitle("Loading results...")
                    runCompletions()
                }}
                BGParsingType.PARSING_BLOCK -> parsingBlock?.invoke(reader)
                else -> {
                }
            }
        }
    }

    fun parseToTextArray(bufferedReader: BufferedReader) {
        parser.parseNodesToTextArray(bufferedReader, type) { returnData ->
            this.returnData = returnData
            runCompletions()
        }
    }

    override fun cancel() {
        client.close()
        serviceManager.server.parser.cancel()
        super.cancel()
        throw Exception("Cancelled.")
    }

    fun addCompletion(completion: (BGReturnData?) -> Unit) {
        completionBlocks.add(completion)
    }
    fun runCompletions() {
        for (completion in completionBlocks) {
            completion(returnData)
        }
    }

    fun encodeUrl(): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"
        val queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(queryString, "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)
        return queryURL
    }
}



