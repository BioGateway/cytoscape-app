package eu.biogateway.app.internal.gui.multiquery

import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.gui.BGColorComboBoxRenderer
import eu.biogateway.app.internal.gui.BGColorableText
import eu.biogateway.app.internal.model.*
import eu.biogateway.app.internal.parser.BGQueryOptions
import eu.biogateway.app.internal.parser.BGSPARQLParser
import eu.biogateway.app.internal.util.Constants
import eu.biogateway.app.internal.util.Utility
import eu.biogateway.app.internal.util.setChildFontSize
import eu.biogateway.app.internal.util.setFontSize
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*


class BGMultiQueryPanel(val constraintPanel: BGQueryConstraintPanel, val uniqueSetsCheckBox: JCheckBox): JPanel() {

    val deleteButtonTooltipText = "Delete this row."

    val variableManager = BGQueryVariableManager()
    val relationTypes = BGServiceManager.config.relationTypeDescriptions

    init {
        // layout = BoxLayout(this, BoxLayout.Y_AXIS)
        layout = GridBagLayout()
    }

    var queryLines = ArrayList<BGMultiQueryAutocompleteLine>()


    private fun createQueryLine(): BGMultiQueryAutocompleteLine {
        val fromField = JTextField()
        fromField.columns = Constants.BG_QUERY_BUILDER_URI_FIELD_COLUMNS
        val toField = JTextField()
        toField.columns = Constants.BG_QUERY_BUILDER_URI_FIELD_COLUMNS

        val relationTypeBox = JComboBox<BGRelationType>(relationTypes.values.toTypedArray())
        relationTypeBox.renderer = BGColorComboBoxRenderer(relationTypeBox as JComboBox<BGColorableText>)

        //val queryLine = BGMultiQueryLine(serviceManager, fromField, relationTypeBox, toField, variableManager)

        val queryLine = BGMultiQueryAutocompleteLine(relationTypeBox, variableManager)

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

        val fontSize = BGServiceManager.config.defaultFontSize;
        relationTypeBox.setFontSize(fontSize)
        queryLine.setChildFontSize(fontSize)

        return queryLine
    }

    private fun addPanel(panel: JPanel) {
        val gbc = GridBagConstraints()
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.anchor = GridBagConstraints.FIRST_LINE_START
        gbc.fill = GridBagConstraints.HORIZONTAL
        this.add(panel, gbc)
    }

    fun addQueryLine(): BGMultiQueryAutocompleteLine {
        val queryLine = createQueryLine()
        queryLines.add(queryLine)
        addPanel(queryLine)
        return queryLine
    }

    private fun addQueryLine(graph: BGSPARQLParser.BGQueryGraph): BGMultiQueryAutocompleteLine {
        val queryLine = createQueryLine()

        if (graph.relation.type != BGSPARQLParser.BGVariableType.URI) throw Exception("Relation type cannot be a variable!")

        val relationIdentifier = Utility.createRelationTypeIdentifier(graph.relation.value, graph.graph.value)
        val relationType = BGServiceManager.config.relationTypeMap.get(relationIdentifier) ?: BGServiceManager.config.getRelationTypesForURI(graph.relation.value)?.first()
        if (relationType == null){
            throw Exception("Relation name not found!")
        }

        queryLine.relationTypeComboBox.selectedItem = relationType
        when (graph.from.type) {
            BGSPARQLParser.BGVariableType.URI -> {
                queryLine.fromUri = graph.from.value
                queryLine.fromComboBox.selectedItem = BGQueryVariable.Entity
                queryLine.fromTypeComboBox.selectedItem = BGNode(graph.from.value).type
            }
            BGSPARQLParser.BGVariableType.Variable -> {
               variableManager.setVariableToComboBox(graph.from.value, queryLine.fromComboBox)
            }
            BGSPARQLParser.BGVariableType.INVALID -> throw Exception("Unable to parse invalid values!")
        }

        when (graph.to.type) {
            BGSPARQLParser.BGVariableType.URI -> {
                queryLine.toUri = graph.to.value
                queryLine.toComboBox.selectedItem = BGQueryVariable.Entity
                queryLine.toTypeComboBox.selectedItem = BGNode(graph.to.value).type

            }
            BGSPARQLParser.BGVariableType.Variable -> {
                variableManager.setVariableToComboBox(graph.to.value, queryLine.toComboBox)
            }
            BGSPARQLParser.BGVariableType.INVALID -> throw Exception("Unable to parse invalid values!")
        }


        queryLines.add(queryLine)
        addPanel(queryLine)
        return queryLine
    }

    private fun removeAllQueryLines() {
        // Make a copy to avoid ConcurrentModificationException while deleting.
        val queryLinesCopy = this.queryLines.toTypedArray().copyOf()
        for (line in queryLinesCopy) {
            this.removeQueryLine(line)
        }

        queryLines.clear()
        this.topLevelAncestor.repaint()
    }

    fun loadQueryGraphs(queryGraphs: Triple<Collection<BGSPARQLParser.BGQueryGraph>, List<BGSPARQLParser.BGGraphConstraint>?, BGQueryOptions>) {
        removeAllQueryLines()
        for (graph in queryGraphs.first) {
            addQueryLine(graph)
        }
        queryGraphs.second?.let {
            for (constraint in it) {
                // TODO: Should set all other constraints to false.
                constraintPanel.setConstraintValue(constraint.id, true, constraint.value)
            }
        }
        uniqueSetsCheckBox.isSelected = !queryGraphs.third.selfLoopsEnabled
    }

    fun addMultiQueryWithURIs(uris: Collection<String>) {
        removeAllQueryLines()
        for (uri in uris) {
            val line = addQueryLine()
            //line.relationTypeComboBox.selectedItem = null
            line.fromTypeComboBox.selectedItem = BGNode(uri).type
            line.fromComboBox.selectedItem = BGQueryVariable.Entity
            line.fromUri = uri
        }
    }

    private fun removeQueryLine(queryLine: BGMultiQueryAutocompleteLine) {
        queryLines.remove(queryLine)
        variableManager.unRegisterUseOfVariableForComponent(queryLine.fromComboBox)
        variableManager.URIcomboBoxes.remove(queryLine.fromComboBox)
        variableManager.unRegisterUseOfVariableForComponent(queryLine.toComboBox)
        variableManager.URIcomboBoxes.remove(queryLine.toComboBox)

        this.remove(queryLine)
        this.repaint()
        this.topLevelAncestor.repaint()
    }

    fun validateNodeTypeConsistency(): String? {
        val nodeVariableTypes = HashMap<BGQueryVariable, BGNodeType>()

        fun checkLineParameter(comboBox: JComboBox<BGQueryVariable>, nodeType: BGNodeType?): String? {
            if (nodeType == null) return null
            val variable = comboBox.selectedItem as? BGQueryVariable ?: return null
            //if (variable.value == Constants.BG_QUERYBUILDER_ENTITY_LABEL) return null
            if (variable == BGQueryVariable.Entity) return null
            val oldType = nodeVariableTypes.get(variable)
            oldType?.let {
                if (it != nodeType) return "$variable is expected to be both $it and $nodeType"
            }
            nodeVariableTypes.put(variable, nodeType)
            return null
        }

        for (line in queryLines) {
            checkLineParameter(line.fromComboBox, line.fromTypeComboBox.selectedItem as? BGNodeType)?.let {
                return "Type inconcistency: Variable $it"
            }
            checkLineParameter(line.toComboBox, line.toTypeComboBox.selectedItem as? BGNodeType)?.let {
                return "Type inconcistency: Variable $it"
            }
        }
        return null
    }

    fun generateSPARQLQuery(): String {
        val queryComponents = generateReturnValuesAndGraphQueries()
        var queryWildcards = variableManager.usedVariables.values.toHashSet()
                .sortedBy { it.name }
                .map { "?"+it.value }
                .fold("") { acc, s -> acc+" "+s }
        if (queryWildcards.isEmpty()) {
            queryWildcards = "<placeholder>"
        }
        val header = ""
        val query = "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT " + queryComponents.first + "\n" +
                "WHERE {\n" +
                "{ SELECT DISTINCT "+queryWildcards+"\n" +
                "WHERE {\n" +
                 queryComponents.second +
                "}}\n\n"+
                queryComponents.third + "}"

        return header+query
    }

    fun generateSimplifiedSPARQL(): String {
        val queryComponents = generateReturnValuesAndGraphQueries()
        var queryWildcards = variableManager.usedVariables.values.toHashSet()
                .sortedBy { it.name }
                .map { "?"+it.value }
                .fold("") { acc, s -> acc+" "+s }
        if (queryWildcards.isEmpty()) {
            queryWildcards = "<placeholder>"
        }
        val query = "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT "+queryWildcards+"\n" +
                "WHERE {\n" +
                queryComponents.second +
                "}"
        return query
    }

    fun generateSPARQLCountQuery(): String {
        val graphQueries = generateReturnValuesAndGraphQueries()
        val query = "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT COUNT (*) \n" +
                "WHERE {\n" +
                graphQueries.second +
                //graphQueries.third +
                "}"

        return query
    }






    private fun generateReturnValuesAndGraphQueries(): Triple<String, String, String> {
        var returnValues = ""
        var graphQueries = ""

        var triples = HashSet<Triple<String, BGRelationType, String>>()

        var nodeNames = HashSet<String>()

        var numberOfGraphQueries = 0
        var numberOfCurrentNetworkSetUses = 0

        for (line in queryLines) {
            val fromUri = line.fromUri ?: throw Exception("Invalid From URI!")
            //var relationType = line.relationType?.let { relationTypes.get(it) } ?: throw Exception("Invalid Relation Type!")
            val relationType = line.relationType ?: throw Exception("Invalid Relation Type!")
            val toUri = line.toUri ?: throw Exception("Invalid To URI!")
            val fromRDFUri = getRDFURI(fromUri)
            val toRDFUri = getRDFURI(toUri)
            if (fromUri == "?current_network") numberOfCurrentNetworkSetUses += 1
            if (toUri == "?current_network") numberOfCurrentNetworkSetUses += 1

            val fromName = "?name_"+getSafeString(fromUri)
            val toName = "?name_"+getSafeString(toUri)

            val graphName = relationType.defaultGraphURI ?: generateGraphName(numberOfGraphQueries, relationType)

            returnValues += fromRDFUri+" as ?"+getSafeString(fromUri)+numberOfGraphQueries+" <"+graphName+"> <"+relationType.uri+"> "+toRDFUri+" as ?"+getSafeString(toUri)+numberOfGraphQueries+" "
            graphQueries += generateSparqlGraph(numberOfGraphQueries, fromRDFUri, relationType, toRDFUri)
            triples.add(Triple(fromRDFUri, relationType, toRDFUri))
            nodeNames.add(fromRDFUri)
            nodeNames.add(toRDFUri)
            numberOfGraphQueries += 1
        }

        var currentNetworkSetFilter = ""
        if (numberOfCurrentNetworkSetUses > 0) {
            // 1. Fetch all URIs of the current network.
            val network = BGServiceManager.applicationManager?.currentNetwork
            val allNodeUris = network?.defaultNodeTable?.getColumn(Constants.BG_FIELD_IDENTIFIER_URI)?.getValues(String::class.java)?.map { "<${it}>" }?.reduce { acc, s -> acc + "," +s} ?: ""
            if (allNodeUris.isNotEmpty()) {
                // 2. Append them to a FILTER.
                currentNetworkSetFilter = "FILTER(?current_network IN(" + allNodeUris + "))\n"
            }
        }

        val uniqueSetsFilter = if (uniqueSetsCheckBox.isSelected) { generateUniqueSetsFilter() } else { "\n #enableSelfLoops \n" }

        try {
            val constraintValues = constraintPanel.getConstraintValues()
            BGServiceManager.config.taxonConstraint?.let { constraint ->
                BGQueryConstraint.generateTaxonConstraintValue()?.let { value ->
                    constraintValues[constraint] = value
                }
            }
            val constraints = currentNetworkSetFilter + uniqueSetsFilter + BGQueryConstraint.generateConstraintQueries(constraintValues, triples)

            return Triple(returnValues, graphQueries, constraints)
        } catch (exception: InvalidInputValueException) {
            JOptionPane.showMessageDialog(this, exception.message, "Invalid query constraints", JOptionPane.ERROR_MESSAGE)
            return Triple(returnValues, graphQueries, uniqueSetsFilter)
        }
    }

    private fun generateUniqueSetsFilter(): String {
        val usedVariables = variableManager.usedVariables.values.toHashSet().map { "?"+it.value }.toTypedArray()
        var uniqueVariablesFilter = ""

        for (i in 0..(usedVariables.size-1)) {
            for (j in i..(usedVariables.size-1)) {
                if (j == i) continue
                uniqueVariablesFilter += "\n FILTER("+usedVariables[i]+"!="+usedVariables[j]+")"
            }
        }
        return uniqueVariablesFilter
    }


    fun generateConstraintQueries(triples: Collection<Triple<String, BGRelationType, String>>): String {

        // Graphs are key, then all the queries on the graphs.
        val constraintQueries = HashMap<String, HashSet<String>>()
        fun addToQueries(key: String, sparql: String) {
            if (!constraintQueries.containsKey(key)) {
                constraintQueries[key] = HashSet()
            }
            constraintQueries[key]?.add(sparql)
        }

        val usedVariables = variableManager.usedVariables.values.toHashSet().map { "?"+it.value }.toTypedArray()
        var uniqueVariablesFilter = ""

        for (i in 0..(usedVariables.size-1)) {
            for (j in i..(usedVariables.size-1)) {
                if (j == i) continue
                uniqueVariablesFilter += "\n FILTER("+usedVariables[i]+"!="+usedVariables[j]+")"
            }
        }
        var sourceConstraintCounter = 1
        var sourceConstraintFilters = ""


        for (triple in triples) {
            val graph = triple.second.defaultGraphURI ?: continue
            val pair = BGDatasetSource.generateSourceConstraint(triple.second, triple.first, triple.third, sourceConstraintCounter)
            sourceConstraintCounter++
//            val relevantSources = serviceManager.config.activeSources.filter { it.relationTypes.contains(triple.second) }
//            if (relevantSources.count() == 0) continue
//            val uri = "?sourceConstraint"+sourceConstraintCounter
//            sourceConstraintCounter++
//
//            val filter = "FILTER("+relevantSources.map { uri+"filter = <"+it.uri+">" }.reduce { acc, s -> acc+"||"+s }+")\n"
//            val sparql = uri+" rdf:subject "+triple.first+".\n" +
//                    uri+" rdf:object "+triple.third+" .\n" +
//                    uri+" rdf:predicate <"+triple.second.uri+"> .\n" +
//                    uri+" <http://semanticscience.org/resource/SIO_000253> "+uri+"filter ."

            pair?.let {
                sourceConstraintFilters += it.first
                addToQueries(graph, it.second)
            }

        }

        try {
            val constraintValues = constraintPanel.getConstraintValues()

            var uniqueIdNumber = 1

        for ((constraint, value) in constraintValues) {
            // Skip this if it's disabled.
            if (!value.isEnabled) continue

            for (triple in triples) {
                for (action in constraint.actions) {
                    if (!action.relationTypes.contains(triple.second)) continue
                    val sparql = action.sparqlTemplate
                            .replace("@first", triple.first)
                            .replace("@last", triple.third)
                            .replace("@value", value.stringValue)
                            .replace("@uniqueId", uniqueIdNumber.toString())
                    uniqueIdNumber++

                    if (action.parameter == BGQueryConstraint.ActionParameter.FIRST && triple.first.startsWith("?")) {
                        addToQueries(action.graph, sparql)
                    }
                    if (action.parameter == BGQueryConstraint.ActionParameter.LAST && triple.third.startsWith("?")) {
                        addToQueries(action.graph, sparql)
                    }
                    if (action.parameter == BGQueryConstraint.ActionParameter.BOTH &&
                            (triple.third.startsWith("?") || triple.first.startsWith("?"))) {
                        addToQueries(action.graph, sparql)
                    }
                }
            }
        }


        if (constraintQueries.count() > 0) {
            val constraintHeader = "\n#QueryConstraints:\n" + constraintValues
                    .filter { it.value.isEnabled }
                    .map { "#Constraint: " + it.key.id + "=" + it.value.stringValue + "\n" }
                    .fold("") { acc, s -> acc + s }

            var constraintQueryString = uniqueVariablesFilter+"\n\n"

            for (graph in constraintQueries.keys) {

                val lines = constraintQueries[graph] ?: continue

                constraintQueryString += "\n" +
                        "GRAPH <" + graph + "> { \n"
                for (line in lines) {
                    constraintQueryString += line + "\n"
                }
                constraintQueryString += "}\n"
            }
            return sourceConstraintFilters + constraintHeader + constraintQueryString
        } else return uniqueVariablesFilter
        } catch (exception: InvalidInputValueException) {
            JOptionPane.showMessageDialog(this, exception.message, "Invalid query constraints", JOptionPane.ERROR_MESSAGE)
            return uniqueVariablesFilter
        }
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

        relation.defaultGraphURI?.let {
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