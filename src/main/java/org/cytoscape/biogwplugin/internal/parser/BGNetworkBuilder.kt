package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable

fun CyNode.setUri(name: String, network: CyNetwork) {
    val table = network.defaultNodeTable
    if (table.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) table.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
    table.getRow(this.suid).set(Constants.BG_FIELD_IDENTIFIER_URI, name)
}

fun CyNode.getUri(network: CyNetwork): String {
    return network.defaultNodeTable.getRow(this.suid).get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java)
}

fun CyNode.setName(name: String, network: CyNetwork) {
    val table = network.defaultNodeTable
    table.getRow(this.suid).set(Constants.BG_FIELD_NAME, name)
}

fun CyNode.getName(network: CyNetwork): String {
    return network.defaultNodeTable.getRow(this.suid).get(Constants.BG_FIELD_NAME, String::class.java)
}

fun CyNode.setDescription(description: String, network: CyNetwork) {
    val table = network.defaultNodeTable
    if (table.getColumn(Constants.BG_FIELD_DESCRIPTION) == null) table.createColumn(Constants.BG_FIELD_DESCRIPTION, String::class.java, false)
    table.getRow(this.suid).set(Constants.BG_FIELD_DESCRIPTION, description)
}

fun CyNode.getDescription(network: CyNetwork): String {
    return network.defaultNodeTable.getRow(this.suid).get(Constants.BG_FIELD_DESCRIPTION, String::class.java)
}

class BGNetworkBuilder(private val serviceManager: BGServiceManager) {

    fun createNetworkFromBGNodes(nodes: Collection<BGNode>): CyNetwork {
        val networkFactory = serviceManager.networkFactory
        var network = networkFactory.createNetwork()
        addBGNodesToNetwork(nodes, network)
        return network
    }

    fun addBGNodesToNetwork(nodes: Collection<BGNode>, network: CyNetwork) {
        var nodeTable = network.defaultNodeTable
        if (nodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) nodeTable.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
        for (node in nodes) {
            addNodeToNetwork(node, network, nodeTable)
        }
    }

    fun addRelationsToNetwork(network: CyNetwork, relations: Collection<BGRelation>) {
        val nodeTable = network.defaultNodeTable
        val edgeTable = network.defaultEdgeTable

        if (nodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) nodeTable.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
        if (edgeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) edgeTable.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
        if (edgeTable.getColumn(Constants.BG_FIELD_EDGE_ID) == null) edgeTable.createColumn(Constants.BG_FIELD_EDGE_ID, String::class.java, false)



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

            val matchingRows = edgeTable.getMatchingRows(Constants.BG_FIELD_EDGE_ID, relation.edgeIdentifier)
            if (matchingRows.size == 0) {
                val edge = addEdgeToNetwork(fromNode, toNode, network, edgeTable, relation.relationType, relation.edgeIdentifier)
            } else if (matchingRows.size > 1) {
                println("WARNING: Duplicate edges!")
            }
        }
    }


    fun addEdgeToNetwork(from: CyNode, to: CyNode, network: CyNetwork, edgeTable: CyTable, relationType: BGRelationType, edgeId: String): CyEdge {
        val edge = network.addEdge(from, to, true)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_IDENTIFIER_URI, relationType.uri)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_NAME, relationType.description)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_EDGE_ID, edgeId)
        return edge
    }

    fun addNodeToNetwork(node: BGNode, network: CyNetwork, table: CyTable): CyNode {
        val cyNode = network.addNode()
        cyNode.setUri(node.uri, network)
        node.name?.let { cyNode.setName(it, network) }
        node.description?.let { cyNode.setDescription(it, network)}
        // TODO: WARNING: Unknown behaviour if the CyNodes CyNetwork is deleted!
        node.cyNodes.add(cyNode)
        return cyNode
    }

    fun getNodeWithUri(uri: String, network: CyNetwork, table: CyTable): CyNode? {
        val nodes = getCyNodesWithValue(network, table, Constants.BG_FIELD_IDENTIFIER_URI, uri)
        if (nodes.size == 1) {
            return nodes.iterator().next()
        } else {
            // TODO: Maybe throw an exception if the count is > 1?
            return null
        }
    }

    fun getEdgeWithEdgeId(edgeId: String, network: CyNetwork, table: CyTable): CyEdge? {
        val edges = getCyEdgesWithValue(network, table, Constants.BG_FIELD_EDGE_ID, edgeId)
        if (edges.size == 1) {
            return edges.iterator().next()
        } else {
            return null
        }
    }

    fun getCyNodesWithValue(network: CyNetwork, nodeTable: CyTable, columnName: String, value: Any): Set<CyNode> {
        val nodes = HashSet<CyNode>()
        val matchingRows = nodeTable.getMatchingRows(columnName, value)

        val primaryKeyColumnName = nodeTable.primaryKey.name
        for (row in matchingRows) {
            //val nodeId = row.get(primaryKeyColumnName, Long::class.java) ?: continue
            val nodeId = row.getRaw(primaryKeyColumnName) as? Long
            if (nodeId is Long) {
                val node = network.getNode(nodeId) ?: continue
                nodes.add(node)
            }
        }
        return nodes
    }

    fun getCyEdgesWithValue(network: CyNetwork, edgeTable: CyTable, columnName: String, value: Any): Set<CyEdge> {
        val edges = HashSet<CyEdge>()

        //val matchingRows = nodeTable.getMatchingRows(columnName, value)
        val matchingRows = edgeTable.getMatchingRows(columnName, value)

        val primaryKeyColumnName = edgeTable.primaryKey.name
        for (row in matchingRows) {
            //val nodeId = row.get(primaryKeyColumnName, Long::class.java) ?: continue
            val edgeId = row.getRaw(primaryKeyColumnName) as? Long
            if (edgeId is Long) {
                val node = network.getEdge(edgeId) ?: continue
                edges.add(node)
            }
        }
        return edges
    }

    fun destroyAndRecreateNetworkView(network: CyNetwork, serviceManager: BGServiceManager) {
        // Destroy all views.
        for (view in serviceManager.viewManager.getNetworkViews(network)) {
            serviceManager.viewManager.destroyNetworkView(view)
        }
        val createNetworkViewTaskFactory = serviceManager.createNetworkViewTaskFactory
        val taskIterator = createNetworkViewTaskFactory.createTaskIterator(setOf(network))
        serviceManager.taskManager.execute(taskIterator)
    }

}