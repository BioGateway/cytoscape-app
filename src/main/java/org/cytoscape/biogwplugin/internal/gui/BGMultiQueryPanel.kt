package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGSPARQLParser
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class BGMultiQueryLine(val serviceManager: BGServiceManager, val fromTextField: JTextField, val relationTypeComboBox: JComboBox<String>, val toTextField: JTextField, val variableManager: BGQueryVariableManager): JPanel() {

    val searchButtonTooltipText = "Search for entity URIs."
    val variablesTooltipText = "Choose URI to specify an entity, or pick a variable letter to find matching entities."

    val fromComboBox: JComboBox<String>
    val toComboBox: JComboBox<String>

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
        fromTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                fromTextField.toolTipText = getLabelForURI(fromTextField.text)
            }

            override fun removeUpdate(e: DocumentEvent) {
                fromTextField.toolTipText = getLabelForURI(fromTextField.text)
            }

            override fun changedUpdate(e: DocumentEvent) {
                fromTextField.toolTipText = getLabelForURI(fromTextField.text)
            }})

        toTextField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                toTextField.toolTipText = getLabelForURI(toTextField.text)
            }

            override fun removeUpdate(e: DocumentEvent) {
                toTextField.toolTipText = getLabelForURI(toTextField.text)
            }

            override fun changedUpdate(e: DocumentEvent) {
                toTextField.toolTipText = getLabelForURI(toTextField.text)
            }})
         */

        this.add(fromComboBox)
        this.add(fromTextField)
        this.add(fromUriSearchButton)
        this.add(relationTypeComboBox)
        this.add(toComboBox)
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
                if (this.fromTextField.text.length == 0) {
                    null
                } else {
                    "<" + this.fromTextField.text + ">"
                }*/
                this.currentFromUri
                } else {
                "?"+selectedItem
            }
        }()
        set(value) {
            //this.fromTextField.text = value
            this.currentFromUri = value

            updateLabelAndDescriptionForField(fromTextField, value ?: "")
        }
    var toUri: String?
        get() = {
            val selectedItem = toComboBox.selectedItem as String
            if (selectedItem == Constants.BG_QUERYBUILDER_ENTITY_LABEL) {
                /*
                if (this.toTextField.text.length == 0) {
                    null
                } else {
                "<"+this.toTextField.text+">"
                }*/
                this.currentToUri
            } else {
                "?"+selectedItem
            }
        }()
        set(value) {
            //this.toTextField.text = value
            this.currentToUri = value
            toTextField.text
            updateLabelAndDescriptionForField(toTextField, value ?: "")
        }
    val relationType: String?
        get() = {
            this.relationTypeComboBox.model.selectedItem as? String
        }()
}

class BGQueryVariableManager {
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

    private fun getUsedVariables(): Array<String> {
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

    private fun updateComboBoxModels() {
        val comboBoxes = usedVariables.keys + URIcomboBoxes
        for (comboBox in comboBoxes) {
            val model = comboBox.model as DefaultComboBoxModel<String>
            val selected = model.selectedItem
            val lastIndex = model.size -1
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
        var shownVariables = arrayOf(Constants.BG_QUERYBUILDER_ENTITY_LABEL) + usedVariables
        nextFreeChar.let {
            shownVariables += it
        }
        return shownVariables
    }
}

class BGMultiQueryPanel(val serviceManager: BGServiceManager): JPanel() {

    val deleteButtonTooltipText = "Delete this row."

    val variableManager = BGQueryVariableManager()
    val relationTypes = serviceManager.cache.relationTypeDescriptions

    init {
        layout = FlowLayout()
    }

    var queryLines = ArrayList<BGMultiQueryLine>()


    private fun createQueryLine(): BGMultiQueryLine {
        val fromField = JTextField()
        //fromField.preferredSize = Dimension(290, Utility.getJTextFieldHeight())
        fromField.columns = Constants.BG_QUERY_BUILDER_URI_FIELD_COLUMNS
        val toField = JTextField()
        //toField.preferredSize = Dimension(290, Utility.getJTextFieldHeight())
        toField.columns = Constants.BG_QUERY_BUILDER_URI_FIELD_COLUMNS

        val relationTypeBox = JComboBox(relationTypes.keys.toTypedArray())
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
        return queryLine
    }

    fun addQueryLine(): BGMultiQueryLine {
        val queryLine = createQueryLine()
        queryLines.add(queryLine)
        this.add(queryLine)
        return queryLine
    }

    private fun addQueryLine(graph: BGSPARQLParser.BGQueryGraph): BGMultiQueryLine {
        val queryLine = createQueryLine()

        when (graph.from.type) {
            BGSPARQLParser.BGVariableType.URI -> {
                queryLine.fromUri = graph.from.value
                queryLine.fromComboBox.selectedItem = Constants.BG_QUERYBUILDER_ENTITY_LABEL
            }
            BGSPARQLParser.BGVariableType.Variable -> {
                queryLine.fromComboBox.selectedItem = graph.from.value
            }
            BGSPARQLParser.BGVariableType.INVALID -> throw Exception("Unable to parse invalid values!")
        }

        if (graph.relation.type != BGSPARQLParser.BGVariableType.URI) throw Exception("Relation type cannot be a variable!")

        val relationIdentifier = Utility.createRelationTypeIdentifier(graph.relation.value, graph.graph.value)
        val relationType = serviceManager.cache.relationTypeMap.get(relationIdentifier) ?: serviceManager.cache.getRelationTypesForURI(graph.relation.value)?.first()
        if (relationType == null){
            throw Exception("Relation name not found!")
        }

        queryLine.relationTypeComboBox.selectedItem = relationType.description

        when (graph.to.type) {
            BGSPARQLParser.BGVariableType.URI -> {
                queryLine.toUri = graph.to.value
                queryLine.toComboBox.selectedItem = Constants.BG_QUERYBUILDER_ENTITY_LABEL
            }
            BGSPARQLParser.BGVariableType.Variable -> {
                queryLine.toComboBox.selectedItem = graph.to.value
            }
            BGSPARQLParser.BGVariableType.INVALID -> throw Exception("Unable to parse invalid values!")
        }

        queryLines.add(queryLine)
        this.add(queryLine)
        return queryLine
    }

    private fun removeAllQueryLines() {
        for (line in queryLines) {
            this.remove(line)
        }
        queryLines.clear()
        this.topLevelAncestor.repaint()
    }

    fun loadQueryGraphs(queryGraphs: Collection<BGSPARQLParser.BGQueryGraph>) {
        removeAllQueryLines()
        for (graph in queryGraphs) {
            addQueryLine(graph)
        }
    }

    fun addMultiQueryWithURIs(uris: Collection<String>) {
        removeAllQueryLines()
        for (uri in uris) {
            val line = addQueryLine()
            line.fromUri = uri
        }
    }

    private fun removeQueryLine(queryLine: BGMultiQueryLine) {
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
        val queryComponents = generateReturnValuesAndGraphQueries()

        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT DISTINCT " + queryComponents.first + "\n" +
                "WHERE {\n" +
                queryComponents.second +
                "}"

        return query
    }

    fun generateSPARQLCountQuery(): String {
        val graphQueries = generateReturnValuesAndGraphQueries().second
        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT COUNT (*) \n" +
                "WHERE {\n" +
                graphQueries +
                "}"

        return query
    }




    private fun generateReturnValuesAndGraphQueries(): Pair<String, String> {
        var returnValues = ""
        var graphQueries = ""

        var nodeNames = HashSet<String>()

        var numberOfGraphQueries = 0

        for (line in queryLines) {
            val fromUri = line.fromUri ?: throw Exception("Invalid From URI!")
            var relationType = line.relationType?.let { relationTypes.get(it) } ?: throw Exception("Invalid Relation Type!")
            val toUri = line.toUri ?: throw Exception("Invalid To URI!")
            val fromRDFUri = getRDFURI(fromUri)
            val toRDFUri = getRDFURI(toUri)

            val fromName = "?name_"+getSafeString(fromUri)
            val toName = "?name_"+getSafeString(toUri)

            val graphName = relationType.defaultGraphName ?: generateGraphName(numberOfGraphQueries, relationType)

            returnValues += fromRDFUri+" as ?"+getSafeString(fromUri)+numberOfGraphQueries+" <"+graphName+"> <"+relationType.uri+"> "+toRDFUri+" as ?"+getSafeString(toUri)+numberOfGraphQueries+" "
            graphQueries += generateSparqlGraph(numberOfGraphQueries, fromRDFUri, relationType, toRDFUri)
            nodeNames.add(fromRDFUri)
            nodeNames.add(toRDFUri)
            numberOfGraphQueries += 1
        }
        return Pair(returnValues, graphQueries)
    }

    private fun getRDFURI(uri: String): String {
        return when (uri.startsWith("?")) {
            true -> uri
            false -> "<"+uri+">"
        }
    }

    private fun getSafeString(uri: String): String {
        return when (uri.startsWith("?")) {
            true -> uri.removePrefix("?")
            false -> {
                if (uri.startsWith("http://")) {
                    uri.replace("<", "").replace(">", "").replace("http://", "").replace("/", "_").replace(".", "_").replace("-", "_")
                } else {
                    throw Exception("Invalid from URI value.")
                }
            }
        }
    }

    private fun generateGraphName(graphNumber: Int, relation: BGRelationType): String {
        var graphName = "?graph"+graphNumber

        relation.defaultGraphName?.let {
            if (it.length > 0) {
                graphName = "<"+it+">"
            }
        }
        return graphName
    }


    private fun generateSparqlGraph(graphNumber: Int, first: String, relation: BGRelationType, second: String): String {

        var graphName = generateGraphName(graphNumber, relation)

        return "GRAPH "+graphName+" {\n" +
                first+" "+relation.sparqlIRI+" "+second+" .\n" +
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