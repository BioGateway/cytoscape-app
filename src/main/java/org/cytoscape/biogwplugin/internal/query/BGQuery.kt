package org.cytoscape.biogwplugin.internal.query
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGRelationDirection {
    TO, FROM
}

enum class BGParsingType {
    TO_ARRAY, RELATIONS, MULTI, PARSE_FUNCTION, METADATA
}


enum class BGQueryType(val returnType: BGReturnType) {
    NODE(BGReturnType.NODE_LIST_DESCRIPTION),
    RELATION(BGReturnType.RELATION_TRIPLE),
    MULTI_RELATION(BGReturnType.RELATION_MULTIPART),
    METADATA(BGReturnType.METADATA_FIELD)
}

abstract class BGTypedQuery(val type: BGQueryType, val serviceManager: BGServiceManager): AbstractTask(), Runnable {
    private var taskMonitor: TaskMonitor? = null
    var taskMonitorTitle = ""
    val client = HttpClients.createDefault()

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    abstract fun generateQueryString(): String

    val futureReturnData = CompletableFuture<BGReturnData>()

    override fun run() {
        taskMonitor?.setTitle(taskMonitorTitle)

        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode

            val data = EntityUtils.toString(response.entity)
            if (statusCode > 204) {
                throw Exception("Server connection failed with code " + statusCode + ": \n\n" + data)
            }
            val reader = BufferedReader(StringReader(data))
            client.close()
            taskMonitor?.setTitle("Loading results...")
            val returnData = serviceManager.server.parser.parseData(reader, type)
            futureReturnData.complete(returnData)
        } else {
            futureReturnData.completeExceptionally(Exception("Invalid URI!"))
        }
    }

    fun encodeUrl(): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"
        val queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(generateQueryString(), "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)
        return queryURL
    }
}


@Suppress("LocalVariableName")
abstract class BGQuery(val serviceManager: BGServiceManager, var type: BGReturnType): AbstractTask(), Runnable {
    var completionBlocks: ArrayList<(BGReturnData?) -> Unit> = ArrayList()
    var returnData: BGReturnData? = null
    var client = HttpClients.createDefault()!!
    var taskMonitor: TaskMonitor? = null
    var taskMonitorTitle = "Searching..."
    var parsingBlock: ((BufferedReader) -> Unit)? = null
    var parseType = when (type) {
        BGReturnType.NODE_LIST -> BGParsingType.TO_ARRAY
        BGReturnType.RELATION_TRIPLE -> BGParsingType.RELATIONS
        BGReturnType.RELATION_MULTIPART -> BGParsingType.RELATIONS
        BGReturnType.PUBMED_ID -> BGParsingType.TO_ARRAY
        BGReturnType.METADATA_FIELD -> BGParsingType.METADATA
        else -> {
            BGParsingType.PARSE_FUNCTION
        }
    }
    val parser = serviceManager.server.parser
    val futureReturnData = CompletableFuture<BGReturnData>()

    abstract fun generateQueryString(): String

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
                    returnData = parser.parseRelations(reader, type, taskMonitor)
                    taskMonitor?.setTitle("Loading results...")
                    runCompletions()
                }
                BGParsingType.PARSE_FUNCTION -> {
                    var function = parsingBlock ?: throw Exception("Parse function not provided!")
                    function.invoke(reader)
                }
                BGParsingType.METADATA -> {
                    returnData = parser.parseMetadata(reader, type)
                }
                else -> {
                }
            }
        }
        futureReturnData.complete(returnData)
    }

    fun parseToTextArray(bufferedReader: BufferedReader) {
        returnData = parser.parseNodesToTextArray(bufferedReader, type)
        runCompletions()
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
        val queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(generateQueryString(), "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)
        return queryURL
    }
}



