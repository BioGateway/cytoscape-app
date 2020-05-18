package eu.biogateway.app.internal.gui.conversion

import eu.biogateway.app.internal.model.BGConversionType
import eu.biogateway.app.internal.model.BGTableDataType
import org.cytoscape.model.CyColumn
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class BGConversionLine(val nodeConversionTypes: Array<BGConversionType>, val nodeColumns: Array<CyColumn>, val edgeConversionTypes: Array<BGConversionType>, val edgeColumns: Array<CyColumn>, val deleteButton: JButton): JPanel() {

    enum class ConversionClass {
        NODE, EDGE
    }

    var conversionClass = ConversionClass.NODE

    val importNameField = JTextField()
    val sourceColumnComboBox = JComboBox(nodeColumns)
    val conversionTypeComboBox: JComboBox<BGConversionType> = JComboBox()
    //val checkBox = JCheckBox()


    val edgeNodeComboBox = JComboBox(arrayOf("Node", "Edge"))

    val dataType: BGTableDataType get() {
        return conversionType.dataType
    }

    val conversionType: BGConversionType get() {
        return conversionTypeComboBox.selectedItem as BGConversionType
    }

    val sourceColumn: CyColumn? get() {
        return sourceColumnComboBox.model.selectedItem as? CyColumn
    }

    val destinationColumnName: String get() {
        var name = importNameField.text
        if (name.isNullOrEmpty()) {
            name = sourceColumn?.name+":"+conversionType.name
        }
        return name
    }

    init {
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

        val layout = BorderLayout()
        this.add(leftPanel)
        layout.addLayoutComponent(leftPanel, BorderLayout.WEST)
        this.add(rightPanel)
        layout.addLayoutComponent(rightPanel, BorderLayout.EAST)
        this.layout = layout

        importNameField.columns = 10
//        leftPanel.add(checkBox)
//        checkBox.isSelected = true

        leftPanel.add(edgeNodeComboBox)

        leftPanel.add(sourceColumnComboBox)
        //conversionTypeComboBox = JComboBox(nodeConversionTypes)
        rightPanel.add(conversionTypeComboBox)
        rightPanel.add(importNameField)
        rightPanel.add(deleteButton)

        edgeNodeComboBox.addActionListener {
            val newClass = when (edgeNodeComboBox.selectedIndex) {
                0 -> ConversionClass.NODE
                1 -> ConversionClass.EDGE
                else -> null } ?: return@addActionListener

            if (conversionClass == newClass) return@addActionListener

            conversionClass = newClass

            updateSourceComboBoxModel()
        }
        sourceColumnComboBox.addActionListener {
            updateConversionTypeComboBoxModel()
        }

        updateSourceComboBoxModel()
    }

    private fun updateSourceComboBoxModel() {
        val sourceModel = sourceColumnComboBox.model as DefaultComboBoxModel
        sourceModel.removeAllElements()

        when (conversionClass) {
            BGConversionLine.ConversionClass.NODE -> {
                val supportedTypes = nodeConversionTypes.map { it.dataType }
                nodeColumns.filter { supportedTypes.contains(getDataTypeForColumn(it)) }
                        .forEach { sourceModel.addElement(it) }
            }
            BGConversionLine.ConversionClass.EDGE -> {
                val supportedTypes = edgeConversionTypes.map { it.dataType }
                edgeColumns.filter { supportedTypes.contains(getDataTypeForColumn(it)) }
                        .forEach { sourceModel.addElement(it) }
            }
        }
    }

    private fun updateConversionTypeComboBoxModel() {
        val conversionModel = conversionTypeComboBox.model as DefaultComboBoxModel
        val column = sourceColumn ?: return
        val sourceType = getDataTypeForColumn(column)
        conversionModel.removeAllElements()

        when (conversionClass) {
            BGConversionLine.ConversionClass.NODE -> {
                nodeConversionTypes.filter { it.dataType == sourceType }.sortedBy { it.name }
                        .forEach { conversionModel.addElement(it) }
            }
            BGConversionLine.ConversionClass.EDGE -> {
                edgeConversionTypes.filter { it.dataType == sourceType }.sortedBy { it.name }
                        .forEach { conversionModel.addElement(it) }
            }
        }
    }

    internal fun getDataTypeForColumn(column: CyColumn): BGTableDataType {
        val columnType: Class<*> = column.type

        // Ugly hack to use the class name, but the Kotlin class check for Double::class.java did not work.
        return when (columnType.canonicalName) {
            BGTableDataType.STRING.javaCanonicalName -> BGTableDataType.STRING
            BGTableDataType.DOUBLE.javaCanonicalName -> BGTableDataType.DOUBLE
            BGTableDataType.BOOLEAN.javaCanonicalName -> BGTableDataType.BOOLEAN
            BGTableDataType.INT.javaCanonicalName -> BGTableDataType.INT

            "java.util.List" -> {
                val elementType: Class<*> = column.listElementType

                when (elementType.canonicalName) {
                    BGTableDataType.STRING.javaCanonicalName -> BGTableDataType.STRINGARRAY
                    BGTableDataType.DOUBLE.javaCanonicalName -> BGTableDataType.DOUBLEARRAY
                    BGTableDataType.INT.javaCanonicalName -> BGTableDataType.INTARRAY
                    else -> BGTableDataType.UNSUPPORTED
                }
            }
            else -> BGTableDataType.UNSUPPORTED
        }
    }
}