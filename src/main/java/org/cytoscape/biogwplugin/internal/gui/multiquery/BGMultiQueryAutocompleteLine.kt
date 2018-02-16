package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.BGNodeLookupController
import org.cytoscape.biogwplugin.internal.gui.BGQueryVariableManager
import org.cytoscape.biogwplugin.internal.util.Constants
import java.awt.FlowLayout
import javax.swing.*

class BGMultiQueryAutocompleteLine(val serviceManager: BGServiceManager, val relationTypeComboBox: JComboBox<String>, val variableManager: BGQueryVariableManager): JPanel() {

    val searchButtonTooltipText = "Search for entity URIs."
    val variablesTooltipText = "Choose URI to specify an entity, or pick a variable letter to find matching entities."

    val fromComboBox: JComboBox<String>
    val toComboBox: JComboBox<String>

    val fromTypeComboBox: JComboBox<String>
    val toTypeComboBox: JComboBox<String>

    val fromSearchBox: BGAutocompleteComboBox
    val toSearchBox: BGAutocompleteComboBox

    var currentFromUri: String? get() {
        return fromSearchBox.selectedUri
    } set(value) {
        fromSearchBox.selectedUri = value
    }
    var currentToUri: String? get() {
        return toSearchBox.selectedUri
    } set(value) {
        toSearchBox.selectedUri = value
    }

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

        fromComboBox = JComboBox(variableManager.getShownVariables())
        fromComboBox.toolTipText = variablesTooltipText

//        updateComboBox(fromComboBox, fromSearchBox)
//        fromComboBox.addActionListener {
//            updateComboBox(fromComboBox, fromSearchBox)
//        }


        toComboBox = JComboBox(variableManager.getShownVariables())
        toComboBox.toolTipText = variablesTooltipText
//        updateComboBox(toComboBox, toSearchBox)
//        toComboBox.addActionListener {
//            updateComboBox(toComboBox, toSearchBox)
//        }


        val types = arrayOf("Gene", "Protein", "GO-Term", "All")
        toTypeComboBox = JComboBox(types)
        fromTypeComboBox = JComboBox(types)

        fromSearchBox = BGAutocompleteComboBox(fromTypeComboBox, serviceManager.endpoint)
        toSearchBox = BGAutocompleteComboBox(toTypeComboBox, serviceManager.endpoint)


        val searchIcon = ImageIcon(this.javaClass.classLoader.getResource("search.png"))
        val fromUriSearchButton = JButton(searchIcon)
        fromUriSearchButton.toolTipText = searchButtonTooltipText
        fromUriSearchButton.addActionListener {
            val lookupController = BGNodeLookupController(serviceManager, this) {
                if (it != null) {
                    this.fromUri = it.uri
                    it.name?.let {
                        this.fromSearchBox.text = it
                    }
                    this.fromSearchBox.toolTipText = it.description
                }
            }
        }
        val toUriSearchButton = JButton(searchIcon)
        toUriSearchButton.toolTipText = searchButtonTooltipText
        toUriSearchButton.addActionListener {
            val lookupController = BGNodeLookupController(serviceManager, this) {
                if (it != null) {
                    this.toUri = it.uri
                    it.name?.let {
                        this.toSearchBox.text = it
                    }
                    this.toSearchBox.toolTipText = it.description
                }
            }
        }
        val swapIcon = ImageIcon(this.javaClass.classLoader.getResource("swap.png"))
        val swapButton = JButton(swapIcon)
        swapButton.addActionListener {
            swapToAndFromParameters()
        }
        //fromSearchBox.isEnabled = false
        //toSearchBox.isEnabled = false

        this.add(fromComboBox)
        this.add(fromTypeComboBox)
        this.add(fromSearchBox)
        this.add(fromUriSearchButton)
        this.add(relationTypeComboBox)
        this.add(toComboBox)
        this.add(toTypeComboBox)
        this.add(toSearchBox)
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

            //updateLabelAndDescriptionForField(fromSearchBox, value ?: "")
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
            toSearchBox.text
            //updateLabelAndDescriptionForField(toSearchBox, value ?: "")
        }
    val relationType: String?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? String
        }()
}