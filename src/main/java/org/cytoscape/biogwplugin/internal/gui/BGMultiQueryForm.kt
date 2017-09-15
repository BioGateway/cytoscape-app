package org.cytoscape.biogwplugin.internal.gui

import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextField

class BGMultiQueryLine(val fromTextField: JTextField, val relationTypeComboBox: JComboBox<String>, val toTextField: JTextField): JPanel() {

    init {
        this.layout = FlowLayout()

        val fromUriSearchButton = JButton("🔍")
        fromUriSearchButton.addActionListener {

        }
        val toUriSearchButton = JButton("🔍")

        this.add(fromTextField)
        this.add(relationTypeComboBox)
        this.add(toTextField)
    }

    var formUri: String?
        get() = {
            this.fromTextField.text
        }()
        set(value) {
            this.fromTextField.text = value
        }
    var toUri: String?
        get() = {
            this.toTextField.text
        }()
        set(value) {
            this.toTextField.text = value
        }
    val relationType: String?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? String
        }()
}

class BGMultiQueryForm(val relationTypes: Array<String>): JPanel() {
    var queryLines = ArrayList<BGMultiQueryLine>()

    fun addQueryLine() {
        val fromField = JTextField()
        val toField = JTextField()
        val relationTypeBox = JComboBox(relationTypes)
        queryLines.add(BGMultiQueryLine(fromField, relationTypeBox, toField))
    }

    fun removeQueryLine(queryLine: BGMultiQueryLine) {
        queryLines.remove(queryLine)
    }
}