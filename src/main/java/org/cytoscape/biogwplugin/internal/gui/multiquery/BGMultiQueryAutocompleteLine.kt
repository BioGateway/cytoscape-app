package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.BGColorComboBoxRenderer
import org.cytoscape.biogwplugin.internal.gui.BGColorableText
import org.cytoscape.biogwplugin.internal.gui.BGNodeLookupController
import org.cytoscape.biogwplugin.internal.gui.BGNodeTypeComboBoxRenderer
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.util.Constants
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.*

class BGMultiQueryAutocompleteLine(val serviceManager: BGServiceManager, val relationTypeComboBox: JComboBox<BGRelationType>, val variableManager: BGQueryVariableManager): JPanel() {

    val searchButtonTooltipText = "Search for entity URIs."
    val variablesTooltipText = "Choose URI to specify an entity, or pick a variable letter to find matching entities."

    val fromComboBox: JComboBox<String>
    val toComboBox: JComboBox<String>

    val fromTypeComboBox: JComboBox<BGNodeType>
    val toTypeComboBox: JComboBox<BGNodeType>
    val fromTypeBoxRenderer: BGNodeTypeComboBoxRenderer
    val toTypeBoxRenderer: BGNodeTypeComboBoxRenderer

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

    fun updateComboBoxColor(comboBox: JComboBox<BGColorableText>) {
        val selected = comboBox.selectedItem as? BGColorableText
        selected?.let {
            comboBox.foreground = selected.textColor
        }
    }

    fun updateColorForIncorrectNodeTypes(fromType: BGNodeType?, toType: BGNodeType?) {

        fromType?.let {
            fromTypeBoxRenderer.acceptedNodeTypes = arrayListOf(it)
        }

        toType?.let {
            toTypeBoxRenderer.acceptedNodeTypes = arrayListOf(it)
        }

        if (fromTypeComboBox.selectedItem != fromType) {
            fromTypeComboBox.foreground = Color.RED
        } else {
            fromTypeComboBox.foreground = Color.BLACK
        }
        if (toTypeComboBox.selectedItem != toType) {
            toTypeComboBox.foreground = Color.RED
        } else {
            toTypeComboBox.foreground = Color.BLACK
        }
    }

    fun updateColorForIncorrectNodeTypes() {
        val relationType = relationTypeComboBox.selectedItem as? BGRelationType
        updateColorForIncorrectNodeTypes(relationType?.fromType, relationType?.toType)
    }

    fun updateComboBox(comboBox: JComboBox<String>, typeComboBox: JComponent, searchComboBox: JComponent) {
        val selectedVariable = comboBox.model.selectedItem as String
        if (selectedVariable == Constants.BG_QUERYBUILDER_ENTITY_LABEL) {
            // Make sure that the old variable is freed up.
            variableManager.unRegisterUseOfVariableForComponent(comboBox)
            variableManager.URIcomboBoxes.add(comboBox)
            typeComboBox.isEnabled = true
            searchComboBox.isEnabled = true
        } else {
            variableManager.registerUseOfVariableForComponent(selectedVariable, comboBox)
            variableManager.URIcomboBoxes.remove(comboBox)
            typeComboBox.isEnabled = false
            searchComboBox.isEnabled = false
        }
    }

    fun updateNodeTypesForRelationType() {
        val selectedType = relationTypeComboBox.selectedItem as? BGRelationType
        val fromType = selectedType?.fromType
        val toType = selectedType?.toType
        println(fromType)
        println(toType)
        fromType?.let {
            fromTypeComboBox.selectedItem = it
        }
        toType?.let {
            toTypeComboBox.selectedItem = it
        }
    }

    init {
        this.layout = FlowLayout()

        fromComboBox = JComboBox(variableManager.getShownVariables())
        fromComboBox.toolTipText = variablesTooltipText

        toComboBox = JComboBox(variableManager.getShownVariables())
        toComboBox.toolTipText = variablesTooltipText

        //val types = arrayOf("Protein", "Gene", "GO-Term", "Taxon", "Disease", "All")
        val types = arrayOf(BGNodeType.Protein, BGNodeType.Gene, BGNodeType.GO, BGNodeType.Taxon, BGNodeType.Disease)
        toTypeComboBox = JComboBox(types)
        fromTypeComboBox = JComboBox(types)
        fromTypeBoxRenderer = BGNodeTypeComboBoxRenderer(fromTypeComboBox)
        toTypeBoxRenderer = BGNodeTypeComboBoxRenderer(toTypeComboBox)

        fromTypeComboBox.renderer = fromTypeBoxRenderer
        toTypeComboBox.renderer = toTypeBoxRenderer

        fromSearchBox = BGAutocompleteComboBox(serviceManager.endpoint) {
            (fromTypeComboBox.selectedItem as? BGNodeType)?.let {
                BGNodeType.forName(it.paremeterType)
            }
        }
        toSearchBox = BGAutocompleteComboBox(serviceManager.endpoint) {
            (toTypeComboBox.selectedItem as? BGNodeType)?.let {
                BGNodeType.forName(it.paremeterType)
            }
        }

        updateComboBox(fromComboBox, fromTypeComboBox, fromSearchBox)
        fromComboBox.addActionListener {
            updateComboBox(fromComboBox, fromTypeComboBox, fromSearchBox)
        }
        updateComboBox(toComboBox, toTypeComboBox, toSearchBox)
        toComboBox.addActionListener {
            updateComboBox(toComboBox, toTypeComboBox, toSearchBox)
        }

        relationTypeComboBox.addActionListener {
            updateNodeTypesForRelationType()
            updateComboBoxColor(relationTypeComboBox as JComboBox<BGColorableText>)
        }

        fromTypeComboBox.addActionListener {
            updateColorForIncorrectNodeTypes()
        }

        toTypeComboBox.addActionListener {
            updateColorForIncorrectNodeTypes()
        }

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

        updateNodeTypesForRelationType()
        updateComboBoxColor(relationTypeComboBox as JComboBox<BGColorableText>)
    }

    private fun swapToAndFromParameters() {
        val tmpFromUri = fromUri
        val tmpToUri = toUri
        val tmpFromItem = fromComboBox.selectedItem
        val tmpToItem = toComboBox.selectedItem
        val tmpFromTypeBoxItem = fromTypeComboBox.selectedItem
        val tmpToTypeBoxItem = toTypeComboBox.selectedItem
        val tmpFromSearchBoxText = fromSearchBox.text
        val tmpToSearchBoxText = toSearchBox.text

        toComboBox.selectedItem = tmpFromItem
        fromComboBox.selectedItem = tmpToItem
        fromUri = tmpToUri
        toUri = tmpFromUri
        toTypeComboBox.selectedItem = tmpFromTypeBoxItem
        fromTypeComboBox.selectedItem = tmpToTypeBoxItem
        fromSearchBox.text = tmpToSearchBoxText
        toSearchBox.text = tmpFromSearchBoxText
    }

    /*
    private fun getLabelForURI(uri: String): String {
        val node = serviceManager.dataModelController.searchForExistingNode(uri)
        node?.name?.let {
            return it
        }
        return ""
    }*/

    private fun updateLabelAndDescriptionForField(textField: JTextField, uri: String) {
        val node = serviceManager.dataModelController.searchForExistingNode(uri)
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
    val relationType: BGRelationType?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? BGRelationType
        }()
}