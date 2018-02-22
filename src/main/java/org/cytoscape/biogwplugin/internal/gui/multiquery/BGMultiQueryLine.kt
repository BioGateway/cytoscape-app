package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.BGNodeLookupController
import org.cytoscape.biogwplugin.internal.util.Constants
import java.awt.FlowLayout
import javax.swing.*


class BGMultiQueryLine(val serviceManager: BGServiceManager, val relationTypeComboBox: JComboBox<String>, val variableManager: BGQueryVariableManager): JPanel() {

    val searchButtonTooltipText = "Search for entity URIs."
    val variablesTooltipText = "Choose URI to specify an entity, or pick a variable letter to find matching entities."

    val fromComboBox: JComboBox<String>
    val toComboBox: JComboBox<String>

    val fromTypeComboBox: JComboBox<String>
    val toTypeComboBox: JComboBox<String>

    val fromTextField: JTextField
    val toTextField: JTextField

    var currentFromUri: String? = null
    var currentToUri: String? = null

    fun updateComboBox(comboBox: JComboBox<String>, textField: JTextField) {
        val selectedVariable = comboBox.model.selectedItem as String
        if (selectedVariable == Constants.BG_QUERYBUILDER_ENTITY_LABEL) {
            // Make sure that the old variable is freed up.
            variableManager.unRegisterUseOfVariableForComponent(comboBox)
            variableManager.URIcomboBoxes.add(comboBox)
            //textField.isEnabled = true
        } else {
            variableManager.registerUseOfVariableForComponent(selectedVariable, comboBox)
            variableManager.URIcomboBoxes.remove(comboBox)
            //textField.isEnabled = false
        }
    }

    init {
        this.layout = FlowLayout()

        fromTextField = JTextField()
        toTextField = JTextField()

        fromComboBox = JComboBox(variableManager.getShownVariables())
        fromComboBox.toolTipText = variablesTooltipText
        updateComboBox(fromComboBox, fromTextField)
        fromComboBox.addActionListener {
            updateComboBox(fromComboBox, fromTextField)
        }


        toComboBox = JComboBox(variableManager.getShownVariables())
        toComboBox.toolTipText = variablesTooltipText
        updateComboBox(toComboBox, toTextField)
        toComboBox.addActionListener {
            updateComboBox(toComboBox, toTextField)
        }


        val types = arrayOf("Protein", "Gene", "GO-Term", "All")
        toTypeComboBox = JComboBox(types)
        fromTypeComboBox = JComboBox(types)

        val searchIcon = ImageIcon(this.javaClass.classLoader.getResource("search.png"))
        val fromUriSearchButton = JButton(searchIcon)
        fromUriSearchButton.toolTipText = searchButtonTooltipText
        fromUriSearchButton.addActionListener {
            val lookupController = BGNodeLookupController(serviceManager, this) {
                if (it != null) {
                    this.fromUri = it.uri
                    this.fromTextField.text = it.name
                    this.fromTextField.toolTipText = it.description
                }
            }
        }
        val toUriSearchButton = JButton(searchIcon)
        toUriSearchButton.toolTipText = searchButtonTooltipText
        toUriSearchButton.addActionListener {
            val lookupController = BGNodeLookupController(serviceManager, this) {
                if (it != null) {
                    this.toUri = it.uri
                    this.toTextField.text = it.name
                    this.toTextField.toolTipText = it.description
                }
            }
        }
        val swapIcon = ImageIcon(this.javaClass.classLoader.getResource("swap.png"))
        val swapButton = JButton(swapIcon)
        swapButton.addActionListener {
            swapToAndFromParameters()
        }
        fromTextField.isEnabled = false
        toTextField.isEnabled = false



        /*
        fromSearchBox.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                fromSearchBox.toolTipText = getLabelForURI(fromSearchBox.text)
            }

            override fun removeUpdate(e: DocumentEvent) {
                fromSearchBox.toolTipText = getLabelForURI(fromSearchBox.text)
            }

            override fun changedUpdate(e: DocumentEvent) {
                fromSearchBox.toolTipText = getLabelForURI(fromSearchBox.text)
            }})

        toSearchBox.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                toSearchBox.toolTipText = getLabelForURI(toSearchBox.text)
            }

            override fun removeUpdate(e: DocumentEvent) {
                toSearchBox.toolTipText = getLabelForURI(toSearchBox.text)
            }

            override fun changedUpdate(e: DocumentEvent) {
                toSearchBox.toolTipText = getLabelForURI(toSearchBox.text)
            }})
         */

        this.add(fromComboBox)
        this.add(fromTypeComboBox)
        this.add(fromTextField)
        this.add(fromUriSearchButton)
        this.add(relationTypeComboBox)
        this.add(toComboBox)
        this.add(toTypeComboBox)
        this.add(toTextField)
        this.add(toUriSearchButton)
        this.add(swapButton)
    }

    private fun swapToAndFromParameters() {
        val tmpFromUri = fromUri
        val tmpToUri = toUri
        val tmpFromItem = fromComboBox.selectedItem
        val tmpToItem = toComboBox.selectedItem

        toComboBox.selectedItem = tmpFromItem
        fromComboBox.selectedItem = tmpToItem
        fromUri = tmpToUri
        toUri = tmpFromUri
    }

    /*
    private fun getLabelForURI(uri: String): String {
        val node = serviceManager.server.searchForExistingNode(uri)
        node?.name?.let {
            return it
        }
        return ""
    }*/

    private fun updateLabelAndDescriptionForField(textField: JTextField, uri: String) {
        val node = serviceManager.server.searchForExistingNode(uri)
        textField.text = node?.name ?: ""
        textField.toolTipText = node?.description ?: ""
    }

    var fromUri: String?
        get() = {
            val selectedItem = fromComboBox.selectedItem as String
            if (selectedItem == Constants.BG_QUERYBUILDER_ENTITY_LABEL) {
                /*
                if (this.fromSearchBox.text.length == 0) {
                    null
                } else {
                    "<" + this.fromSearchBox.text + ">"
                }*/
                this.currentFromUri
                } else {
                "?"+selectedItem
            }
        }()
        set(value) {
            //this.fromSearchBox.text = value
            this.currentFromUri = value

            updateLabelAndDescriptionForField(fromTextField, value ?: "")
        }
    var toUri: String?
        get() = {
            val selectedItem = toComboBox.selectedItem as String
            if (selectedItem == Constants.BG_QUERYBUILDER_ENTITY_LABEL) {
                /*
                if (this.toSearchBox.text.length == 0) {
                    null
                } else {
                "<"+this.toSearchBox.text+">"
                }*/
                this.currentToUri
            } else {
                "?"+selectedItem
            }
        }()
        set(value) {
            //this.toSearchBox.text = value
            this.currentToUri = value
            toTextField.text
            updateLabelAndDescriptionForField(toTextField, value ?: "")
        }
    val relationType: String?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? String
        }()
}