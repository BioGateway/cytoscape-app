package eu.biogateway.cytoscape.internal.gui

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGConversionType
import java.awt.FlowLayout
import javax.swing.*

enum class BGTableDataType {
    STRING, DOUBLE, INT, BOOLEAN
}

class BGImportConversionLine(val serviceManager: BGServiceManager, val conversionTypes: Array<BGConversionType>, val columnName: String, val dataType: BGConversionType.DataType): JPanel() {

    val importNameField = JTextField()
    val importConversionTypeComboBox: JComboBox<BGConversionType>
    val checkBox = JCheckBox()

    init {
        importNameField.columns = 20
        val layout = FlowLayout()
        layout.alignment = FlowLayout.LEFT
        this.layout = layout
        this.add(checkBox)
        checkBox.isSelected = false
        this.add(JLabel(columnName))
        importConversionTypeComboBox = JComboBox(conversionTypes)
        this.add(importConversionTypeComboBox)
        this.add(importNameField)
    }
}