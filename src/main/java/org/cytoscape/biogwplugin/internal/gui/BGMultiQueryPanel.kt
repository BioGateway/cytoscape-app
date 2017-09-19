package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*


class BGMultiQueryLine(val serviceManager: BGServiceManager, val fromTextField: JTextField, val relationTypeComboBox: JComboBox<String>, val toTextField: JTextField, val variableManager: BGQueryVariableManager): JPanel() {

    val searchButtonTooltipText = "Search for entity URIs."
    val variablesTooltipText = "Choose URI to specify an entity, or pick a variable letter to find matching entities."

    val fromComboBox: JComboBox<String>
    val toComboBox: JComboBox<String>

    fun updateComboBox(comboBox: JComboBox<String>, textField: JTextField) {
        val selectedVariable = comboBox.model.selectedItem as String
        if (selectedVariable == "URI:") {
            // Make sure that the old variable is freed up.
            variableManager.unRegisterUseOfVariableForComponent(comboBox)
            variableManager.URIcomboBoxes.add(comboBox)
            textField.isEnabled = true
        } else {
            variableManager.registerUseOfVariableForComponent(selectedVariable, comboBox)
            variableManager.URIcomboBoxes.remove(comboBox)
            textField.isEnabled = false
        }
    }

    init {
        this.layout = FlowLayout()

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

        val searchIcon = ImageIcon(this.javaClass.classLoader.getResource("search.png"))
        val fromUriSearchButton = JButton(searchIcon)
        fromUriSearchButton.toolTipText = searchButtonTooltipText
        //fromUriSearchButton.preferredSize = Dimension(20, 20)
        fromUriSearchButton.addActionListener {
            val lookupController = BGURILookupController(serviceManager, this) {
                if (it != null) {
                    this.fromUri = it.uri
                    this.fromTextField.toolTipText = it.description
                }
            }
        }
        val toUriSearchButton = JButton(searchIcon)
        toUriSearchButton.toolTipText = searchButtonTooltipText
        //toUriSearchButton.preferredSize = Dimension(20, 20)
        toUriSearchButton.addActionListener {
            val lookupController = BGURILookupController(serviceManager, this) {
                if (it != null) {
                    this.toUri = it.uri
                    this.toTextField.toolTipText = it.description
                }
            }
        }

        this.add(fromComboBox)
        this.add(fromTextField)
        this.add(fromUriSearchButton)
        this.add(relationTypeComboBox)
        this.add(toComboBox)
        this.add(toTextField)
        this.add(toUriSearchButton)
    }

    var fromUri: String?
        get() = {
            val selectedItem = fromComboBox.selectedItem as String
            if (selectedItem == "URI:") {
                if (this.fromTextField.text.length == 0) {
                    null
                } else {
                    "<" + this.fromTextField.text + ">"
                }
                } else {
                "?"+selectedItem
            }
        }()
        set(value) {
            this.fromTextField.text = value
        }
    var toUri: String?
        get() = {
            val selectedItem = toComboBox.selectedItem as String
            if (selectedItem == "URI:") {
                if (this.toTextField.text.length == 0) {
                    null
                } else {
                "<"+this.toTextField.text+">"
                }
            } else {
                "?"+selectedItem
            }
        }()
        set(value) {
            this.toTextField.text = value
        }
    val relationType: String?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? String
        }()
}

class BGQueryVariableManager() {
    val possibleVariables = "ABCDEFGHIJKLMNOPQRST".toCharArray().map { it.toString() }
    var usedVariables = HashMap<JComboBox<String>, String>()
    var URIcomboBoxes = HashSet<JComboBox<String>>()
    var previouslyAssignedVariable: String = "A"

    fun getNextFreeVariable(): String? {
        for (variable in possibleVariables) {
            if (!usedVariables.values.contains(variable)) {
                // The variable is not taken by any component!
                return variable
            }
        }
        return null
    }

    fun getUsedVariables(): Array<String> {
        return usedVariables.values.toHashSet().sorted().toTypedArray()
    }

    fun unRegisterUseOfVariableForComponent(jComboBox: JComboBox<String>) {
        usedVariables.remove(jComboBox)
        updateComboBoxModels()
    }

    fun registerUseOfVariableForComponent(variable: String, jComboBox: JComboBox<String>) {
        usedVariables[jComboBox] = variable
        previouslyAssignedVariable = variable

        updateComboBoxModels()
    }

    fun updateComboBoxModels() {
        val comboBoxes = usedVariables.keys + URIcomboBoxes
        for (comboBox in comboBoxes) {
            val model = comboBox.model as DefaultComboBoxModel<String>
            val selected = model.selectedItem
            val lastIndex = model.getSize()-1
            var containsNextVariable = false
            if (lastIndex < 1) {
                return
            }

            for (i in lastIndex.downTo(1)) {
                // Iterating backwards because we are deleting elements.
                val element = model.getElementAt(i)
                if (element != selected && !usedVariables.values.contains(element) && element != getNextFreeVariable()) {
                    model.removeElementAt(i)
                }
                if (element == getNextFreeVariable()) {
                    containsNextVariable = true
                }
            }
            if (!containsNextVariable) {
                model.addElement(getNextFreeVariable())
            }
        }
    }

    fun getShownVariables(): Array<String> {
        var usedVariables = getUsedVariables().map { it.toString() }.toTypedArray()
        var nextFreeChar = getNextFreeVariable().toString()
        var shownVariables = arrayOf("URI:") + usedVariables
        nextFreeChar?.let {
            shownVariables += it
        }
        return shownVariables
    }
}

class BGMultiQueryPanel(val serviceManager: BGServiceManager): JPanel() {

    val deleteButtonTooltipText = "Delete this row."

    val variableManager = BGQueryVariableManager()

    init {
        layout = FlowLayout()
    }

    var queryLines = ArrayList<BGMultiQueryLine>()

    fun addQueryLine() {
        val fromField = JTextField()
        fromField.preferredSize = Dimension(290, 20)
        val toField = JTextField()
        toField.preferredSize = Dimension(290, 20)
        val relationTypeArray = serviceManager.cache.relationTypeOrderedList.map { serviceManager.cache.relationTypeMap.get(it) }.map { it?.description }.filter { it != null }.map { it as String }.toTypedArray()
        val relationTypeBox = JComboBox(relationTypeArray)
        val queryLine = BGMultiQueryLine(serviceManager, fromField, relationTypeBox, toField, variableManager)

        val deleteIcon = ImageIcon(this.javaClass.classLoader.getResource("delete.png"))
        val deleteButton = JButton(deleteIcon)
        deleteButton.addActionListener {
            if (queryLines.count() > 1) {
                removeQueryLine(queryLine) // Retain loop?
                queryLine.remove(deleteButton)
            }
        }
        deleteButton.toolTipText = deleteButtonTooltipText
        queryLine.add(deleteButton)
        queryLines.add(queryLine)
        this.add(queryLine)
    }

    fun removeQueryLine(queryLine: BGMultiQueryLine) {
        queryLines.remove(queryLine)
        variableManager.unRegisterUseOfVariableForComponent(queryLine.fromComboBox)
        variableManager.URIcomboBoxes.remove(queryLine.fromComboBox)
        variableManager.unRegisterUseOfVariableForComponent(queryLine.toComboBox)
        variableManager.URIcomboBoxes.remove(queryLine.toComboBox)

        this.remove(queryLine)
        this.repaint()
        this.topLevelAncestor.repaint()
    }

    fun generateSPARQLQuery(): String {
        var returnValues = ""
        var graphQueries = ""

        var nodeNames = HashSet<String>()

        var numberOfGraphQueries = 0

        for (line in queryLines) {
            val fromUri = line.fromUri ?: throw Exception("Invalid From URI!")
            var relationUri = line.relationType?.let { serviceManager.cache.getRelationUriForName(it) } ?: throw Exception("Invalid Relation Type!")
            val relationType = serviceManager.server.cache.relationTypeMap.get(relationUri) ?: throw Exception("Unknown relation type!")
            val toUri = line.toUri ?: throw Exception("Invalid To URI!")

            val fromName = "?name_"+getSafeString(fromUri)
            val toName = "?name_"+getSafeString(toUri)

            returnValues += fromUri+" as ?"+getSafeString(fromUri)+numberOfGraphQueries+" <"+relationUri+"> "+toUri+" as ?"+getSafeString(toUri)+numberOfGraphQueries+" "

            //returnValues += fromUri+" as ?"+getSafeString(fromUri)+numberOfGraphQueries+" "+fromName+" as "+fromName+numberOfGraphQueries+" <"+relationUri+"> "+toUri+" as ?"+getSafeString(toUri)+numberOfGraphQueries+" "+toName+" as "+toName+numberOfGraphQueries+" "
            graphQueries += generateSparqlGraph(numberOfGraphQueries, fromUri, relationType, toUri)
            nodeNames.add(fromUri)
            nodeNames.add(toUri)
            numberOfGraphQueries += 1
        }

        val nameQueries = generateSparqlNameGraphs(nodeNames)

        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT DISTINCT " + returnValues + "\n" +
                "WHERE {\n" +
                graphQueries +
                //nameQueries +
                "}"

        return query
    }

    private fun getSafeString(uri: String): String {
        return when (uri.startsWith("?")) {
            true -> uri.removePrefix("?")
            false -> {
                if (uri.startsWith("<http://")) {
                    uri.replace("<", "").replace(">", "").replace("http://", "").replace("/", "_").replace(".", "_")
                } else {
                    throw Exception("Invalid from URI value.")
                }
            }
        }
    }


    private fun generateSparqlGraph(graphNumber: Int, first: String, relation: BGRelationType, second: String): String {

        var graphName = "?graph"+graphNumber

        relation.defaultGraphName?.let {
            if (it.length > 0) {
                graphName = "<"+it+">"
            }
        }


        return "GRAPH "+graphName+" {\n" +
                first+" <"+relation.uri+"> "+second+" .\n" +
                "}\n"
    }

    private fun generateSparqlNameGraphs(nodeUris: Set<String>): String {
        var nameQueryLines = ""
        for (nodeUri in nodeUris) {
            nameQueryLines += nodeUri+" skos:prefLabel ?name_"+getSafeString(nodeUri)+" .\n"
        }
        return nameQueryLines
    }
}