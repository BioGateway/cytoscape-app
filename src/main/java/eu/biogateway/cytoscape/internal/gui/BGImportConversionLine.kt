package eu.biogateway.cytoscape.internal.gui

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGConversionType
import eu.biogateway.cytoscape.internal.model.BGNodeConversionType
import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.model.BGTableDataType
import org.cytoscape.model.CyColumn
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class BGImportIdentifierLine(val serviceManager: BGServiceManager, val nodeTypes: Array<BGNodeType>, val columns: Array<CyColumn>, val conversionTypes: Array<BGNodeConversionType>): JPanel() {

    val nodeTypeComboBox: JComboBox<BGNodeType>
    val sourceColumnComboBox: JComboBox<String>
    val importConversionTypeComboBox: JComboBox<BGNodeConversionType>

    init {

        val layout = FlowLayout()
        layout.alignment = FlowLayout.CENTER
        this.layout = layout

        nodeTypeComboBox = JComboBox(nodeTypes)
        sourceColumnComboBox = JComboBox(columns.map { it.name }.toTypedArray())
        importConversionTypeComboBox = JComboBox(conversionTypes)

        nodeTypeComboBox.addActionListener {
            updateForNodeTypeSelected()
        }

        this.add(nodeTypeComboBox)
        this.add(importConversionTypeComboBox)
        this.add(sourceColumnComboBox)
        updateForNodeTypeSelected()

    }

    fun updateForNodeTypeSelected() {
        val nodeType = nodeTypeComboBox.selectedItem as? BGNodeType ?: return

        val model = importConversionTypeComboBox.model as DefaultComboBoxModel
        model.removeAllElements()
        for (conversion in conversionTypes) {
            if (conversion.nodeType == nodeType) {
                model.addElement(conversion)
            }
        }
    }

    fun updateForConversionTypeSelected(conversionType: BGNodeConversionType) {
        val model = sourceColumnComboBox.model as DefaultComboBoxModel
        model.removeAllElements()
        columns.filter { it.type.canonicalName == conversionType.dataType.javaCanonicalName }
                .forEach { model.addElement(it.name) }
    }
}

class BGImportConversionLine(val serviceManager: BGServiceManager, val conversionTypes: Array<BGConversionType>, val columnName: String, val dataType: BGTableDataType): JPanel() {

    val importNameField = JTextField()
    val importConversionTypeComboBox: JComboBox<BGConversionType>
    val checkBox = JCheckBox()

    init {
        val leftLayout = FlowLayout()
        leftLayout.alignment = FlowLayout.LEFT
        val leftPanel = JPanel(leftLayout)
        val rightLayout = FlowLayout()
        rightLayout.alignment = FlowLayout.RIGHT
        val rightPanel = JPanel(rightLayout)

        // Layout debug coloring:
//        this.background = Color.CYAN
//        leftPanel.background = Color.RED
//        rightPanel.background = Color.GREEN

        val layout = BorderLayout()
        this.layout = layout
        this.add(leftPanel)
        layout.addLayoutComponent(leftPanel, BorderLayout.WEST)
        this.add(rightPanel)
        layout.addLayoutComponent(rightPanel, BorderLayout.EAST)

        importNameField.columns = 20
        leftPanel.add(checkBox)
        checkBox.isSelected = false
        leftPanel.add(JLabel(columnName))
        importConversionTypeComboBox = JComboBox(conversionTypes)
        rightPanel.add(importConversionTypeComboBox)
        rightPanel.add(importNameField)
    }
}