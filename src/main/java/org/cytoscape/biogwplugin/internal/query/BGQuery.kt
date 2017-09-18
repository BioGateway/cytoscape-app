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

abstract class BGQuery(val serviceManager: BGServiceManager, var type: BGReturnType, val parser: BGParser): AbstractTask(), Runnable {
    var completionBlocks: ArrayList<(BGReturnData?) -> Unit> = ArrayList()
    var returnData: BGReturnData? = null
    var client = HttpClients.createDefault();
    var taskMonitor: TaskMonitor? = null

    abstract var queryString: String

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
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
            print(data)
            val reader = BufferedReader(StringReader(data))
            client.close()
            taskMonitor?.setTitle("Parsing results...")
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

class BGRelationsQuery(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGQuery(serviceManager, returnType, parser) {

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
            taskMonitor?.setTitle("Parsing results...")
            parser.parseRelations(reader, returnType) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
    }
}

class BGMultiRelationsQuery(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGQuery(serviceManager, returnType, parser) {

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
            taskMonitor?.setTitle("Parsing results...")
            parser.parsePathway(reader, returnType) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                taskMonitor?.setTitle("Loading results...")
                runCompletions()
            }
        }
    }
}


class BGNodeFetchQuery(serviceManager: BGServiceManager, val nodeUri: String, parser: BGParser, type: BGReturnType): BGQuery(serviceManager, type, parser) {

    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            taskMonitor?.setTitle("Parsing results...")
            val reader = BufferedReader(InputStreamReader(stream))
            parser.parseNodesToTextArray(reader, type) {
                returnData = it as? BGReturnNodeData ?: throw Exception("Invalid return data!")
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


            taskMonitor?.setTitle("Parsing results...")
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
                "GRAPH <tf-tg> {  \n" +
                "?triple rdf:subject <"+fromNodeUri+"> .  \n" +
                "?triple rdf:predicate <"+relationUri+"> .  \n" +
                "?triple rdf:object <"+toNodeUri+"> . \n" +
                "?triple has_source: ?pubmedId .\n" +
                "}}"
    }
}


class BGQuickFetchNodeQuery(serviceManager: BGServiceManager, val nodeName: String, val nodeType: BGNodeType, parser: BGParser): BGQuery(serviceManager, BGReturnType.NODE_LIST_DESCRIPTION_TAXON, parser) {

    var nodeTypeGraph = when (nodeType) {
        BGNodeType.GENE -> "<refseq>"
        BGNodeType.PROTEIN -> "<refprot>"
        BGNodeType.GO -> "<go-basic-inf>"
        BGNodeType.ANY -> "?anyGraph"
    }

    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            taskMonitor?.setTitle("Parsing results...")
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
            "SELECT DISTINCT ?resourceUri ?value ?description ?taxonName\n" +
            "WHERE {  \n" +
            "\tFILTER ( ?value = '"+nodeName+"') . \n" +
            "\tGRAPH "+nodeTypeGraph+" {  \n" +
            "\t\t?resourceUri skos:prefLabel|skos:altLabel ?value .\n" +
            " ?resourceUri skos:definition ?description .\n" +
            " ?resourceUri inheres_in: ?taxon .\n" +
            "\t} \n" +
            "GRAPH taxaGraph: {\n" +
            "?taxon rdfs:subClassOf sio:SIO_010000 . \n" +
            "?taxon skos:prefLabel ?taxonName\n" +
            "}  \n" +
            "}\n" +
            "ORDER BY ?resourceUri"
}


