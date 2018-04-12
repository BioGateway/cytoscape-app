package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGNetworkTableHelper
import org.cytoscape.model.CyNetwork
import javax.swing.JOptionPane

class BGNetworkConverter(val serviceManager: BGServiceManager) {

    fun importNetwork(sourceNetwork: CyNetwork, identifierConversions: Collection<BGIdentifierConversion>, nodeConversions: Collection<BGConversion>, edgeConversions: Collection<BGConversion>) {

        // TODO: Incomplete

        val networkBuilder = serviceManager.dataModelController.networkBuilder

        val network = networkBuilder.createNetwork()
        serviceManager.networkManager?.addNetwork(network)

        /*
         We want to end up with an map of source CyNodes and the BGNodes they import to.

         First, we need to run all the nodeConversions, and get the appropriate nodes.

         Ideally, the nodes will only have ONE identifying column. Or, at least only get result data for one.
         Another, naive solution is to run the first identifying pass, and then only run the subsequent ones for
         the unmatched CyNodes. The list would then be a priority list of identifiers. The upside to this is that
         nodes that aren't found, can be given alternate options, or even end up with an identifier not recognized
         by BioGateway, but still preserved in the new network.

         The downside to that approach would be that it might prioritize "bad" data for some data types, especially if an entity
         has both the Protein and Gene identifier, as is common in some networks. We might propose to not support this altogether.

        */

        // This maps the SUID of the source CyNode to the new BGNodes.
        val newNodes = HashMap<Long, BGNode>()

        for (identifierConversion in identifierConversions) {

            val unloadedNodes = sourceNetwork.nodeList.filter { !newNodes.keys.contains(it.suid) }

            val foundNodes = when (identifierConversion.type.dataType) {
                BGTableDataType.STRING -> {
                    unloadedNodes.map { Pair(it, BGNetworkTableHelper.getStringForNodeColumnName(it, identifierConversion.sourceFieldName, sourceNetwork)) }
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
                        val value =  BGNetworkTableHelper.getDoubleForNodeColumnName(cyNode, conversion.sourceFieldName, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForNodeColumnName(cyNode, conversion.sourceFieldName, sourceNetwork) ?: continue@loop
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
            val name = BGNetworkTableHelper.getStringForEdgeColumnName(it, "name", sourceNetwork) ?: ""
            val from = newNodes[it.source.suid]!! // We know these to be present because of the filter above.
            val to = newNodes[it.target.suid]!!
            BGRelation(from, BGExternalRelationType(name), to) })

        // Get additional metadata for these CyEdges:

        for (cyEdge in relationsMap.keys) {
            loop@ for (conversion in edgeConversions) {
                val result = when (conversion.type.dataType) {
                    BGTableDataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForEdgeColumnName(cyEdge, conversion.sourceFieldName, sourceNetwork) ?: continue@loop
                        conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                    }
                    BGTableDataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForEdgeColumnName(cyEdge, conversion.sourceFieldName, sourceNetwork) ?: continue@loop
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
        networkBuilder.createNetworkView(network, serviceManager)
    }
}