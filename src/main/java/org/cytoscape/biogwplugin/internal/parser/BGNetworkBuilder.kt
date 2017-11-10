package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadata
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.util.BGVisualStyleBuilder
import org.cytoscape.biogwplugin.internal.util.BGVisualStyleTask
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.cytoscape.view.model.View
import org.cytoscape.work.AbstractTask
import java.awt.EventQueue

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

fun CyNode.setType(type: String, network: CyNetwork) {
    val table = network.defaultNodeTable
    table.getRow(this.suid).set(Constants.BG_FIELD_NODE_TYPE, type)
}

fun CyNode.getType(network: CyNetwork): String {
    return network.defaultNodeTable.getRow(this.suid).get(Constants.BG_FIELD_NODE_TYPE, String::class.java)
}

class BGNetworkBuilder(private val serviceManager: BGServiceManager) {

    fun createNetworkFromBGNodes(nodes: Collection<BGNode>): CyNetwork {
        val network = createNetwork()
        addBGNodesToNetwork(nodes, network)
        return network
    }

    fun createNetwork(): CyNetwork {
        val network = serviceManager.networkFactory.createNetwork()
        val nodeTable = network.defaultNodeTable
        val edgeTable = network.defaultEdgeTable

        // Create the columns needed:

        checkForMissingColumns(edgeTable, nodeTable)



        return network
    }

    private fun checkForMissingColumns(edgeTable: CyTable?, nodeTable: CyTable?) {

        // Node table
        if (nodeTable?.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) nodeTable?.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
        if (nodeTable?.getColumn(Constants.BG_FIELD_NODE_TYPE) == null) nodeTable?.createColumn(Constants.BG_FIELD_NODE_TYPE, String::class.java, false)

        // Edge table
        if (edgeTable?.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) edgeTable?.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
        //if (edgeTable.getColumn(Constants.BG_FIELD_PUBMED_URI) == null) edgeTable.createColumn(Constants.BG_FIELD_PUBMED_URI, String::class.java, false)
        if (edgeTable?.getColumn(Constants.BG_FIELD_SOURCE_GRAPH) == null) edgeTable?.createColumn(Constants.BG_FIELD_SOURCE_GRAPH, String::class.java, false)
        if (edgeTable?.getColumn(Constants.BG_FIELD_EDGE_ID) == null) edgeTable?.createColumn(Constants.BG_FIELD_EDGE_ID, String::class.java, false)
    }

    fun addBGNodesToNetwork(nodes: Collection<BGNode>, network: CyNetwork) {
        val nodeTable = network.defaultNodeTable

        checkForMissingColumns(null, nodeTable)

        val uniqueNodes = HashSet<BGNode>()
        uniqueNodes.addAll(nodes)
        //val dummyEdges = ArrayList<CyEdge>()

        for (bgNode in nodes) {
            var node = getNodeWithUri(bgNode.uri, network, nodeTable)
            if (node == null) {
                val cyNode = addNodeToNetwork(bgNode, network, nodeTable)

                //val edge = network.addEdge(cyNode, cyNode, true)
                //dummyEdges.add(edge)
            }
        }
        // Let's see if adding and removing edges forces the view to sync.
        //network.removeEdges(dummyEdges)
        EventQueue.invokeLater {
            //addAndRemoveDummyNodeToNetwork(network)
            serviceManager.applicationManager.currentNetworkView.updateView()
            serviceManager.eventHelper.flushPayloadEvents()
        }
    }

    fun addRelationsToNetwork(network: CyNetwork, relations: Collection<BGRelation>) {
        val nodeTable = network.defaultNodeTable
        val edgeTable = network.defaultEdgeTable

        checkForMissingColumns(edgeTable, nodeTable)

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
                if (!bgNode.isLoaded) {
                    // This method is synchronous, so should be completed before the next line (addNodeToNetwork)
                    serviceManager.server.loadDataForNode(bgNode)
                }
                node = addNodeToNetwork(bgNode, network, nodeTable)
            }
            cyNodes.put(uri, node)
        }

        // Now we have all the CyNodes needed in the network to create the relations. Edges can be created.

        for (relation in relations) {
            val fromNode = cyNodes.get(relation.fromNode.uri) ?: throw Exception("CyNode not found!")
            val toNode = cyNodes.get(relation.toNode.uri) ?: throw Exception("CyNode not found!")
            if (!checkForExistingEdges(edgeTable, relation)) {
                val edge = addEdgeToNetwork(fromNode, toNode, network, edgeTable, relation.relationType, relation.edgeIdentifier, relation.metadata)
            } else {
                println("WARNING: Duplicate edges!")
            }
        }
    }

    private fun checkForExistingEdges(edgeTable: CyTable, relation: BGRelation): Boolean {
        // If undirected, first check for edges the other way.
        if (!relation.relationType.directed) {
            val matchingRows = edgeTable.getMatchingRows(Constants.BG_FIELD_EDGE_ID, relation.reverseEdgeIdentifier)
            if (matchingRows.size != 0) {
                return true
            }
        }
        // Check for any edges with the same nodes, type and direction.
        val matchingRows = edgeTable.getMatchingRows(Constants.BG_FIELD_EDGE_ID, relation.edgeIdentifier)
        if (matchingRows.size != 0) {
            return true
        }
        // No edges found.
        return false
    }


    fun addEdgeToNetwork(from: CyNode, to: CyNode, network: CyNetwork, edgeTable: CyTable, relationType: BGRelationType, edgeId: String, metadata: BGRelationMetadata): CyEdge {

        val edge = network.addEdge(from, to, relationType.directed)
        checkForMissingColumns(edgeTable, null)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_IDENTIFIER_URI, relationType.uri)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_NAME, relationType.name)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_EDGE_ID, edgeId)
        if (metadata.sourceGraph != null) {
            edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_SOURCE_GRAPH, metadata.sourceGraph)
        }
        return edge
    }

    private fun addNodeToNetwork(node: BGNode, network: CyNetwork, table: CyTable): CyNode {
        val cyNode = network.addNode()
        cyNode.setUri(node.uri, network)
        val name = node.name
        if (name != null) {
            cyNode.setName(name, network)
        } else {
            cyNode.setName(node.generateName(), network)
        }

        cyNode.setType(node.type.paremeterType, network)

        node.description?.let { cyNode.setDescription(it, network)}
        // TODO: WARNING: Unknown behaviour if the CyNodes CyNetwork is deleted!
        node.cyNodes.add(cyNode)
        return cyNode
    }

    fun addAndRemoveDummyNodeToNetwork(network: CyNetwork) {
        EventQueue.invokeLater {
            val cyNode = network.addNode()
            EventQueue.invokeLater {
                network.removeNodes(listOf(cyNode))
            }
        }
    }

    fun getNodeWithUri(uri: String, network: CyNetwork, table: CyTable): CyNode? {
        val nodes = getCyNodesWithValue(network, table, Constants.BG_FIELD_IDENTIFIER_URI, uri)
        if (nodes.size == 1) {
            if (nodes.size > 1) println("WARNING: Duplicate nodes!")
            return nodes.iterator().next()
        } else {
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

    fun createNetworkView(network: CyNetwork, serviceManager: BGServiceManager) {

        if (serviceManager.server.settings.useBioGatewayLayoutStyleAsDefault) {
            val view = serviceManager.adapter.cyNetworkViewFactory.createNetworkView(network)

            val visualStyle = Utility.getOrCreateBioGatewayVisualStyle(serviceManager)

//            serviceManager.adapter.visualMappingManager.addVisualStyle(visualStyle)
            visualStyle.apply(view)
            serviceManager.viewManager.addNetworkView(view)

            val layoutManager = serviceManager.adapter.cyLayoutAlgorithmManager
            var defaultLayout = layoutManager.defaultLayout

            val taskIterator = defaultLayout.createTaskIterator(view, defaultLayout.defaultLayoutContext, view.nodeViews.toHashSet(), null)
            serviceManager.taskManager.execute(taskIterator)


        } else {
            val createNetworkViewTaskFactory = serviceManager.createNetworkViewTaskFactory
            val taskIterator = createNetworkViewTaskFactory.createTaskIterator(setOf(network))

            serviceManager.taskManager.execute(taskIterator)
        }
    }
}