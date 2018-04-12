package eu.biogateway.cytoscape.internal.gui

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.*
import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyNetwork
import javax.swing.ImageIcon
import javax.swing.JButton

class BGImportExportController(val serviceManager: BGServiceManager) {

    private val view = BGImportExportView(this)
    private val identifierConversionLines = ArrayList<BGImportIdentifierLine>()

    private val nodeConversionLines = ArrayList<BGImportConversionLine>()
    private val edgeConversionLines = ArrayList<BGImportConversionLine>()
    private val network: CyNetwork?

    init {
        addIdentifierLine()
        loadImportsToUI()
        network = serviceManager.applicationManager?.currentNetwork
    }

    fun runImports() {

        if (network == null) return

        val nodeConversions = HashSet<BGConversion>()
        val edgeConversions = HashSet<BGConversion>()

        val identifierConversions = HashSet<BGIdentifierConversion>()

        for (line in identifierConversionLines) {
            val nodeType = line.nodeTypeComboBox.selectedItem as? BGNodeType ?: continue
            val columnName = line.sourceColumnComboBox.selectedItem as? String ?: continue
            val type = line.importConversionTypeComboBox.selectedItem as? BGConversionType ?: continue

            val conversion = BGIdentifierConversion(nodeType, type, network, columnName, type.biogwId)
            identifierConversions.add(conversion)
        }

        for (line in nodeConversionLines) {
            if (!line.checkBox.isSelected) continue

            val type = line.importConversionTypeComboBox.selectedItem as? BGConversionType ?: continue
            var destinationColumnName = line.importNameField.text
            if (destinationColumnName.isEmpty()) {
                destinationColumnName = line.columnName
            }

            val conversion = BGConversion(type, network, line.columnName, destinationColumnName)
            nodeConversions.add(conversion)
        }

        for (line in edgeConversionLines) {
            if (!line.checkBox.isSelected) continue

            val type = line.importConversionTypeComboBox.selectedItem as? BGConversionType ?: continue
            var destinationColumnName = line.importNameField.text
            if (destinationColumnName.isEmpty()) {
                destinationColumnName = line.columnName
            }

            val conversion = BGConversion(type, network, line.columnName, destinationColumnName)
            edgeConversions.add(conversion)
        }

        // TODO: Send the edge conversions too.
        serviceManager.networkConverter.importNetwork(network, identifierConversions, nodeConversions, edgeConversions)

    }

    internal fun getDataTypeForColumn(column: CyColumn): BGTableDataType {
        val columnType: Class<*> = column.type

        // Ugly hack to use the class name, but the Kotlin class check for Double::class.java did not work.
        return when (columnType.canonicalName) {
            BGTableDataType.STRING.javaCanonicalName -> BGTableDataType.STRING
            BGTableDataType.DOUBLE.javaCanonicalName -> BGTableDataType.DOUBLE
            BGTableDataType.BOOLEAN.javaCanonicalName -> BGTableDataType.BOOLEAN
            else -> BGTableDataType.UNSUPPORTED
        }
    }

    private fun loadImportsToUI() {
        // Load node conversions:
        val currentNetwork = serviceManager.applicationManager?.currentNetwork ?: return
        val importNodeConversions = serviceManager.cache.importNodeConversionTypes ?: return
        val nodeColumns = currentNetwork.defaultNodeTable.columns
        val edgeColumns = currentNetwork.defaultEdgeTable.columns

        for (column in nodeColumns) {
            if (column.isPrimaryKey) continue

            val type = getDataTypeForColumn(column)
            val filteredConversions = importNodeConversions
                    .filter { it.dataType.equals(type) }
                    .sortedBy { it.id }.toTypedArray() as Array<BGConversionType> // Why is this unchecked when casting to a superclass?
            if (filteredConversions.isEmpty()) continue
            val line = BGImportConversionLine(serviceManager, filteredConversions, column.name, type)
            view.nodeImportsPanel.add(line)
            nodeConversionLines.add(line)
        }

        // Load edge conversions:

        val importEdgeConversions = serviceManager.cache.importEdgeConversionTypes ?: return

        for (column in edgeColumns) {
            if (column.isPrimaryKey) continue

            val type = getDataTypeForColumn(column)
            val filteredConversions = importEdgeConversions
                    .filter { it.dataType.equals(type) }
                    .sortedBy { it.id }.toTypedArray() as Array<BGConversionType> // Why is this unchecked when casting to a superclass?
            if (filteredConversions.isEmpty()) continue
            val line = BGImportConversionLine(serviceManager, filteredConversions, column.name, type)
            view.edgeImportsPanel.add(line)
            edgeConversionLines.add(line)
        }

    }

    fun addIdentifierLine() {

        val currentNetwork = serviceManager.applicationManager?.currentNetwork ?: return
        val importNodeConversions = serviceManager.cache.importNodeConversionTypes ?: return
        val nodeColumns = currentNetwork.defaultNodeTable.columns
        val identifierConversions = importNodeConversions
                .filter { it.biogwId.equals("identifier uri") }
                .sortedBy { it.id }.toTypedArray()

        // Finds the columns compatible with any of the available identifier conversion data types.
        val identifierColumns = nodeColumns
                .filter { !it.isPrimaryKey }
                .filter { identifierConversions.map { it.dataType }.contains(getDataTypeForColumn(it))}
                .sortedBy { it.name }.toTypedArray()

        val nodeTypes = identifierConversions.map { it.nodeType }.sortedBy { it.paremeterType }.toHashSet().toTypedArray()
        val identifierLine = BGImportIdentifierLine(serviceManager, nodeTypes, identifierColumns, identifierConversions)
        val deleteIcon = ImageIcon(this.javaClass.classLoader.getResource("delete.png"))
        val deleteButton = JButton(deleteIcon)
        deleteButton.addActionListener {
            identifierConversionLines.remove(identifierLine)
            view.nodeImportIdentifiersPanel.remove(identifierLine)
            view.nodeImportIdentifiersPanel.updateUI()
        }
        identifierLine.add(deleteButton)
        view.nodeImportIdentifiersPanel.add(identifierLine)
        identifierConversionLines.add(identifierLine)
    }


}
