package org.cytoscape.biogwplugin.internal.query
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader
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

enum class BGParsingType {
    TO_ARRAY, RELATIONS, MULTI, PARSING_BLOCK
}

abstract class BGQuery(val serviceManager: BGServiceManager, var type: BGReturnType, val parser: BGParser): AbstractTask(), Runnable {
    var completionBlocks: ArrayList<(BGReturnData?) -> Unit> = ArrayList()
    var returnData: BGReturnData? = null
    var client = HttpClients.createDefault();
    var taskMonitor: TaskMonitor? = null
    var taskMonitorText = "Searching..."
    var parsingBlock: ((BufferedReader) -> Unit)? = null
    var parseType = BGParsingType.PARSING_BLOCK

    abstract var queryString: String

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    override fun run() {
        taskMonitor?.setTitle(taskMonitorText)

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
            when (parseType) {
                BGParsingType.TO_ARRAY -> parseToTextArray(reader)
                BGParsingType.RELATIONS -> TODO()
                BGParsingType.MULTI -> TODO()
                BGParsingType.PARSING_BLOCK -> parsingBlock?.invoke(reader)
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


class BGNodeSearchQuery(serviceManager: BGServiceManager, override var queryString: String, returnType: BGReturnType, parser: BGParser): BGQuery(serviceManager, returnType, parser) {

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
}


open class BGMultiRelationsQuery(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGQuery(serviceManager, returnType, parser) {

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
}


class BGFetchPubmedIdQuery(serviceManager: BGServiceManager, val fromNodeUri: String, val relationUri: String, val toNodeUri: String): BGQuery(serviceManager, BGReturnType.PUBMED_ID, serviceManager.server.parser) {
    override var queryString: String
        get() = generateQueryString(fromNodeUri, relationUri, toNodeUri) //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

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

    fun synchronousRun(completion: (String?) -> Unit) {
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            val reader = BufferedReader(InputStreamReader(stream))
//            val pubmedIDs = parser.parsePubmedIdsToTextArray(reader, type)
//            if (pubmedIDs.size > 0) {
//                return pubmedIDs[0]
//            }
            parser.parsePubmedIdsToTextArray(reader, type) {
                if (it == null) {
                    throw Exception("Invalid return data!")
                }
                returnData = it
                val pubmedIds = it.pubmedIDlist
                if (pubmedIds.size == 0) throw Exception("No results found.")
                completion(pubmedIds[0])
                return@parsePubmedIdsToTextArray
            }
        }
    }

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


class BGQuickFetchNodeQuery(serviceManager: BGServiceManager, val nodeName: String, val nodeType: BGNodeType, parser: BGParser): BGQuery(serviceManager, BGReturnType.NODE_LIST_DESCRIPTION_TAXON, parser) {

    var nodeTypeGraph = when (nodeType) {
        BGNodeType.Gene -> "<refseq>"
        BGNodeType.Protein -> "<refprot>"
        BGNodeType.GO -> "<go-basic-inf>"
        BGNodeType.Taxon -> "<cco>"
        else -> {
            "?anyGraph"
        }
    }

    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            taskMonitor?.setTitle("Loading results...")
            val reader = BufferedReader(InputStreamReader(stream))
            parser.parseNodesToTextArray(reader, BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
    }

    override var queryString = "BASE <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX inheres_in: <http://purl.obolibrary.org/obo/RO_0000052>  \n" +
            "PREFIX taxaGraph: <cco>\n" +
            "PREFIX sio:  <http://semanticscience.org/resource/>  \n" +
            "SELECT DISTINCT ?resourceUri ?value ?name ?taxonName\n" +
            "WHERE {  \n" +
            "\tFILTER ( ?value = '"+nodeName+"') . \n" +
            "\tGRAPH "+nodeTypeGraph+" {  \n" +
            "\t\t?resourceUri skos:prefLabel|skos:altLabel ?value .\n" +
            " ?resourceUri skos:definition ?name .\n" +
            " ?resourceUri inheres_in: ?taxon .\n" +
            "\t} \n" +
            "GRAPH taxaGraph: {\n" +
            "?taxon rdfs:subClassOf sio:SIO_010000 . \n" +
            "?taxon skos:prefLabel ?taxonName\n" +
            "}  \n" +
            "}\n" +
            "ORDER BY ?resourceUri"
}




class BGFetchNodeNameForURIQuery(val serviceManager: BGServiceManager, val nodeUri: String) {

    var queryString = "BASE <http://www.semantic-systems-biology.org/>\n" +
            "SELECT DISTINCT ?name\n" +
            "WHERE {\n" +
            "<"+nodeUri+"> skos:prefLabel ?name .\n" +
            "\n" +
            "}"

    fun runSynchronously(): String? {
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            val reader = BufferedReader(InputStreamReader(stream))
            reader.readLine()
            val name = reader.readLine()
            if (name.length > 0) {
                return name
            }
        }
        return null
    }

    fun encodeUrl(): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"

        val queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(queryString, "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)

        return queryURL
    }
}



