package eu.biogateway.cytoscape.internal.gui.conversion

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.gui.BGImportExportView
import eu.biogateway.cytoscape.internal.model.*
import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyNetwork
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import javax.swing.DefaultComboBoxModel
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JPanel

class BGImportExportController() {

    private val view = BGImportExportView(this)

    private var convertBottomPadPanel = JPanel(FlowLayout(FlowLayout.LEFT))
    private var importBottomPadPanel = JPanel(FlowLayout(FlowLayout.LEFT))

    private val identifierConversionLines = ArrayList<BGImportIdentifierLine>()
    private val exportConversionLines = ArrayList<BGConversionLine>()
    private val importConversionLines = ArrayList<BGConversionLine>()
    private var network: CyNetwork?

    init {
        // Initialize with the current selected network, if it is set.
        network = BGServiceManager.applicationManager?.currentNetwork

        val networks = BGServiceManager.networkManager?.networkSet?.toTypedArray()

        if (networks != null && networks.isNotEmpty()) {
            view.sourceNetworkComboBox.model = DefaultComboBoxModel(networks)
            if (network != null) {
                view.sourceNetworkComboBox.model.selectedItem = network
            }
        } else {
            view.sourceNetworkComboBox.model = DefaultComboBoxModel(arrayOf("No networks found."))
        }

        view.sourceNetworkComboBox.addActionListener {
            val selectedNetwork = view.sourceNetworkComboBox.selectedItem as? CyNetwork ?: return@addActionListener
            network = selectedNetwork
            clearUI()
            loadUI()
        }



        val convertAddButton = JButton("Add")
        convertAddButton.addActionListener {
            addOutputConversionLine()
        }
        convertBottomPadPanel.add(convertAddButton)

        val importAddButton = JButton("Add")
        importAddButton.addActionListener {
            addImportConversionLine()
        }
        importBottomPadPanel.add(importAddButton)

        loadUI()
    }

    fun clearUI() {
        view.nodeImportIdentifiersPanel.removeAll()
        view.nodeImportsPanel.removeAll()
        view.convertColumnsPanel.removeAll()
        identifierConversionLines.clear()
        importConversionLines.clear()
        exportConversionLines.clear()
    }

    fun loadUI() {
        addIdentifierLine()
        addImportConversionLine()
        addOutputConversionLine()
    }

    fun addIdentifierLine() {
        val network = network ?: return
        val importNodeConversions = BGServiceManager.cache.importNodeConversionTypes ?: return
        val nodeColumns = network.defaultNodeTable.columns
        val identifierConversions = importNodeConversions
                .filter { it.biogwId.equals("identifier uri") }
                .sortedBy { it.id }.toTypedArray()

        // Finds the columns compatible with any of the available identifier conversion data types.
        val identifierColumns = nodeColumns
                .filter { !it.isPrimaryKey }
                .filter { identifierConversions.map { it.dataType }.contains(getDataTypeForColumn(it))}
                .sortedBy { it.name }.toTypedArray()

        val nodeTypes = identifierConversions.map { it.nodeType }.toHashSet().toTypedArray().sortedArray()
        val identifierLine = BGImportIdentifierLine(nodeTypes, identifierColumns, identifierConversions)
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
        view.nodeImportIdentifiersPanel.updateUI()
    }

    internal fun getDataTypeForColumn(column: CyColumn): BGTableDataType {
        val columnType: Class<*> = column.type

        // Ugly hack to use the class name, but the Kotlin class check for Double::class.java did not work.
        return when (columnType.canonicalName) {
            BGTableDataType.STRING.javaCanonicalName -> BGTableDataType.STRING
            BGTableDataType.DOUBLE.javaCanonicalName -> BGTableDataType.DOUBLE
            BGTableDataType.BOOLEAN.javaCanonicalName -> BGTableDataType.BOOLEAN
            BGTableDataType.STRINGARRAY.javaCanonicalName -> {
                print("Found list!")
                BGTableDataType.STRINGARRAY
            }
            else -> BGTableDataType.UNSUPPORTED
        }
    }


    fun addBottomPadding(parentPanel: JPanel, padPanel: JPanel, gridY: Int) {

        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.BOTH
        constraints.gridy = gridY
        constraints.weighty = 1.0

        parentPanel.remove(padPanel)
        parentPanel.add(padPanel, constraints)
        parentPanel.updateUI()
    }

    fun addConversionLine(nodeConversions: Collection<BGConversionType>,
                          edgeConversions: Collection<BGConversionType>,
                          conversionLines: ArrayList<BGConversionLine>,
                          panel: JPanel,
                          padPanel: JPanel) {
        val network = network ?: return

        val deleteIcon = ImageIcon(this.javaClass.classLoader.getResource("delete.png"))
        val deleteButton = JButton(deleteIcon)

        val line = BGConversionLine(nodeConversions.toTypedArray(),
                network.defaultNodeTable.columns.filter { !it.isPrimaryKey }.map { it }.toTypedArray(),
                edgeConversions.toTypedArray(),
                network.defaultEdgeTable.columns.filter { !it.isPrimaryKey }.map { it }.toTypedArray(),
                deleteButton)

        deleteButton.addActionListener {
            conversionLines.remove(line)
            panel.remove(line)
            panel.updateUI()
        }

        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridy = conversionLines.size
        constraints.weightx = 0.5
        constraints.anchor = GridBagConstraints.NORTH

        conversionLines.add(line)
        panel.add(line, constraints)

        addBottomPadding(panel, padPanel, conversionLines.size)
    }

    fun addOutputConversionLine() {
        val edgeConversions = BGServiceManager.cache.exportEdgeConversionTypes ?: return
        val nodeConversions = BGServiceManager.cache.exportNodeConversionTypes ?: return

        addConversionLine(nodeConversions, edgeConversions, exportConversionLines, view.convertColumnsPanel, convertBottomPadPanel)
    }

    fun addImportConversionLine() {
        val edgeConversions = BGServiceManager.cache.importEdgeConversionTypes ?: return
        val nodeConversions = BGServiceManager.cache.importNodeConversionTypes ?: return
        addConversionLine(nodeConversions, edgeConversions, importConversionLines, view.nodeImportsPanel, importBottomPadPanel)
    }


    fun runImport() {
        val network = network ?: return
        val nodeIdentifiers = identifierConversionLines
                .map {
                    BGIdentifierConversion(it.nodeType, it.conversionType, network, it.sourceColumn, it.conversionType.biogwId)
                }
        val nodeConversions = importConversionLines
                .filter { it.conversionClass == BGConversionLine.ConversionClass.NODE}
                .filter { it.sourceColumn != null }
                .map {
                    BGConversion(it.conversionType, network, it.sourceColumn!!, it.destinationColumnName)
                }
        val edgeConversions = importConversionLines
                .filter { it.conversionClass == BGConversionLine.ConversionClass.EDGE}
                .filter { it.sourceColumn != null }
                .map {
                    BGConversion(it.conversionType, network, it.sourceColumn!!, it.destinationColumnName)
                }
        BGServiceManager.networkConverter.importNetwork(network, nodeIdentifiers, nodeConversions, edgeConversions)
    }

    fun runConvertColumns() {
        val network = network ?: return
        val nodeConversions = exportConversionLines
                .filter { it.conversionClass == BGConversionLine.ConversionClass.NODE}
                .filter { it.sourceColumn != null }
                .map {
            BGConversion(it.conversionType, network, it.sourceColumn!!, it.destinationColumnName)
        }
        val edgeConversions = exportConversionLines
                .filter { it.conversionClass == BGConversionLine.ConversionClass.EDGE}
                .filter { it.sourceColumn != null }
                .map {
                    BGConversion(it.conversionType, network, it.sourceColumn!!, it.destinationColumnName)
                }
        BGServiceManager.networkConverter.exportNetwork(network, nodeConversions, edgeConversions)
    }
}
