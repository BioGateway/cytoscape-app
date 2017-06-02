package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.query.BGReturnNodeData
import org.cytoscape.biogwplugin.internal.query.BGReturnRelationsData
import org.cytoscape.biogwplugin.internal.server.BGServer
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import java.awt.image.ByteLookupTable
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGParserField(val fieldName: String) {
    URI("identifier uri"),
    COMMON_NAME("common name"),
    RELATION_TYPE("type")
}

enum class BGQueryType(val paremeterCount: Int) {
    NODE_QUERY(3),              // nodeUri, common_name, description
    RELATION_TRIPLE(3),         // nodeUri, relationUri, nodeUri
    RELATION_TRIPLE_NAMED(5)    // nodeUri, common_name, relationUri, nodeUri, common_name
}


class BGParser(private val serviceManager: BGServiceManager) {
    private val server = serviceManager.server

    fun parseNodesToTextArray(stream: InputStream, completion: (BGReturnNodeData?) -> Unit) {
        // TODO: Add exception handling.
        val reader = BufferedReader(InputStreamReader(stream))

        val columnNames = reader.readLine().split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
        val returnData = BGReturnNodeData(BGQueryType.NODE_QUERY, columnNames)

        reader.forEachLine {
            val lineColumns = it.split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
            returnData.addEntry(lineColumns)
        }
        completion(returnData)
    }

    fun parseRelations(stream: InputStream, queryType: BGQueryType, completion: (BGReturnRelationsData?) -> Unit) {
        val reader = BufferedReader(InputStreamReader(stream))

        val columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.toTypedArray()
        var returnData = BGReturnRelationsData(queryType, columnNames)

        reader.forEachLine {
            val lineColumns = it.split("\t").dropLastWhile {  it.isEmpty() }.toTypedArray()
            if (lineColumns.size != queryType.paremeterCount) throw Exception("Number of columns in data array must match the parameter count of the query type!")

            if (queryType == BGQueryType.RELATION_TRIPLE) {

            } else if (queryType == BGQueryType.RELATION_TRIPLE_NAMED) {
                val fromNodeUri = lineColumns[0]
                val fromNodeName = lineColumns[1]
                val relationUri = lineColumns[2]
                val toNodeUri = lineColumns[3]
                val toNodeName = lineColumns[4]

                var fromNode = server.getNodeFromCache(fromNodeUri)
                fromNode.name = fromNodeName
                var toNode = server.getNodeFromCache(toNodeUri)
                toNode.name = toNodeName
                val relationType = server.cache.relationTypes.get(relationUri) ?: throw NullPointerException("RelationType not found for this URI!")

                val relation = BGRelation(fromNode, relationType, toNode)

                returnData.relationsData.add(relation)
            }
        }
        completion(returnData)
    }
}

class BGNetworkBuilder(private val serviceManager: BGServiceManager) {
    val networkFactory = serviceManager.networkFactory
    val server = serviceManager.server

    fun createNetworkFromBGNodes(nodes: Collection<BGNode>): CyNetwork {
        var network = networkFactory.createNetwork()
        var table = network.defaultNodeTable
        table.createColumn("identifier uri", String::class.java, false)

        for (node in nodes) {
            addNodeToNetwork(node, network, table)
        }
        return network
    }

    fun addRelationsToNetwork(network: CyNetwork, relations: Collection<BGRelation>) {

        val nodeTable = network.defaultNodeTable
        val edgeTable = network.defaultEdgeTable

        // Find the unique nodes in the set of relations. Duplicates get overwritten.
        val uniqueNodes = HashMap<String, BGNode>()
        for (relation in relations) {
            uniqueNodes.put(relation.toNode.uri, relation.toNode)
            uniqueNodes.put(relation.fromNode.uri, relation.fromNode)
        }

        // Find or create the CyNodes for these BGNodes.
        val cyNodes = HashMap<String, CyNode>()
        for ((uri, bgNode) in uniqueNodes) {
            var node = getNodeWithUri(uri, network, nodeTable)
            if (node == null) {
                node = addNodeToNetwork(bgNode, network, nodeTable)
            }
            cyNodes.put(uri, node)
        }

        // Now we have all the CyNodes needed in the network to create the relations. Edges can be created.

        for (relation in relations) {
            val fromNode = cyNodes.get(relation.fromNode.uri) ?: throw Exception("CyNode not found!")
            val toNode = cyNodes.get(relation.toNode.uri) ?: throw Exception("CyNode not found!")
            val edge = addEdgeToNetwork(fromNode, toNode, network, edgeTable, relation.relationType)
        }
    }


    fun addEdgeToNetwork(from: CyNode, to: CyNode, network: CyNetwork, edgeTable: CyTable, relationType: BGRelationType): CyEdge {
        val edge = network.addEdge(from, to, true)
        edgeTable.getRow(edge.suid).set("identifier uri", relationType.uri)
        edgeTable.getRow(edge.suid).set("name", relationType.description)
        return edge
    }

    fun addNodeToNetwork(node: BGNode, network: CyNetwork, table: CyTable): CyNode {
        val newNode = network.addNode()
        table.getRow(newNode.suid).set("identifier uri", node.uri)
        table.getRow(newNode.suid).set("name", node.name)
        return newNode
    }

    fun getNodeWithUri(uri: String, network: CyNetwork, table: CyTable): CyNode? {
        val nodes = getCyNodesWithValue(network, table, "identifier uri", uri)
        if (nodes.size == 1) {
            return nodes.iterator().next()
        } else {
            // TODO: Maybe throw an exception if the count is > 1?
            return null
        }
    }

    fun getCyNodesWithValue(network: CyNetwork, nodeTable: CyTable, columnName: String, value: Any): Set<CyNode> {
        val nodes = HashSet<CyNode>()
        val matchingRows = nodeTable.getMatchingRows(columnName, value)

        val primaryKeyColumnName = nodeTable.primaryKey.name
        for (row in matchingRows) {
            val nodeId = row.get(primaryKeyColumnName, Long::class.java) ?: continue
            val node = network.getNode(nodeId) ?: continue
            nodes.add(node)
        }
        return nodes
    }

}