package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGNetworkTableHelper
import eu.biogateway.cytoscape.internal.parser.setDoubleForColumnName
import eu.biogateway.cytoscape.internal.parser.setStringForColumnName
import org.cytoscape.model.CyNetwork

class BGNetworkConverter(val serviceManager: BGServiceManager) {


    fun convertNetwork(sourceNetwork: CyNetwork, conversions: Collection<BGConversion>) {
        // TODO: Incomplete

        val networkBuilder = serviceManager.dataModelController.networkBuilder

        val network = networkBuilder.createNetwork()
        serviceManager.networkManager?.addNetwork(network)

        for (cyNode in sourceNetwork.nodeList) {
            val newNode = network.addNode()
            loop@ for (conversion in conversions) {
                when (conversion.type.dataType) {
                    BGConversionType.DataType.DOUBLE -> {
                        val value =  BGNetworkTableHelper.getDoubleForNodeColumnName(cyNode, conversion.sourceFieldName, sourceNetwork) ?: continue@loop
                        val result = conversion.runForDataString(serviceManager, value.toString())?.toDouble() ?: continue@loop
                        BGNetworkTableHelper.assureThatNodeColumnExists(network.defaultNodeTable, conversion.destinationFieldName, conversion.type.dataType, false)
                        newNode.setDoubleForColumnName(result, conversion.destinationFieldName, network.defaultNodeTable)
                    }
                    BGConversionType.DataType.STRING -> {
                        val value =  BGNetworkTableHelper.getStringForNodeColumnName(cyNode, conversion.sourceFieldName, sourceNetwork) ?: continue@loop
                        val result = conversion.runForDataString(serviceManager, value) ?: continue@loop
                        BGNetworkTableHelper.assureThatNodeColumnExists(network.defaultNodeTable, conversion.destinationFieldName, conversion.type.dataType, false)
                        newNode.setStringForColumnName(result, conversion.destinationFieldName, network.defaultNodeTable)
                    }
                    else -> null
                } ?: continue
            }
        }
        networkBuilder.createNetworkView(network, serviceManager)
    }
}