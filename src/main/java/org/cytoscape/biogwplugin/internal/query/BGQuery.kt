package org.cytoscape.biogwplugin.internal.query
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.server.BGServer
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGRelationDirection {
    TO, FROM
}

abstract class BGQuery(val serviceManager: BGServiceManager, var type: BGReturnType, val parser: BGParser): AbstractTask(), Runnable {
    var completionBlocks: ArrayList<(BGReturnData?) -> Unit> = ArrayList()
    var returnData: BGReturnData? = null
    abstract var queryString: String

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

class BGNodeSearchQuery(serviceManager: BGServiceManager, override var queryString: String, returnType: BGReturnType, parser: BGParser): BGQuery(serviceManager, returnType, parser) {
    var client = HttpClients.createDefault();

    override fun run(taskMonitor: TaskMonitor?) {
        run()
    }

    override fun cancel() {
        client.close()
        super.cancel()
    }

    override fun run() {
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            if (statusCode > 204) {
                throw Exception("Server connection failed with code "+ statusCode)
            }
            val data = EntityUtils.toString(response.entity)
            print(data)
            val reader = BufferedReader(StringReader(data))
            client.close()
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
}

class BGGenericQuery(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGQuery(serviceManager, returnType, parser) {
    override fun run(taskMonitor: TaskMonitor?) {
        run()
    }
    var client = HttpClients.createDefault();

    override fun run() {
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            val data = EntityUtils.toString(response.entity)
            print(data)
            val reader = BufferedReader(StringReader(data))
            client.close()
            parser.parseRelations(reader, returnType) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }

        /*
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            parser.parseRelations(stream, returnType) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        } */
    }
}
class BGRelationsQuery(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGQuery(serviceManager, returnType, parser) {
    override fun run(taskMonitor: TaskMonitor?) {
        run()
    }

    val client = HttpClients.createDefault()

    override fun cancel() {
        client.close()
        super.cancel()
    }

    override fun run() {
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            if (statusCode > 204) {
                throw Exception("Server connection failed with code "+statusCode)
            }
            val data = EntityUtils.toString(response.entity)
            print(data)
            val reader = BufferedReader(StringReader(data))
            client.close()
            parser.parseRelations(reader, returnType) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
        /*

        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            parser.parseRelations(stream, returnType) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        } */
    }
}

class BGNodeFetchQuery(serviceManager: BGServiceManager, val nodeUri: String, parser: BGParser, type: BGReturnType): BGQuery(serviceManager, type, parser) {
    override fun run(taskMonitor: TaskMonitor?) {
        run()
    }

    override fun run() {
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            val reader = BufferedReader(InputStreamReader(stream))
            parser.parseNodesToTextArray(reader, type) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
    }

    override var queryString = "BASE   <http://www.semantic-systems-biology.org/> \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  \n" +
            "PREFIX term_id: <"+ nodeUri +">  \n" +
            "PREFIX graph: <cco>  \n" +
            "SELECT term_id: ?label ?description\n" +
            "WHERE {  \n" +
            " GRAPH graph: {  \n" +
            "  term_id: skos:prefLabel ?label .\n" +
            " term_id: skos:definition ?description .\n" +
            " } } \n"
}



