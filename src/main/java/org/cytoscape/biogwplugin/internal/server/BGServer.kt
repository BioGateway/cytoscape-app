package org.cytoscape.biogwplugin.internal.server

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.query.BGNodeFetchQuery
import org.cytoscape.biogwplugin.internal.query.QueryParameter
import org.cytoscape.biogwplugin.internal.query.QueryTemplate
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by sholmas on 26/05/2017.
 */


class BGServer(private val serviceManager: BGServiceManager) {

    class BGCache() {
        // A cache of BGNodes, which are a local representation of the node found on the server.
        // Note that this cache is independent of the CyNodes and CyNetworks.
        var nodeCache = HashMap<String, BGNode>()

        var relationTypes = ArrayList<BGRelationType>()
        var queryTemplates = HashMap<String, QueryTemplate>()

        fun addNode(node: BGNode) {
            // Only add a node if it's not already been added.
            // TODO: Merge the new node data into the previously cached node.
            if (nodeCache.contains(node.uri)) return
            nodeCache.set(node.uri, node)
        }
    }

    val cache = BGCache()
    val parser = BGParser()

    init {
        loadXMLFileFromServer()
    }

    fun getNode(uri: String, completion: (BGNode?) -> Unit) {
        var node = cache.nodeCache.get(uri)
        if (node != null) {
            completion(node)
            return
        }
        for (network in serviceManager.networkManager.networkSet) {
            node = getNodeFromCyNetwork(uri, network)
            if (node != null) {
                cache.addNode(node)
                completion(node)
                return
            }
        }
        // Missed both our cache and the data stored in the CyNetwork. Seems like we have to fetch it from Biogateway.
        getNodeFromServer(uri) {
            completion(it)
        }
    }

    private fun  getNodeFromServer(uri: String, completion: (BGNode?) -> Unit) {
        val query = BGNodeFetchQuery(serviceManager.serverPath, uri, serviceManager.server.parser)
        val stream = query.encodeUrl()?.openStream()
        if (stream != null) {
            parser.parseNodes(stream) {
                val data = it ?: throw Exception("Invalid return data!")
                val node = data.nodeData.get(uri)
                completion(node)
            }
        }
    }

    fun getNodeFromCyNetwork(uri: String, network: CyNetwork): BGNode? {
        val nodeTable = network.defaultNodeTable
        val nodes = getCyNodesWithValue(network, nodeTable, "identifier uri", uri)

        if (nodes.size == 1) {
            val cyNode = nodes.iterator().next() // Get the first (next) node.
            val nodeName = nodeTable.getRow(cyNode.suid).get("name", String::class.java)

        }


        return null
    }

    private fun getCyNodesWithValue(network: CyNetwork, nodeTable: CyTable, columnName: String, value: Any): Set<CyNode> {
        var nodes = HashSet<CyNode>()
        val matchingRows = nodeTable.getMatchingRows(columnName, value)

        val primaryKeyColumnName = nodeTable.primaryKey.name
        for (row in matchingRows) {
            val nodeId = row.get(primaryKeyColumnName, Long::class.java) ?: continue
            val node = network.getNode(nodeId) ?: continue
            nodes.add(node)
        }
        return nodes
    }


    private fun loadXMLFileFromServer() {
        try {
            val queryFileUrl = URL("https://dl.dropboxusercontent.com/u/32368359/BiogatewayQueries.xml")
            val connection = queryFileUrl.openConnection()
            val inputStream = connection.getInputStream()
            parseXMLConfigFile(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun parseXMLConfigFile(stream: InputStream) {

        val queryTemplateHashMap = java.util.HashMap<String, QueryTemplate>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        try {
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(stream)
            doc.documentElement.normalize()
            val relationTypesNode = (doc.getElementsByTagName("relationTypes").item(0) as? Element) ?: throw Exception("relationTypes element not found in XML file!")
            val rList = relationTypesNode.getElementsByTagName("relationType") ?: throw Exception()

            var relationTypes = ArrayList<BGRelationType>()

            for (index in 0..rList.length -1) {
                val element = rList.item(index) as? Element
                val name = element?.getAttribute("name")
                val uri = element?.textContent

                if (name != null && uri != null) {
                    relationTypes.add(BGRelationType(uri, name))
                }
            }
            cache.relationTypes = relationTypes

            // Will crash if the queryList tag isn't present.
            val queryList = (doc.getElementsByTagName("queryList").item(0) as? Element) ?: throw Exception("queryList element not found in XML file!")
            val nList = queryList.getElementsByTagName("query")

            for (temp in 0..nList.length - 1) {
                val nNode = nList.item(temp)

                if (nNode.nodeType == Node.ELEMENT_NODE) {
                    val qElement = nNode as Element
                    val queryName = qElement.getAttribute("name")
                    val queryDescription = qElement.getElementsByTagName("description").item(0).textContent
                    val sparqlString = qElement.getElementsByTagName("sparql").item(0).textContent.replace("\t", "") // Remove tabs from the XML file. (They might be added "for show").
                    val query = QueryTemplate(queryName, queryDescription, sparqlString)
                    val parameterList = qElement.getElementsByTagName("parameter")

                    for (pIndex in 0..parameterList.length - 1) {

                        if (parameterList.item(pIndex).nodeType == Node.ELEMENT_NODE) {
                            val parameter = parameterList.item(pIndex) as Element
                            val pId = parameter.getAttribute("id")
                            val pTypeString = parameter.getAttribute("type")
                            val pName = parameter.getElementsByTagName("name").item(0).textContent

                            val pType = when (pTypeString) {
                                "text" -> QueryParameter.ParameterType.TEXT
                                "checkbox" -> QueryParameter.ParameterType.CHECKBOX
                                "combobox" -> QueryParameter.ParameterType.COMBOBOX
                                "uniprot_id" -> QueryParameter.ParameterType.UNIPROT_ID
                                "ontology" -> QueryParameter.ParameterType.ONTOLOGY
                                "optionalUri" -> QueryParameter.ParameterType.OPTIONAL_URI
                                else -> QueryParameter.ParameterType.TEXT
                            }
                            val qParameter = QueryParameter(pId, pName, pType)
                            val optionsList = parameter.getElementsByTagName("option")

                            for (oIndex in 0..optionsList.length - 1) {
                                if (optionsList.item(oIndex).nodeType == Node.ELEMENT_NODE) {
                                    val oElement = optionsList.item(oIndex) as Element
                                    val oName = oElement.getAttribute("name")
                                    val oValue = oElement.textContent
                                    qParameter.addOption(oName, oValue)
                                } }
                            query.addParameter(qParameter)
                        } }
                    queryTemplateHashMap.put(queryName, query)
                } }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        cache.queryTemplates = queryTemplateHashMap
    }

    fun createBiopaxURL(queryData: String, returnType: String, options: String): URL? {
        val queryURL: URL
        try {
            queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(queryData, "UTF-8") + "&format=" + URLEncoder.encode(returnType, "UTF-8") + "&" + options)

        } catch (e: MalformedURLException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return null

        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        }

        return queryURL
    }
}
