package org.cytoscape.biogwplugin.internal.server

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.*
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Constants.BG_SHOULD_USE_BG_DICT
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyNetwork
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit


class BGServer(private val serviceManager: BGServiceManager) {

    class BGCache {
        // A cache of BGNodes, which are a local representation of the node found on the server.
        // Note that this cache is independent of the CyNodes and CyNetworks.
        var nodeCache = HashMap<String, BGNode>()

        var relationTypeMap = HashMap<String, BGRelationType>()
        var activeGraphs = arrayOf("intact", "tf-tg", "goa", "refprot").toHashSet()

        val filteredRelationTypeMap: Map<String, BGRelationType> get() {
            return relationTypeMap.filter { activeGraphs.contains(it.key.split(":").first()) }
        }



        fun getRelationTypesForURI(uri: String): Collection<BGRelationType>? {
            val types = relationTypeMap.values.filter { it.uri == uri }
            return if (types.size > 0) types else null
        }

        fun getRelationTypeForURIandGraph(uri: String, graph: String): BGRelationType? {
            if (graph.startsWith("?")) {
                val types = getRelationTypesForURI(uri)
                return types?.first()
            }
            val relationWithGraph = relationTypeMap.get(Utility.createRelationTypeIdentifier(uri, graph))
            if (relationWithGraph != null) return relationWithGraph
            val types = getRelationTypesForURI(uri)
            return types?.first()
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

    fun addGraphFilter(graph: String) {
        cache.activeGraphs.add(graph)
    }
    fun removeGraphFilter(graph: String) {
        cache.activeGraphs.remove(graph)
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

            val fetchedNode = getNodeFromServer(node.uri)
            if (fetchedNode != null) {
                node.name = fetchedNode.name
                node.description = fetchedNode.description
                node.isLoaded = true
                cache.addNode(node)
                for (cyNode in node.cyNodes) {
                    fetchedNode.name?.let {
                        cyNode.setName(it, cyNode.networkPointer)
                    }
                    fetchedNode.description?.let {
                        cyNode.setDescription(it, cyNode.networkPointer)
                    }
                }
            }
        } else {
            cache.addNode(node)
            node.isLoaded = true
        }
    }



    private fun getNodeDataFromNetworks(uri: String): BGNode? {
        val networks = serviceManager.networkManager?.networkSet ?: return null
        for (network in networks) {
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

    
    fun loadNodesFromServerSynchronously(nodes: Collection<BGNode>) {

        val unloadedNodes = nodes.filter { (it.description == null) or (it.name == null) }
        nodes.toHashSet().subtract(unloadedNodes).forEach {
            cache.addNode(it)
            it.isLoaded = true
        }
        val unloadedNodeUris = unloadedNodes.map { it.uri }
        val fetchedNodes = getNodesFromDictionaryServer(unloadedNodeUris)
        unloadedNodes.forEach { node ->
            val fetchedNode = fetchedNodes[node.uri]
            if (fetchedNode != null) {
                updateNodeData(node, fetchedNode)
            }
        }

    }

    private fun updateNodeData(node: BGNode, fetchedNode: BGNode) {
        node.name = fetchedNode.name
        node.description = fetchedNode.description
        node.isLoaded = true
        cache.addNode(node)
        for (cyNode in node.cyNodes) {
            fetchedNode.name?.let {
                cyNode.setName(it, cyNode.networkPointer)
            }
            fetchedNode.description?.let {
                cyNode.setDescription(it, cyNode.networkPointer)
            }
        }
    }

    private fun getNodesFromDictionaryServer(nodeUris: Collection<String>): HashMap<String, BGNode> {
        val query = BGMultiNodeFetchMongoQuery(serviceManager, nodeUris)
        query.run()
        val data = query.futureReturnData.get() as BGReturnNodeData

        return data.nodeData
    }

    private fun  getNodeFromServer(uri: String): BGNode? {
        if (!uri.startsWith("http://")) {
            return null
        }

        val nodeType = BGNode.static.nodeTypeForUri(uri)


        val query = if (BG_SHOULD_USE_BG_DICT && (nodeType == BGNodeType.Protein || nodeType == BGNodeType.Gene || nodeType == BGNodeType.GO)) {
            BGNodeFetchMongoQuery(serviceManager, uri)
        } else {
            BGNodeFetchQuery(serviceManager, uri)
        }

        query.run()
        val data = query.futureReturnData.get(10, TimeUnit.SECONDS ) as? BGReturnNodeData
        val node = data?.nodeData?.get(uri)
        return node

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


    fun loadXMLFileFromServer() {
        try {
            val queryFileUrl = URL(Constants.BG_CONFIG_FILE_URL)
            val connection = queryFileUrl.openConnection()
            val inputStream = connection.getInputStream()
            BGConfigParser.parseXMLConfigFile(inputStream, cache)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
