package eu.biogateway.cytoscape.internal.gui.multiquery

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.gui.BGColorableText
import eu.biogateway.cytoscape.internal.gui.BGNodeLookupController
import eu.biogateway.cytoscape.internal.gui.BGNodeTypeComboBoxRenderer
import eu.biogateway.cytoscape.internal.model.BGNodeTypeNew
import eu.biogateway.cytoscape.internal.model.BGRelationType
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Utility
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class BGMultiQueryAutocompleteLine(val relationTypeComboBox: JComboBox<BGRelationType>, val variableManager: BGQueryVariableManager): JPanel() {

    val searchButtonTooltipText = "Search for entity URIs."
    val variablesTooltipText = "Choose URI to specify an entity, or pick a variable letter to find matching entities."

    val fromComboBox: JComboBox<BGQueryVariable>
    val toComboBox: JComboBox<BGQueryVariable>

    val fromTypeComboBox: JComboBox<BGNodeTypeNew>
    val toTypeComboBox: JComboBox<BGNodeTypeNew>
    val fromTypeBoxRenderer: BGNodeTypeComboBoxRenderer
    val toTypeBoxRenderer: BGNodeTypeComboBoxRenderer

    val fromSearchBox: BGAutocompleteComboBox
    val toSearchBox: BGAutocompleteComboBox



    var currentFromUri: String? get() {
        return fromSearchBox.selectedUri
    } set(value) {
        fromSearchBox.selectedUri = value
        fromSearchBox.getNameForSelectedURI()
    }
    var currentToUri: String? get() {
        return toSearchBox.selectedUri
    } set(value) {
        toSearchBox.selectedUri = value
        toSearchBox.getNameForSelectedURI()
    }

    fun updateComboBoxColor(comboBox: JComboBox<BGColorableText>) {
        val selected = comboBox.selectedItem as? BGColorableText
        selected?.let {
            comboBox.foreground = selected.textColor
        }
    }

    fun updateColorForIncorrectNodeTypes(fromType: BGNodeTypeNew?, toType: BGNodeTypeNew?) {

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

    fun updateComboBox(comboBox: JComboBox<BGQueryVariable>, typeComboBox: JComponent, searchComboBox: JComponent) {
        val selectedVariable = comboBox.model.selectedItem as BGQueryVariable
        if (selectedVariable == BGQueryVariable.Entity) {
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

        val types = BGServiceManager.config.nodeTypes.values.filter { it.autocompleteType != null }.toTypedArray()

        //val types = arrayOf(BGNodeType.Protein, BGNodeType.Gene, BGNodeType.GOTerm, BGNodeType.Taxon, BGNodeType.Disease)
        toTypeComboBox = JComboBox(types)
        fromTypeComboBox = JComboBox(types)
        fromTypeBoxRenderer = BGNodeTypeComboBoxRenderer(fromTypeComboBox)
        toTypeBoxRenderer = BGNodeTypeComboBoxRenderer(toTypeComboBox)

        fromTypeComboBox.renderer = fromTypeBoxRenderer
        toTypeComboBox.renderer = toTypeBoxRenderer

        fromSearchBox = BGAutocompleteComboBox(BGServiceManager.endpoint) {
            (fromTypeComboBox.selectedItem as? BGNodeTypeNew)?.let {
                it
            }
        }
        toSearchBox = BGAutocompleteComboBox(BGServiceManager.endpoint) {
            (toTypeComboBox.selectedItem as? BGNodeTypeNew)?.let {
                it
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
            val lookupController = BGNodeLookupController(this) {
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
            val lookupController = BGNodeLookupController(this) {
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
        toTypeComboBox.selectedItem = tmpFromTypeBoxItem
        fromTypeComboBox.selectedItem = tmpToTypeBoxItem
        fromSearchBox.text = tmpToSearchBoxText
        toSearchBox.text = tmpFromSearchBoxText
        fromUri = tmpToUri
        toUri = tmpFromUri
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
        val node = BGServiceManager.dataModelController.searchForExistingNode(uri)
        textField.text = node?.name ?: ""
        textField.toolTipText = node?.description ?: ""
    }

    var fromUri: String?
        get() = {
            val selectedItem = fromComboBox.selectedItem as BGQueryVariable
            if (selectedItem == BGQueryVariable.Entity) {
                /*
                if (this.fromSearchBox.text.length == 0) {
                    null
                } else {
                    "<" + this.fromSearchBox.text + ">"
                }*/
                this.currentFromUri
            } else {
                "?"+selectedItem.value
            }
        }()
        set(value) {
            //this.fromSearchBox.text = value
            this.currentFromUri = value

            //updateLabelAndDescriptionForField(fromSearchBox, value ?: "")
        }
    var toUri: String?
        get() = {
            val selectedItem = toComboBox.selectedItem as BGQueryVariable
            if (selectedItem == BGQueryVariable.Entity) {
                /*
                if (this.toSearchBox.text.length == 0) {
                    null
                } else {
                "<"+this.toSearchBox.text+">"
                }*/
                this.currentToUri
            } else {
                "?"+selectedItem.value
            }
        }()
        set(value) {
            //this.toSearchBox.text = value
            this.currentToUri = value
            //updateLabelAndDescriptionForField(toSearchBox, value ?: "")
        }
    val relationType: BGRelationType?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? BGRelationType
        }()
}