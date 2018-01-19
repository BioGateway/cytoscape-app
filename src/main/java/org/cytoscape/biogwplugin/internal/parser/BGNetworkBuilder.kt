package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadata
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.view.presentation.property.BasicVisualLexicon
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import java.lang.Math.*

fun Vector2D.getPerpendicular(): Vector2D {
    return Vector2D(this.y, -this.x)
}

fun getCoordinate(nodeView: View<CyNode>): Vector2D {
    return Vector2D(
            nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION),
            nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION))
}

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

fun CyEdge.setExpandable(expandable: Boolean, network: CyNetwork) {
    network.defaultEdgeTable.getRow(this.suid).set(Constants.BG_FIELD_EDGE_EXPANDABLE, if (expandable) "true" else "false")
}

fun CyEdge.getExpandable(network: CyNetwork): Boolean {
    return (network.defaultEdgeTable.getRow(this.suid).get(Constants.BG_FIELD_EDGE_EXPANDABLE, String::class.java) == "true")
}

fun CyEdge.getId(network: CyNetwork): String {
    return network.defaultEdgeTable.getRow(this.suid).get(Constants.BG_FIELD_EDGE_ID, String::class.java)
}
fun CyEdge.getUri(network: CyNetwork): String {
    return network.defaultEdgeTable.getRow(this.suid).get(Constants.BG_FIELD_EDGE_ID, String::class.java)
}

class BGNetworkBuilder(private val serviceManager: BGServiceManager) {

    fun expandEdgeWithRelations(netView: CyNetworkView, initialEdgeView: View<CyEdge>, nodes: Collection<BGNode>, relations: Collection<BGRelation>) {

        val expandedNodeDistanceFactor = 1.2

        val network = netView.model

        // 1. Find the node positions in the network
        val fromNode = initialEdgeView.model.source
        val toNode = initialEdgeView.model.target

        val fromNodeView = netView.getNodeView(fromNode)
        val toNodeView = netView.getNodeView(toNode)

        val fromNodeCoords = getCoordinate(fromNodeView)
        val toNodeCoords = getCoordinate(toNodeView)

        // 2. Calculate the positions of the new nodes

        // Assume that the original edge was a straight line. Calculate the normal to this line, intersecting the line in the middle between the nodes.

        // Vector from fromNode to toNode:
        val unitVector = toNodeCoords.subtract(fromNodeCoords).normalize()
        val distance = fromNodeCoords.distance(toNodeCoords)

        // We now have a normal and a distance. We can then get the point between the two nodes.
        val middlePoint = fromNodeCoords.add(unitVector.scalarMultiply(distance/2))

        fun middleWithOffset(offset: Double): Vector2D {
            // Calculates a point on the orthogonal, with a positive or negative offset.
            val offsetVector = fromNodeCoords.getPerpendicular().normalize().scalarMultiply(offset)
            return middlePoint.add(offsetVector)
        }

        val nodeSize = fromNodeView.getVisualProperty(BasicVisualLexicon.NODE_SIZE)

        fun calculateNodeCoordinates(): ArrayList<Vector2D> {
            val nodeCoordinates = ArrayList<Vector2D>()
            if (nodes.size == 1) {
                nodeCoordinates.add(middlePoint)
                return nodeCoordinates
            }
            if (nodes.size % 2 == 1) {
                for ((index, node) in nodes.withIndex()) {
                    val offset: Double = floorDiv((index + 1), 2) * nodeSize * expandedNodeDistanceFactor
                    if (index == 0) {
                        // First node of odd total nodes goes in the middle.
                        nodeCoordinates.add(middlePoint)
                    } else if (index % 2 == 0) {
                        // Odd nodes goes below, so the offset is negative.
                        nodeCoordinates.add(middleWithOffset(-offset))
                    } else {
                        // Even nodes goes above.
                        nodeCoordinates.add(middleWithOffset(offset))
                    }
                }
            } else {
                // We have an even number of nodes.
                for ((index, node) in nodes.withIndex()) {
                    val offset: Double = (floorDiv(index, 2) + 0.5) * (nodeSize * expandedNodeDistanceFactor)
                    if (index % 2 == 0) {
                        // Odd nodes goes below, so the offset is negative.
                        nodeCoordinates.add(middleWithOffset(-offset))
                    } else {
                        // Even nodes goes above.
                        nodeCoordinates.add(middleWithOffset(offset))
                    }
                }
            }
            return nodeCoordinates
        }

        val coordinates = calculateNodeCoordinates()

        // 3. Hide the original edge
        initialEdgeView.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, false)

        // 4. Add the new nodes
        val addedNodes = ArrayList<CyNode>()
        for ((index, node) in nodes.withIndex()) {
            node.collapsableToEdgeID = initialEdgeView.model.getId(network)
            val cyNode = addNodeToNetwork(node, network, network.defaultNodeTable)
            addedNodes.add(cyNode)
        }

        // 5. Add new edges

        addRelationsToNetwork(network, relations)


        // 6. Set the positions of the new nodes
        netView.updateView()
        serviceManager.eventHelper.flushPayloadEvents()

        val addedNodeViews = HashSet<View<CyNode>>()
        for ((index, node) in addedNodes.withIndex()) {
            val view = netView.getNodeView(node)
            val coordinate = coordinates[index]
            view.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, coordinate.x)
            view.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, coordinate.y)
            addedNodeViews.add(view)
        }

        netView.updateView()

        Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)
    }


    fun collapseEdgeWithNodes(netView: CyNetworkView, nodeView: View<CyNode>, nodeUri: String, relationTypeUri: String) {

        val network = netView.model
        val edgeTable = network.defaultEdgeTable

        // Get the edge SUID
        val node = serviceManager.server.searchForExistingNode(nodeUri)
        val edgeID = node?.collapsableToEdgeID

        if (edgeID == null) return

        val edgeList = getCyEdgesWithValue(network, edgeTable, Constants.BG_FIELD_EDGE_ID, edgeID)

        if (edgeList.size > 1) throw Exception("Duplicate EdgeIDs in network!")
        if (edgeList.size == 0) throw Exception("Edge ID not found in network!")

        val edge = edgeList.first()
        val edgeView = netView.getEdgeView(edge)

        if (edgeView == null) {
            // TODO: Create a new CyEdge and view.
            return
        }

        // Find the other nodes representing this edge.

        val fromNode = edge.source
        val toNode = edge.target

        val nodes = findNodesWithEdgesToNodes(fromNode, toNode, relationTypeUri)

        // Show the hidden edge.
        edgeView.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true)

        network.removeNodes(nodes)

        Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)

    }

    /// Finds nodes with relations of the given type to both nodes provided.
    private fun findNodesWithEdgesToNodes(firstNode: CyNode, secondNode: CyNode, relationTypeUri: String): Set<CyNode> {

        val network = firstNode.networkPointer
        if (secondNode.networkPointer != network) throw Exception("Cannot find edges between nodes in different networks!")
        if (firstNode == secondNode) throw Exception("This method is not ment to find self-pointing edges!")

        val nodesPointingToFirst = HashSet<CyNode>()
        val nodesPointingToSecond = HashSet<CyNode>()

        for (edge in network.edgeList) {
            if (edge.getUri(network) == relationTypeUri) {
                if (edge.target == firstNode) nodesPointingToFirst.add(edge.source)
                if (edge.target == secondNode) nodesPointingToSecond.add(edge.source)
            }
        }

        // The intersection is the ones we are looking for.
        val commonNodes = nodesPointingToFirst.intersect(nodesPointingToSecond)

        return commonNodes
    }


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
        if (nodeTable?.getColumn(Constants.BG_FIELD_NODE_PARENT_EDGE_ID) == null) nodeTable?.createColumn(Constants.BG_FIELD_NODE_PARENT_EDGE_ID, String::class.java, false)

        // Edge table
        if (edgeTable?.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) edgeTable?.createColumn(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java, false)
        if (edgeTable?.getColumn(Constants.BG_FIELD_SOURCE_GRAPH) == null) edgeTable?.createColumn(Constants.BG_FIELD_SOURCE_GRAPH, String::class.java, false)
        if (edgeTable?.getColumn(Constants.BG_FIELD_EDGE_ID) == null) edgeTable?.createColumn(Constants.BG_FIELD_EDGE_ID, String::class.java, false)
        if (edgeTable?.getColumn(Constants.BG_FIELD_EDGE_EXPANDABLE) == null) edgeTable?.createColumn(Constants.BG_FIELD_EDGE_EXPANDABLE, String::class.java, false)
    }

    fun addBGNodesToNetwork(nodes: Collection<BGNode>, network: CyNetwork) {
        val nodeTable = network.defaultNodeTable

        checkForMissingColumns(null, nodeTable)

        val uniqueNodes = HashSet<BGNode>()
        uniqueNodes.addAll(nodes)

        for (bgNode in nodes) {
            var node = getNodeWithUri(bgNode.uri, network, nodeTable)
            if (node == null) {
                val cyNode = addNodeToNetwork(bgNode, network, nodeTable)
            }
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
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_EDGE_EXPANDABLE, if (relationType.expandable) "true" else "false")
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

    fun getNodeWithUri(uri: String, network: CyNetwork, table: CyTable): CyNode? {
        val nodes = getCyNodesWithValue(network, table, Constants.BG_FIELD_IDENTIFIER_URI, uri)
        if (nodes.size == 1) {
            if (nodes.size > 1) println("WARNING: Duplicate nodes!")
            return nodes.iterator().next()
        } else {
            return null
        }
    }

    fun getCyNodesWithValue(network: CyNetwork, nodeTable: CyTable, columnName: String, value: Any): Set<CyNode> {
        val nodes = HashSet<CyNode>()
        val matchingRows = nodeTable.getMatchingRows(columnName, value)

        val primaryKeyColumnName = nodeTable.primaryKey.name
        for (row in matchingRows) {
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

        val matchingRows = edgeTable.getMatchingRows(columnName, value)

        val primaryKeyColumnName = edgeTable.primaryKey.name
        for (row in matchingRows) {
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
            serviceManager.adapter.visualMappingManager.setVisualStyle(visualStyle, view)
            serviceManager.viewManager.addNetworkView(view)

            val layoutManager = serviceManager.adapter.cyLayoutAlgorithmManager
            val defaultLayout = layoutManager.defaultLayout

            val taskIterator = defaultLayout.createTaskIterator(view, defaultLayout.defaultLayoutContext, view.nodeViews.toHashSet(), null)
            serviceManager.taskManager.execute(taskIterator)

        } else {
            val createNetworkViewTaskFactory = serviceManager.createNetworkViewTaskFactory
            val taskIterator = createNetworkViewTaskFactory.createTaskIterator(setOf(network))
            serviceManager.taskManager.execute(taskIterator)
        }
    }
}