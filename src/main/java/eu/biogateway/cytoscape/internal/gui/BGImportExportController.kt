package eu.biogateway.cytoscape.internal.gui

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGConversion
import eu.biogateway.cytoscape.internal.model.BGConversionType
import org.cytoscape.model.CyNetwork

class BGImportExportController(val serviceManager: BGServiceManager) {

    private val view = BGImportExportView(this)
    private val conversionLines = HashSet<BGImportConversionLine>()
    private val network: CyNetwork?

    init {
        loadImportsToUI()
        network = serviceManager.applicationManager?.currentNetwork
    }

    fun generateConversions() {

        if (network == null) return

        val conversions = HashSet<BGConversion>()

        for (line in conversionLines) {
            if (!line.checkBox.isSelected) continue

            val type = line.importConversionTypeComboBox.selectedItem as? BGConversionType ?: continue
            var destinationColumnName = line.importNameField.text
            if (destinationColumnName.isEmpty()) {
                destinationColumnName = line.columnName
            }

            val conversion = BGConversion(type, network, line.columnName, destinationColumnName)
            conversions.add(conversion)

        }
        serviceManager.networkConverter.convertNetwork(network, conversions)
    }

    private fun loadImportsToUI() {

        val cache = serviceManager.cache

        // Get all the import conversionTypes from the cache.

        // Get the available columns in the current network to import from.

        val currentNetwork = serviceManager.applicationManager?.currentNetwork ?: return
        val importNodeConversions = cache.importNodeConversionTypes ?: return
        val nodeColumns = currentNetwork.defaultNodeTable.columns

        // Create BGImportConversionLines for each applicable column.

        for (column in nodeColumns) {
            if (column.isPrimaryKey) continue

            val columnType: Class<*> = column.type

            // Ugly hack to use the class name, but the Kotlin class check for Double::class.java did not work.
            val type = when (columnType.canonicalName) {
                "java.lang.String" -> BGConversionType.DataType.STRING
                "java.lang.Double" -> BGConversionType.DataType.DOUBLE
                "java.lang.Boolean" -> BGConversionType.DataType.BOOLEAN
                else -> null
            } ?: continue

            val filteredConversions = importNodeConversions.filter { it.dataType.equals(type) }.toTypedArray()

            if (filteredConversions.isEmpty()) continue

            val line = BGImportConversionLine(serviceManager, filteredConversions, column.name, type)
            view.nodeImportsPanel.add(line)
            conversionLines.add(line)
        }
    }
}