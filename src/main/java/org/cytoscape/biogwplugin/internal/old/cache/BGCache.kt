package org.cytoscape.biogwplugin.internal.old.cache

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.old.query.BGNode
import org.cytoscape.biogwplugin.internal.old.query.BGQueryBuilderModel
import org.cytoscape.biogwplugin.internal.old.query.QueryTemplate
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable

import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by sholmas on 24/03/2017.
 */
class BGCache(private val serviceManager: BGServiceManager) {

    private val nodeCache = HashMap<String, BGNode>()
    var relationTypes = HashMap<String, String>()
        private set
    private var queryTemplateHashMap: HashMap<String, QueryTemplate> = HashMap()

    init {

        // Load initial data

        // Load the XML file

        // Load a list of Relation Types
//        val relationTypesQuery = BGFetchRelationTypesQuery(BGServiceManager.SERVER_PATH)
//        relationTypesQuery.addCallback { this.relationTypes = relationTypesQuery.returnData  }
//        relationTypesQuery.run()
        loadXMLFileFromServer()
    }

    private fun loadXMLFileFromServer() {
        try {
            val queryFileUrl = URL("https://dl.dropboxusercontent.com/u/32368359/BiogatewayQueries.xml")
            val connection = queryFileUrl.openConnection()
            val `is` = connection.getInputStream()
            queryTemplateHashMap = BGQueryBuilderModel.parseXMLFile(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun getQueryTemplateHashMap(): HashMap<String, QueryTemplate> {
        // TODO: Make this asynchronous.
        if (queryTemplateHashMap.size == 0) {
            loadXMLFileFromServer()
        }
        return queryTemplateHashMap
    }

    fun getNodeWithURI(uri: String): BGNode? {
        // TODO: Make this asynchronous.
        var node: BGNode? = nodeCache[uri]
        if (node == null) {
            println("BGCache Warning: Node $uri not found. Attempting to fetch it from the current CyNetworks.")
            for (network in serviceManager.networkManager.networkSet) {
                node = fetchNodeFromCyNetwork(uri, network)
                if (node != null) {
                    nodeCache.put(uri, node)
                    return node
                }
            }
        }
        return node
    }

    fun getNodesWithURIs(uris: Collection<String>): ArrayList<BGNode> {
        // TODO: Make this asynchronous.
        val nodes = ArrayList<BGNode>()
        for (uri in uris) {
            val node = getNodeWithURI(uri)
            if (node != null) {
                nodes.add(node)
            }
        }
        return nodes
    }

    fun addNode(node: BGNode) {
        nodeCache.put(node.URI, node)
    }


    private fun fetchNodeFromCyNetwork(nodeUri: String, network: CyNetwork): BGNode? {
        val table = network.defaultNodeTable
        val nodes = getNodesWithValue(network, table, "identifier uri", nodeUri)
        if (nodes.size == 1) {
            // TODO: Make sure that the identifier uri is found!
            val cyNode = nodes.iterator().next()
            val commonName = table.getRow(cyNode.suid).get("name", String::class.java)
            val bgNode = BGNode(nodeUri)
            bgNode.commonName = commonName
            return bgNode
        } else if (nodes.size > 1) {
            println("BGCache Warning: CyNetwork inconsistency: Found multiple nodes with same OPTIONAL_URI in the same network!")
            return null
        } else {
            println("BGCache Warning: Did not find missing node in CyNetwork table either.")
            return null
        }
    }

    private fun getNodesWithValue(network: CyNetwork, table: CyTable, columnName: String, value: Any): Set<CyNode> {
        val matchingRows = table.getMatchingRows(columnName, value)
        val nodes = HashSet<CyNode>()
        val primaryKeyColname = table.primaryKey.name
        for (row in matchingRows) {
            val nodeId = row.get(primaryKeyColname, Long::class.java) ?: continue
            val node = network.getNode(nodeId) ?: continue
            nodes.add(node)
        }
        return nodes
    }

    fun getNameForRelationType(relationTypeURI: String): String? {
        return relationTypes[relationTypeURI]
    }
}
