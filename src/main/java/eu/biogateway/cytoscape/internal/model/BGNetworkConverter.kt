package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGNetworkTableHelper
import eu.biogateway.cytoscape.internal.parser.getUri
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import javax.swing.JOptionPane

class BGNetworkConverter(val serviceManager: BGServiceManager) {

    fun importNetwork(sourceNetwork: CyNetwork, identifierConversions: Collection<BGIdentifierConversion>, nodeConversions: Collection<BGConversion>, edgeConversions: Collection<BGConversion>) {

        val networkBuilder = serviceManager.dataModelController.networkBuilder
        val destinationNetwork = networkBuilder.createNetwork()
        serviceManager.networkManager?.addNetwork(destinationNetwork)

        // This maps the SUID of the source CyNode to the new BGNodes.
        val newNodes = HashMap<Long, BGNode>()

        for (identifierConversion in identifierConversions) {
            val unloadedNodes = sourceNetwork.nodeList.filter { !newNodes.keys.contains(it.suid) }
            val foundNodes = when (identifierConversion.type.dataType) {
                BGTableDataType.STRING -> {
                    unloadedNodes.map { Pair(it, BGNetworkTableHelper.getStringForNodeColumnName(it, identifierConversion.sourceColumn.name, sourceNetwork)) }
                            .filter {
                                !it.second.isNullOrEmpty()
                            }
                            .map {
                                Pair(it.first, identifierConversion.runForDataString(serviceManager, it.second!!))
                            }
                            .filter {
                                it.second != null
                            }
                            .map {
                                Pair(it.first.suid, serviceManager.dataModelController.getNodeFromCacheOrNetworks(BGNode(it.second!!)))
                            }
                            .toMap()
                }
                BGTableDataType.DOUBLE -> TODO()
                BGTableDataType.INT -> TODO()
                BGTableDataType.BOOLEAN -> TODO()
                BGTableDataType.UNSUPPORTED -> TODO()
                BGTableDataType.STRINGARRAY -> TODO()
                BGTableDataType.INTARRAY -> TODO()
                BGTableDataType.DOUBLEARRAY -> TODO()
            }
            newNodes.putAll(foundNodes)
        }

        if (newNodes.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Unable to load nodes with the specified identifiers.")
            return
        }
        // Fetch data from the dictionary server.
        serviceManager.dataModelController.loadNodesFromServerSynchronously(newNodes.values)

        // Fetch the extra node metadata from the source network:
        val nodeMetadata = HashMap<BGNode, HashSet<BGNodeMetadata>>()
        for (cyNode in sourceNetwork.nodeList) {
            loop@ for (conversion in nodeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForNodeColumnName(cyNode, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForNodeColumnName(cyNode, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value) ?: continue@loop
                    }

                    BGTableDataType.INT, BGTableDataType.BOOLEAN, BGTableDataType.STRINGARRAY, BGTableDataType.DOUBLEARRAY, BGTableDataType.INTARRAY -> {
                        val value = BGNetworkTableHelper.getValueForNodeColumnName(cyNode, conversion.sourceColumn.name, sourceNetwork, conversion.sourceColumn.type)
                        value
                    }

//                    BGTableDataType.STRINGARRAY, BGTableDataType.DOUBLEARRAY, BGTableDataType.INTARRAY -> {
//                        val value = BGNetworkTableHelper.getListForNodeColumnName(cyNode, conversion.sourceColumn.name, network)
//                        value
//                    }
//                    BGTableDataType.INT, BGTableDataType.BOOLEAN -> {
//                        val value = BGNetworkTableHelper.getValueForNodeColumnName(cyNode, conversion.sourceColumn.name, network, conversion.sourceColumn.type)
//                        value
//                    }
                    BGTableDataType.UNSUPPORTED -> null
                } ?: continue

                val metadata = BGNodeMetadata(conversion.type.dataType, result, conversion.destinationFieldName)
                newNodes[cyNode.suid]?.let {
                    if (!nodeMetadata.containsKey(it)) {
                        nodeMetadata[it] = HashSet()
                    }
                    nodeMetadata[it]?.add(metadata)
                }
            }
        }

        // Add the new nodes to the network together with metadata:
        networkBuilder.addBGNodesToNetworkWithMetadata(newNodes.values, nodeMetadata, destinationNetwork)

        // Get all the relations in the source network which goes between nodes that could be imported:
        val relationsMap = sourceNetwork.edgeList
                .filter { newNodes.keys.contains(it.source.suid) }
                .filter { newNodes.keys.contains(it.target.suid)  }
                .associateBy({it}, {
            val name = BGNetworkTableHelper.getStringForEdgeColumnName(it, "name", sourceNetwork) ?: ""
            val from = newNodes[it.source.suid]!! // We know these to be present because of the filter above.
            val to = newNodes[it.target.suid]!!
            BGRelation(from, BGExternalRelationType(name), to) })

        // Get additional metadata for these CyEdges:

        for (cyEdge in relationsMap.keys) {
            loop@ for (conversion in edgeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForEdgeColumnName(cyEdge, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForEdgeColumnName(cyEdge, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value) ?: continue@loop
                    }
                    BGTableDataType.STRINGARRAY, BGTableDataType.DOUBLEARRAY, BGTableDataType.INTARRAY, BGTableDataType.INT ,BGTableDataType.BOOLEAN -> {
                        val value = BGNetworkTableHelper.getValueForEdgeColumnName(cyEdge, conversion.sourceColumn.name, sourceNetwork, conversion.sourceColumn.type) ?: continue@loop
                        value
                    }
                    BGTableDataType.UNSUPPORTED -> null
                } ?: continue

                val metadata = BGRelationMetadata(conversion.type.dataType, result)
                relationsMap[cyEdge]?.let {
                    it.metadata[conversion.destinationFieldName] = metadata
                }
            }
        }

        networkBuilder.addRelationsToNetwork(destinationNetwork, relationsMap.values)
        // Create a network view for the new network:
        networkBuilder.createNetworkView(destinationNetwork)
    }

    fun addExportColumns(network: CyNetwork, nodeConversions: Collection<BGConversion>, edgeConversions: Collection<BGConversion>) {
        val networkBuilder = serviceManager.dataModelController.networkBuilder

        // Fetch the extra node metadata from the source network:
        val nodeMetadata = HashMap<CyNode, HashSet<BGNodeMetadata>>()
        for (cyNode in network.nodeList) {
            loop@ for (conversion in nodeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForNodeColumnName(cyNode, conversion.sourceColumn.name, network) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForNodeColumnName(cyNode, conversion.sourceColumn.name, network) ?: continue@loop
                        conversion.runForDataString(serviceManager, value) ?: continue@loop
                    }
                    else -> null } ?: continue

                val metadata = BGNodeMetadata(conversion.type.dataType, result, conversion.destinationFieldName)
                    if (!nodeMetadata.containsKey(cyNode)) {
                        nodeMetadata[cyNode] = HashSet()
                    }
                    nodeMetadata[cyNode]?.add(metadata)
                }
            }

        networkBuilder.updateMetadataForCyNodes(nodeMetadata, network.defaultNodeTable)

        // TODO: Add support for conveting edge data.

        /*
        val edgeMetadata = HashMap<CyEdge, HashSet<BGRelationMetadata>>()

        for (cyEdge in network.edgeList) {
            loop@ for (conversion in edgeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForEdgeColumnName(cyEdge, conversion.sourceColumn.name, network) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForEdgeColumnName(cyEdge, conversion.sourceColumn.name, network) ?: continue@loop
                        conversion.runForDataString(serviceManager, value) ?: continue@loop
                    }
                    else -> null } ?: continue

                val metadata = BGRelationMetadata(conversion.type.dataType, result)
                // TODO: Complete this.
            }
        }
        */


        }



    fun exportNetworkToCopy(sourceNetwork: CyNetwork, nodeConversions: Collection<BGConversion>, edgeConversions: Collection<BGConversion>) {

        val networkBuilder = serviceManager.dataModelController.networkBuilder
        val network = networkBuilder.createNetwork()
        serviceManager.networkManager?.addNetwork(network)

        // This maps the SUID of the source CyNode to the new BGNodes.


        val newNodes = sourceNetwork.nodeList.map { Pair(it, it.getUri(sourceNetwork)) }
                .filter {
                    !it.second.isEmpty()
                }
                .map {
                    Pair(it.first.suid, serviceManager.dataModelController.getNodeFromCacheOrNetworks(BGNode(it.second)))
                }
                .toMap()

        if (newNodes.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Unable to load nodes with the specified identifiers.")
            return
        }
        // Fetch data from the dictionary server.
        serviceManager.dataModelController.loadNodesFromServerSynchronously(newNodes.values)

        // Fetch the extra node metadata from the source network:
        val nodeMetadata = HashMap<BGNode, HashSet<BGNodeMetadata>>()
        for (cyNode in sourceNetwork.nodeList) {
            loop@ for (conversion in nodeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForNodeColumnName(cyNode, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForNodeColumnName(cyNode, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value) ?: continue@loop
                    }
                    else -> null } ?: continue

                val metadata = BGNodeMetadata(conversion.type.dataType, result, conversion.destinationFieldName)
                newNodes[cyNode.suid]?.let {
                    if (!nodeMetadata.containsKey(it)) {
                        nodeMetadata[it] = HashSet()
                    }
                    nodeMetadata[it]?.add(metadata)
                }
            }
        }


        // Add the new nodes to the network together with metadata:
        networkBuilder.addBGNodesToNetworkWithMetadata(newNodes.values, nodeMetadata, network)

        // Get all the relations in the source network which goes between nodes that could be imported:
        val relationsMap = sourceNetwork.edgeList
                .filter { newNodes.keys.contains(it.source.suid) }
                .filter { newNodes.keys.contains(it.target.suid)  }
                .associateBy({it}, {
                    val name = BGNetworkTableHelper.getStringForEdgeColumnName(it, "interaction", sourceNetwork) ?: ""
                    val from = newNodes[it.source.suid]!! // We know these to be present because of the filter above.
                    val to = newNodes[it.target.suid]!!
                    BGRelation(from, BGExternalRelationType(name), to) })

        // Get additional metadata for these CyEdges:

        for (cyEdge in relationsMap.keys) {
            loop@ for (conversion in edgeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForEdgeColumnName(cyEdge, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForEdgeColumnName(cyEdge, conversion.sourceColumn.name, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value) ?: continue@loop
                    }
                    else -> null } ?: continue

                val metadata = BGRelationMetadata(conversion.type.dataType, result)
                relationsMap[cyEdge]?.let {
                    it.metadata[conversion.destinationFieldName] = metadata
                }
            }
        }

        networkBuilder.addRelationsToNetwork(network, relationsMap.values)
        // Create a network view for the new network:
        networkBuilder.createNetworkView(network)
    }
}