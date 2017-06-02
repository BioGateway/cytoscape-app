package org.cytoscape.biogwplugin.internal.old.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.old.query.BGNode
import org.cytoscape.biogwplugin.internal.old.query.BGRelation
import org.cytoscape.model.*
import org.cytoscape.task.create.CreateNetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator

import java.util.*

/**
 * Created by sholmas on 24/03/2017.
 */
object BGNetworkBuilder {

    fun createNetworkFromBGNodes(nodes: ArrayList<BGNode>, serviceManager: BGServiceManager): CyNetwork {
        val network = serviceManager.networkFactory.createNetwork()
        val nodeTable = network.defaultNodeTable
        nodeTable.createColumn("identifier uri", String::class.java, false)


        for (node in nodes) {
            val newNode = network.addNode()
            nodeTable.getRow(newNode.suid).set("identifier uri", node.URI)
            nodeTable.getRow(newNode.suid).set("name", node.commonName)
        }

        return network
    }

    fun addBGNodesToNetwork(network: CyNetwork, nodes: ArrayList<BGNode>, serviceManager: BGServiceManager) {
        for (node in nodes) {
            val matchingNodes = getNodesWithValue(network, network.defaultNodeTable, "identifier uri", node.URI)
            if (matchingNodes.isEmpty()) {
                val newNode = network.addNode()
                network.defaultNodeTable.getRow(newNode.suid).set("identifier uri", node.URI)
                network.defaultNodeTable.getRow(newNode.suid).set("name", node.commonName)
            }
        }
    }

    fun addBGRelationsToNetwork(network: CyNetwork, relations: ArrayList<BGRelation>, serviceManager: BGServiceManager) {
        val toNodes = HashSet<BGNode>()
        val fromNodes = HashSet<BGNode>()
        val nodeTable = network.defaultNodeTable
        val edgeTable = network.defaultEdgeTable
        if (edgeTable.getColumn("identifier uri") == null) edgeTable.createColumn("identifier uri", String::class.java, false)

        // Fetch all nodes involved in the relations:
        for (relation in relations) {
            fromNodes.add(relation.fromNode)
            toNodes.add(relation.toNode)
        }

        // TODO: Add the missing nodes to the network. For now we assume it's being done elsewhere.

        for (relation in relations) {
            val fromNode = getNodeForUri(relation.fromNode.URI, network, nodeTable)
            val toNode = getNodeForUri(relation.toNode.URI, network, nodeTable)

            // TODO: Create unique edge identifiers to assure that duplicate edges are not added. See old KT-App code for details.
            val edge = network.addEdge(fromNode, toNode, true)
            edgeTable.getRow(edge.suid).set("identifier uri", relation.URI)
            edgeTable.getRow(edge.suid).set<String>("name", serviceManager.cache.getNameForRelationType(relation.URI))
        }
    }

    private fun getNodeForUri(nodeUri: String, network: CyNetwork, table: CyTable): CyNode? {

        val nodes = getNodesWithValue(network, table, "identifier uri", nodeUri)
        // Node uri should be unique, so there should be no more than one match.
        if (nodes.size == 1) {
            return nodes.iterator().next() // Just return the first one, it's just one anyway.
        }
        return null
    }

    // TODO: A similar method is already defined in the BGCache class. Consolidate them?
    private fun getNodesWithValue(net: CyNetwork, table: CyTable, colname: String, value: Any): Set<CyNode> {
        val matchingRows = table.getMatchingRows(colname, value)
        val nodes = HashSet<CyNode>()
        val primaryKeyColname = table.primaryKey.name
        for (row in matchingRows) {
            val nodeId = row.get(primaryKeyColname, Long::class.java) ?: continue
            val node = net.getNode(nodeId) ?: continue
            nodes.add(node)
        }
        return nodes
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

    fun createNetworkView(network: CyNetwork, serviceManager: BGServiceManager) {
        val viewTaskFactory = serviceManager.createNetworkViewTaskFactory
        val taskIterator = viewTaskFactory.createTaskIterator(setOf(network))
        serviceManager.taskManager.execute(taskIterator)
    }
}
