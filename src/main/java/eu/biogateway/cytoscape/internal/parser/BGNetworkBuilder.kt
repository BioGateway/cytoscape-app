package eu.biogateway.cytoscape.internal.parser

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Utility
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.view.presentation.property.BasicVisualLexicon
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D
import eu.biogateway.cytoscape.internal.model.*
import eu.biogateway.cytoscape.internal.query.*
import org.cytoscape.model.*
import java.lang.Math.*
import kotlin.system.measureTimeMillis

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

fun CyNode.setParentEdgeId(edgeId: String, network: CyNetwork) {
    network.defaultNodeTable.getRow(this.suid).set(Constants.BG_FIELD_NODE_PARENT_EDGE_ID, edgeId)
}
fun CyNode.getParentEdgeId(network: CyNetwork): String {
    return network.defaultNodeTable.getRow(this.suid).get(Constants.BG_FIELD_NODE_PARENT_EDGE_ID, String::class.java)
}

fun CyEdge.getId(network: CyNetwork): String {
    return network.defaultEdgeTable.getRow(this.suid).get(Constants.BG_FIELD_EDGE_ID, String::class.java)
}
fun CyEdge.getUri(network: CyNetwork): String {
    return network.defaultEdgeTable.getRow(this.suid).get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java)
}
fun CyEdge.getSourceGraph(network: CyNetwork): String {
    return network.defaultEdgeTable.getRow(this.suid).get(Constants.BG_FIELD_SOURCE_GRAPH, String::class.java)
}

fun CyEdge.setDoubleForColumnName(value: Double, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyEdge.setStringForColumnName(value: String, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyEdge.setStringListForColumnName(value: List<String>, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyEdge.setDoubleListForColumnName(value: List<Double>, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyEdge.setValueForColumnName(value: Any, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}


fun CyNode.setDoubleForColumnName(value: Double, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyNode.setStringForColumnName(value: String, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyNode.setStringListForColumnName(value: List<String>, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyNode.setIntForColumnName(value: Int, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyNode.setBooleanForColumnName(value: Boolean, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}
fun CyNode.setValueForColumnName(value: Any, columnName: String, table: CyTable) {
    table.getRow(this.suid).set(columnName, value)
}


class BGNetworkBuilder() {


    // TODO: Use this to hide some of the node metadata. Also add fields for the extra data.
    fun createNodeMetadataTable() {
        val table = BGServiceManager.tableFactory?.createTable(Constants.BG_TABLE_NODE_METADATA, "suid", Long::class.java, false, true) ?: throw Exception("Unable to create metadata CyTable!")
        table.createColumn(Constants.BG_FIELD_NODE_PARENT_EDGE_ID, String::class.java, true)
    }

    fun expandEdgeWithRelations(netView: CyNetworkView, initialEdgeView: View<CyEdge>, nodes: Collection<BGNode>, relations: Collection<BGRelation>) {

        val expandedNodeDistanceFactor = 1.2

        val network = netView.model
        checkForMissingColumns(network.defaultEdgeTable, network.defaultNodeTable)

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
            val offsetVector = unitVector.getPerpendicular().normalize().scalarMultiply(offset)
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

        // 3. Add the new nodes
        val addedNodes = ArrayList<CyNode>()
        for ((index, node) in nodes.withIndex()) {

            //node.collapsableToEdgeID = initialEdgeView.model.getUri(network)
            val cyNode = addNodeToNetwork(node, network, network.defaultNodeTable)
            cyNode.setParentEdgeId(initialEdgeView.model.getId(network), network)
            addedNodes.add(cyNode)
        }

        // 4. Add new edges

        addRelationsToNetwork(network, relations)


        // 5. Delete the original edge

        network.removeEdges(arrayListOf(initialEdgeView.model))

        //initialEdgeView.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, false)
        //netView.model.removeEdges(arrayListOf(initialEdgeView.model))

        //netView.updateView()

        // 6. Set the positions of the new nodes
        //netView.updateView()
        //serviceManager.eventHelper.flushPayloadEvents()

        val addedNodeViews = HashSet<View<CyNode>>()
        for ((index, node) in addedNodes.withIndex()) {
            val view = netView.getNodeView(node)
            val coordinate = coordinates[index]
            view.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, coordinate.x)
            view.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, coordinate.y)
            addedNodeViews.add(view)
        }

        //netView.getEdgeView(model).setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, false)
        netView.updateView()
        Utility.reloadCurrentVisualStyleCurrentNetworkView()

    }

    fun createAggregatedEdgeForRelationNode(netView: CyNetworkView, nodeView: View<CyNode>) {
        val network = netView.model
        val nodeUri = nodeView.model.getUri(network)
        val cyNode = nodeView.model

        val parentEdgeId = cyNode.getParentEdgeId(network)
        val cyNodes = if (parentEdgeId.isNotBlank()) getCyNodesWithValue(network, network.defaultNodeTable, Constants.BG_FIELD_NODE_PARENT_EDGE_ID, parentEdgeId) else null

        val fromUri = if (parentEdgeId.isNotBlank()) parentEdgeId.split(";")[0]
        else {
            null
        }
        val relationIdentifier = if (parentEdgeId.isNotBlank()) parentEdgeId.split(";")[1]
        else {
            ""
        }
        val toUri = if (parentEdgeId.isNotBlank()) parentEdgeId.split(";")[2]
        else {
            null
        }
        if (fromUri == null) throw Exception("Invalid fromUri!")
        if (toUri == null) throw Exception("Invalid toUri!")

        val startTime = System.currentTimeMillis()
        val adjacentNodeUris = network.getAdjacentEdgeIterable(cyNode, CyEdge.Type.ANY).fold(HashSet<CyNode>()) { acc, cyEdge -> acc.union(setOf(cyEdge.source, cyEdge.target)).toHashSet() }.map { it.getUri(network) }
        val adjacentNodeExecutionTime = System.currentTimeMillis() - startTime

        // Get the associated BGNode
        val node = BGServiceManager.dataModelController.searchForExistingNode(nodeUri)
                ?: throw Exception("Node not found!")

        val query: BGRelationQuery = when (node.type.typeClass) {
            BGNodeTypeNew.BGNodeTypeClass.UNDIRECTED_STATEMENT -> BGFetchAggregatedUndirectedRelationForNodeQuery(node, relationIdentifier, fromUri, toUri)
            else -> {
                BGFetchAggregatedRelationForNodeQuery(node, relationIdentifier)
            }
        }

        network.removeEdges(network.getAdjacentEdgeIterable(cyNode, CyEdge.Type.ANY).toHashSet())



        query.addCompletion {
            val data = it as BGReturnRelationsData
            val filteredRelations = data.relationsData.filter { adjacentNodeUris.contains(it.fromNodeUri) }.filter { adjacentNodeUris.contains(it.toNodeUri) }

            addRelationsToNetwork(network, filteredRelations, true)

            if (cyNodes != null) {
                network.removeNodes(cyNodes)
            } else {
                network.removeNodes(arrayListOf(cyNode))
            }
        }
        val queryExecutionTime = measureTimeMillis {
            query.run()
        }

        if (Constants.PROFILING) print("Adjacent node execution: $adjacentNodeExecutionTime ms. Query execution: $queryExecutionTime ms.")

    }


    fun collapseEdgeWithNodes(netView: CyNetworkView, nodeView: View<CyNode>, relationTypeUri: String) {


        val network = netView.model
        val edgeTable = network.defaultEdgeTable
        val nodeUri = nodeView.model.getUri(network)

        checkForMissingColumns(network.defaultEdgeTable, network.defaultNodeTable)

        val node = BGServiceManager.dataModelController.searchForExistingNode(nodeUri) ?: return

        createAggregatedEdgeForRelationNode(netView, nodeView)

        // Fetch the relation associated with this node:

        /*
        val edgeID = nodeView.model.getParentEdgeId(network)
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
        */

        /*

        // Find the other nodes representing this edge.

        val fromNode = edge.source
        val toNode = edge.target

        val nodes = findNodesWithEdgesToNodes(network, fromNode, toNode, relationTypeUri)

        // Show the hidden edge.
        edgeView.setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true)

        network.removeNodes(nodes)
        */



        Utility.reloadCurrentVisualStyleCurrentNetworkView()


    }

    /// Finds nodes with relations of the given dataType to both nodes provided.
    private fun findNodesWithEdgesToNodes(network: CyNetwork, firstNode: CyNode, secondNode: CyNode, relationTypeUri: String): Set<CyNode> {

        //if (secondNode.networkPointer != network) throw Exception("Cannot find edges between nodes in different networks!")
        //if (firstNode == secondNode) throw Exception("This method is not ment to find self-pointing edges!")

        val nodesPointingToFirst = HashSet<CyNode>()
        val nodesPointingToSecond = HashSet<CyNode>()

        for (edge in network.edgeList) {
            val uri = edge.getUri(network)
            if (uri == relationTypeUri) {
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
        val network = BGServiceManager.networkFactory?.createNetwork() ?: throw Exception("Unable to create network!")
        val nodeTable = network.defaultNodeTable
        val edgeTable = network.defaultEdgeTable

        // Create the columns needed:

        checkForMissingColumns(edgeTable, nodeTable)



        return network
    }


    private fun checkForMissingColumns(edgeTable: CyTable?, nodeTable: CyTable?) {

        BGNetworkTableHelper.checkForMissingColumns(edgeTable, nodeTable)

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

    fun updateMetadataForCyNodes(metadata: HashMap<CyNode, HashSet<BGNodeMetadata>>, table: CyTable) {
        for (entry in metadata) {
            updateMetadataForNode(entry.value, entry.key, table)
        }
    }

    fun updateMetadataForCyEdges(metadata: HashMap<CyEdge, HashMap<String, BGRelationMetadata>>, table: CyTable) {
        for (entry in metadata) {
            updateMetadataForEdge(entry.value, entry.key, table)
        }
    }

    fun addBGNodesToNetworkWithMetadata(nodes: Collection<BGNode>, metadata: Map<BGNode, Set<BGNodeMetadata>>, network: CyNetwork) {

        val nodeTable = network.defaultNodeTable
        checkForMissingColumns(null, nodeTable)

        for (node in nodes) {
            val cyNode = addNodeToNetwork(node, network, nodeTable)
            metadata[node]?.let {
                updateMetadataForNode(it, cyNode, nodeTable)
            }
        }
    }



    fun addRelationsToNetwork(network: CyNetwork, relations: Collection<BGRelation>, reloadMetadata: Boolean = false) {
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
                    BGServiceManager.dataModelController.loadDataForNode(bgNode)
                }
                node = addNodeToNetwork(bgNode, network, nodeTable)
            }
            cyNodes.put(uri, node)
        }

        // Now we have all the CyNodes needed in the network to create the relations. Edges can be created.

        val relationEdges = HashMap<BGPrimitiveRelation, CyEdge>()

        for (relation in relations) {
            val fromNode = cyNodes.get(relation.fromNode.uri) ?: throw Exception("CyNode not found!")
            val toNode = cyNodes.get(relation.toNode.uri) ?: throw Exception("CyNode not found!")
            if (!checkForExistingEdges(edgeTable, relation)) {
//                if (relation.relationType.identifier == "intact:http://purl.obolibrary.org/obo/RO_0002436") {
//                    serviceManager.dataModelController.getConfidenceScoreForRelation(relation)?.let {
//                        println(it)
//                        relation.metadata.confidence = it
//                    }
//                }
                val edge = addEdgeToNetwork(fromNode, toNode, network, edgeTable, relation.relationType, relation.edgeIdentifier, relation.metadata, relation.sourceGraph)
                relationEdges[relation] = edge
            } else {
                println("WARNING: Duplicate edges!")
            }
        }

        if (reloadMetadata) {
            reloadMetadataForEdges(relationEdges, network)
        }
    }

    private fun checkForExistingEdges(edgeTable: CyTable, edgeId: String): Boolean {
        val matchingRows = edgeTable.getMatchingRows(Constants.BG_FIELD_EDGE_ID, edgeId)
        if (matchingRows.isNotEmpty()) {
            return true
        }
        return false
    }

    private fun checkForExistingEdges(edgeTable: CyTable, relation: BGRelation): Boolean {
        // If undirected, first check for edges the other way.
        var reverseExists = false
        if (!relation.relationType.directed) {
            reverseExists = checkForExistingEdges(edgeTable, relation.reverseEdgeIdentifier)
        }
        return checkForExistingEdges(edgeTable, relation.edgeIdentifier) || reverseExists
    }

    /*
    @Deprecated("Use the one below!")
    fun addEdgeToNetwork(network: CyNetwork, edgeId: String) {
        val components = edgeId.split(";")
        if (components.size != 3) throw Exception("Invalid number of EdgeId components!")
        val fromUri = components[0]
        val relationComponents = components[1].split("::")
        val graph = if (relationComponents.size == 2) relationComponents[1] else null
        val relationUri = relationComponents[1]
        val toUri = components[2]

        // TODO: Extract the metadata from the nodes and add it to the edge.

        val relationType = when (graph != null) {
            true -> {
                serviceManager.config.getRelationTypeForURIandGraph(relationUri, graph!!)
                        ?: BGRelationType(relationUri, relationUri, 0)
            }
            false -> {
                serviceManager.config.getRelationTypesForURI(relationUri)?.first()
                        ?: BGRelationType(relationUri, relationUri, 0)
            }
        }
        val fromNode = getNodeWithUri(fromUri, network, network.defaultNodeTable) ?: throw Exception("CyNode not found!")
        val toNode = getNodeWithUri(toUri, network, network.defaultNodeTable) ?: throw Exception("CyNode not found!")
        if (!checkForExistingEdges(network.defaultEdgeTable, edgeId)) {
            val edge = addEdgeToNetwork(fromNode, toNode, network, network.defaultEdgeTable, relationType, edgeId, graph)
        } else {
            println("WARNING: Duplicate edges!")
        }
    }*/

    private fun addEdgeToNetwork(from: CyNode, to: CyNode, network: CyNetwork, edgeTable: CyTable, relationType: BGRelationType, edgeId: String, metadata: Map<String, BGRelationMetadata>, sourceGraph: String?): CyEdge {

        val edge = network.addEdge(from, to, relationType.directed)
        checkForMissingColumns(edgeTable, null)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_IDENTIFIER_URI, relationType.uri)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_INTERACTION, relationType.identifier)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_NAME, relationType.name)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_EDGE_ID, edgeId)
        edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_EDGE_EXPANDABLE, if (relationType.expandable) "true" else "false")
        if (sourceGraph != null) {
            edgeTable.getRow(edge.suid).set(Constants.BG_FIELD_SOURCE_GRAPH, sourceGraph)
        }

        updateMetadataForEdge(metadata, edge, edgeTable)

        return edge
    }

    private fun updateMetadataForNode(metadata: Set<BGNodeMetadata>, node: CyNode, nodeTable: CyTable) {
        for (metaData in metadata) {
            if (BGNetworkTableHelper.assureThatColumnExists(nodeTable,
                            metaData.columnName,
                            metaData.dataType,
                            false)) {

                when (metaData.dataType) {
                    BGTableDataType.STRING -> (metaData.getValue() as String?)?.let {
                        node.setValueForColumnName(it, metaData.columnName, nodeTable)
                    }
                    BGTableDataType.DOUBLE -> (metaData.getValue() as Double?)?.let {
                    node.setValueForColumnName(it, metaData.columnName, nodeTable)
                }
                    BGTableDataType.INT -> (metaData.getValue() as Int?)?.let {
                    node.setValueForColumnName(it, metaData.columnName, nodeTable)
                }
                    BGTableDataType.BOOLEAN -> (metaData.getValue() as Boolean?)?.let {
                    node.setValueForColumnName(it, metaData.columnName, nodeTable)
                }
                    BGTableDataType.STRINGARRAY -> (metaData.getValue() as List<String>)?.let {
                    node.setValueForColumnName(it, metaData.columnName, nodeTable)
                }
                    BGTableDataType.INTARRAY -> (metaData.getValue() as List<Int>)?.let {
                    node.setValueForColumnName(it, metaData.columnName, nodeTable)
                }
                    BGTableDataType.DOUBLEARRAY -> (metaData.getValue() as List<Double>)?.let {
                    node.setValueForColumnName(it, metaData.columnName, nodeTable)
                }
                    BGTableDataType.UNSUPPORTED -> throw Exception("Unsupported Data type.")

                }
            }
        }
    }

    private fun updateMetadataForEdge(metadata: Map<String, BGRelationMetadata>, edge: CyEdge, edgeTable: CyTable) {
        for ((columnName, metaData) in metadata.iterator()) {
            if (BGNetworkTableHelper.assureThatColumnExists(edgeTable,
                            columnName,
                            metaData.dataType,
                            false)) {

                when (metaData.dataType) {
                    BGTableDataType.DOUBLE -> {
                        metaData.numericValue?.let {
                            edge.setDoubleForColumnName(it, columnName, edgeTable)
                        }
                    }
                    BGTableDataType.STRING -> {
                        metaData.stringValue?.let {
                            // TODO: Remove this when the server stops using dummy URIs
                            // TODO: Get the label name instead.
                            //edge.setStringForColumnName(it.replace("http://www.semantic-systems-biology.org/ssb/", ""), columnName, edgeTable)
                            edge.setStringForColumnName(it, columnName, edgeTable)
                        }
                    }
                    BGTableDataType.INT -> (metaData.value as? Int)?.let {
                        edge.setValueForColumnName(it, columnName, edgeTable)
                    }
                    BGTableDataType.BOOLEAN -> (metaData.value as? Boolean)?.let {
                        edge.setValueForColumnName(it, columnName, edgeTable)
                    }
                    BGTableDataType.STRINGARRAY -> (metaData.value as? List<String>)?.let {
                        edge.setValueForColumnName(it, columnName, edgeTable)
                    }
                    BGTableDataType.INTARRAY -> (metaData.value as? List<Int>)?.let {
                        edge.setValueForColumnName(it, columnName, edgeTable)
                    }
                    BGTableDataType.DOUBLEARRAY -> {
                        (metaData.value as? List<Double>)?.let {
                            edge.setValueForColumnName(it, columnName, edgeTable)
                        }
                    }
                    BGTableDataType.UNSUPPORTED -> throw Exception("Unsupported Data type.")
                }
            }
        }
    }

    fun updateEdgeTableMetadataForCyEdges(network: CyNetwork, relations: Map<BGPrimitiveRelation, CyEdge>) {
        for ((relation, edge) in relations) {
            updateMetadataForEdge(relation.metadata, edge, network.defaultEdgeTable)
        }
    }

    fun reloadMetadataForEdges(relationEdges: Map<BGPrimitiveRelation, CyEdge>, network: CyNetwork) {
        // Get the active metadata types.
        val activeMetadataTypes = BGServiceManager.config.activeEdgeMetadataTypes

        val query = BGLoadRelationMetadataQuery(relationEdges.keys, activeMetadataTypes) {
            BGServiceManager.dataModelController.networkBuilder.updateEdgeTableMetadataForCyEdges(network, relationEdges)
        }
        BGServiceManager.execute(query)
    }

    fun reloadMetadataForRelationsInCurrentNetwork() {
        // Get the active metadata types.
        val activeMetadataTypes = BGServiceManager.config.activeEdgeMetadataTypes
        // Get a list of column names for the active metadata types.
        val activeColumnNames = activeMetadataTypes.map { it.name }

        // Get the CyEdges of the current network.
        val network = BGServiceManager.applicationManager?.currentNetwork ?: return
        val edgeTable = network.defaultEdgeTable

        // Attempt to recreate them as BGRelations in a Map<CyEdge, BGRelation>
        val relations = HashMap<BGPrimitiveRelation, CyEdge>()
        for (edge in network.edgeList) {
            val relationType = BGServiceManager.config.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network)) ?: continue

            // TODO: Check that these values exist! The URI table might not even be present!
            val fromUri = edge.source.getUri(network)
            val toUri = edge.target.getUri(network)
            val sourceGraph = edge.getSourceGraph(network)

            val relation = BGPrimitiveRelation(fromUri, relationType, toUri)
            relation.sourceGraph = sourceGraph
            relations[relation] = edge
        }

        val unloadedRelations = HashMap<BGPrimitiveRelation, CyEdge>()

        // Iterate through each metadata dataType.
        for (metadataType in activeMetadataTypes) {
            // Filter out the relations of the wrong relation dataType.
            val relevantRelations = relations.filter { metadataType.supportedRelations.contains(it.key.relationType) }
                    .filter {
                        // Filter out the CyEdges that have the data present.
                        val value = BGNetworkTableHelper.getValueForEdgeColumnName(it.value, metadataType.name, network, Any::class.java)
                        value == null
                    }
            // Add the remaining relations to a set.
            unloadedRelations.putAll(relevantRelations)
        }

        if (unloadedRelations.size == 0) return
        val query = BGLoadRelationMetadataQuery(unloadedRelations.keys, activeMetadataTypes) {
            BGServiceManager.dataModelController.networkBuilder.updateEdgeTableMetadataForCyEdges(network, unloadedRelations)
        }
        BGServiceManager.execute(query)
    }

    fun reloadMetadataForNodesInCurrentNetwork() {
        val network = BGServiceManager.applicationManager?.currentNetwork ?: return
        reloadMetadataForNodesInNetwork(network)
    }


    fun reloadMetadataForNodesInNetwork(network: CyNetwork) {
        // Get the active metadata types.
        val activeMetadataTypes = BGServiceManager.config.activeNodeMetadataTypes
        //val activeMetadataTypes = BGServiceManager.config.nodeMetadataTypes.values.toHashSet()
        // Get a list of column names for the active metadata types.

        // Get the CyEdges of the current network.
        val nodeTable = network.defaultNodeTable

        val nodeUrisToCyNodes = network.nodeList.associateBy( { it.getUri(network) }, {it})

        val unloadedNodes = HashMap<String, CyNode>()



        // Iterate through each metadata dataType.
        for (metadataType in activeMetadataTypes) {
            // Filter out the relations of the wrong relation dataType.
            val relevantNodes = nodeUrisToCyNodes.filter { BGNetworkTableHelper.getValueForNodeColumnName(it.value, metadataType.id, network, Any::class.java) == null }
            unloadedNodes.putAll(relevantNodes)
        }

        if (unloadedNodes.size == 0) return

        val query = BGLoadNodeMetadataQuery(unloadedNodes.keys, activeMetadataTypes) {
            // TODO: Load Data to network.
            val result = it
            print(result)
            for ((uri, metadataSet) in it) {
                val cyNode = nodeUrisToCyNodes[uri] ?: continue
                updateMetadataForNode(metadataSet, cyNode, nodeTable)
            }

        }
        BGServiceManager.execute(query)
    }

    private fun addNodeToNetwork(node: BGNode, network: CyNetwork, table: CyTable): CyNode {
        val cyNode = network.addNode()
        cyNode.setUri(node.uri, network)
        val name = node.name
        cyNode.setName(name, network)

        cyNode.setType(node.type.name, network)

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

    fun createNetworkView(network: CyNetwork) {
        if (BGServiceManager.dataModelController.settings.useBioGatewayLayoutStyleAsDefault) {
            val view = BGServiceManager.adapter?.cyNetworkViewFactory?.createNetworkView(network)

            val visualStyle = Utility.getOrCreateBioGatewayVisualStyle()
            BGServiceManager.adapter?.visualMappingManager?.setVisualStyle(visualStyle, view)
            BGServiceManager.viewManager?.addNetworkView(view)

            val layoutManager = BGServiceManager.adapter?.cyLayoutAlgorithmManager
            val defaultLayout = layoutManager?.defaultLayout

            val taskIterator = defaultLayout?.createTaskIterator(view, defaultLayout.defaultLayoutContext, view?.nodeViews?.toHashSet(), null)
            BGServiceManager.taskManager?.execute(taskIterator)

        } else {
            val createNetworkViewTaskFactory = BGServiceManager.createNetworkViewTaskFactory
            val taskIterator = createNetworkViewTaskFactory?.createTaskIterator(setOf(network))
            BGServiceManager.taskManager?.execute(taskIterator)
        }
    }
}