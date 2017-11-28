package org.cytoscape.biogwplugin.internal.server

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.*
import org.cytoscape.biogwplugin.internal.query.BGNodeFetchQuery
import org.cytoscape.biogwplugin.internal.query.QueryTemplate
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyNetwork
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder


class BGServer(private val serviceManager: BGServiceManager) {

    class BGCache() {
        // A cache of BGNodes, which are a local representation of the node found on the server.
        // Note that this cache is independent of the CyNodes and CyNetworks.
        var nodeCache = HashMap<String, BGNode>()

        var relationTypeMap = HashMap<String, BGRelationType>()

        fun getRelationTypesForURI(uri: String): Collection<BGRelationType> {
            return relationTypeMap.values.filter { it.uri == uri }
        }

        fun getRelationTypeForURIandGraph(uri: String, graph: String): BGRelationType? {
            if (graph.startsWith("?")) {
                val types = getRelationTypesForURI(uri)
                return types.first()
            }
            val relationWithGraph = relationTypeMap.get(Utility.createRelationTypeIdentifier(uri, graph))
            if (relationWithGraph != null) return relationWithGraph
            val types = getRelationTypesForURI(uri)
            return types.first()
        }

        val relationTypeDescriptions: LinkedHashMap<String, BGRelationType> get() {
            val relationTypes = LinkedHashMap<String, BGRelationType>()
            val relations = relationTypeMap.values.sortedBy { it.number }
            for (relation in relations) {
                relationTypes.put(relation.description, relation)
            }
            return relationTypes
        }

        var queryTemplates = HashMap<String, QueryTemplate>()

        fun addNode(node: BGNode) {
            // Only add a node if it's not already been added.
            // TODO: Merge the new node data into the previously cached node.
            if (nodeCache.contains(node.uri)) return
            nodeCache.set(node.uri, node)
        }

        fun getRelationsForName(name: String): Collection<BGRelationType> {
            return relationTypeMap.filter { it.value.name == name }.map { it.value }.toList()
        }
    }

    val cache = BGCache()
    val settings = BGSettings()
    val parser = BGParser(serviceManager)
    val networkBuilder = BGNetworkBuilder(serviceManager)

    init {
        loadXMLFileFromServer()
    }

    fun searchForExistingNode(uri: String): BGNode? {
        var node = cache.nodeCache[uri]

        if (node == null) {
            node = getNodeDataFromNetworks(uri)
        }
        return node
    }

    fun getNodeFromCacheOrNetworks(newNode: BGNode): BGNode {
        // Check if the node already exists in the cache.
        var node = cache.nodeCache[newNode.uri]

        if (node == null) {
            // If it doesn't exist, add the new node.
            node = newNode

            if ((node.description == null) or (node.name == null)) {
                val nodeFromNetwork = getNodeDataFromNetworks(node.uri)
                if (nodeFromNetwork != null) {
                    node = nodeFromNetwork
                    println("CyNetwork hit: "+node.name)
                }
            }
        } else {
            //println("Cache hit: "+node.name)
        }
        // The node should be added to cache even though not loaded, to avoid duplicating cache misses.
        cache.addNode(node)
        return node
    }

    fun loadDataForNode(node: BGNode) {
        if ((node.description == null) or (node.name == null)) {
            // Query the server for more info about this node.
            if (!node.uri.startsWith("http")) {
                return
            }

            getNodeFromServer(node.uri) {
                if (it != null) {
                    //println("Cache miss: "+it.name)
                    node.name = it.name
                    node.description = it.description
                    node.isLoaded = true
                    cache.addNode(node)
                    for (cyNode in node.cyNodes) {
                        it.name?.let {
                            cyNode.setName(it, cyNode.networkPointer)
                        }
                        it.description?.let {
                            cyNode.setDescription(it, cyNode.networkPointer)
                        }
                    }
                }
            }
        } else {
            cache.addNode(node)
            node.isLoaded = true
        }
    }


    private fun getNodeDataFromNetworks(uri: String): BGNode? {
        for (network in serviceManager.networkManager.networkSet) {
            if (network.defaultNodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) {
                continue
            }
            val node = getNodeFromCyNetwork(uri, network)
            if (node != null) {
                cache.addNode(node)
                node.isLoaded = true
                return node
            }
        }
        return null
    }

    private fun  getNodeFromServer(uri: String, completion: (BGNode?) -> Unit) {
        if (!uri.startsWith("http://")) {
            completion(null)
            return
        }
        val query = BGNodeFetchQuery(serviceManager, uri, serviceManager.server.parser, BGReturnType.NODE_LIST_DESCRIPTION)
        // TODO: Use the CloseableHttpClient. Because this might cause things to take a looooong time.
        val stream = query.encodeUrl()?.openStream()
        if (stream != null) {
            val reader = BufferedReader(InputStreamReader(stream))
            parser.parseNodesToTextArray(reader, BGReturnType.NODE_LIST_DESCRIPTION) {
                val data = it ?: throw Exception("Invalid return data!")
                val node = data.nodeData.get(uri)
                completion(node)
            }
        }
    }

    private fun getNodeFromCyNetwork(uri: String, network: CyNetwork): BGNode? {
        val nodeTable = network.defaultNodeTable
        val nodes = networkBuilder.getCyNodesWithValue(network, nodeTable, Constants.BG_FIELD_IDENTIFIER_URI, uri)

        for (cyNode in nodes) {
            val nodeName = nodeTable.getRow(cyNode.suid).get(Constants.BG_FIELD_NAME, String::class.java)
            val description = nodeTable.getRow(cyNode.suid).get(Constants.BG_FIELD_DESCRIPTION, String::class.java)
            if (nodeName != null && description != null) {
                val node = BGNode(uri, nodeName, description)
                return node
            }
        }
        return null
    }


    private fun loadXMLFileFromServer() {
        try {
            val queryFileUrl = URL(Constants.BG_CONFIG_FILE_URL)
            val connection = queryFileUrl.openConnection()
            val inputStream = connection.getInputStream()
            //parseXMLConfigFile(inputStream)
            BGConfigParser.parseXMLConfigFile(inputStream, cache)
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
