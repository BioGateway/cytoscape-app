package eu.biogateway.cytoscape.internal.gui.conversion

import eu.biogateway.cytoscape.internal.model.BGNodeConversionType
import eu.biogateway.cytoscape.internal.model.BGNodeType
import org.cytoscape.model.CyColumn
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JPanel

class BGImportIdentifierLine(val nodeTypes: Array<BGNodeType>, val columns: Array<CyColumn>, val conversionTypes: Array<BGNodeConversionType>): JPanel() {

    val nodeTypeComboBox: JComboBox<BGNodeType>
    val sourceColumnComboBox = JComboBox(columns)
    val importConversionTypeComboBox: JComboBox<BGNodeConversionType>


    val nodeType: BGNodeType get() {
        return nodeTypeComboBox.selectedItem as BGNodeType
    }

    val sourceColumn: CyColumn get() {
        return sourceColumnComboBox.selectedItem as CyColumn
    }

    val conversionType: BGNodeConversionType get() {
        return importConversionTypeComboBox.selectedItem as BGNodeConversionType
    }

    init {

        val layout = FlowLayout()
        layout.alignment = FlowLayout.CENTER
        this.layout = layout

        nodeTypeComboBox = JComboBox(nodeTypes)
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
                .forEach { model.addElement(it) }
    }
}