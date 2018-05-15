package eu.biogateway.cytoscape.internal.gui.multiquery

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.gui.BGColorComboBoxRenderer
import eu.biogateway.cytoscape.internal.gui.BGColorableText
import eu.biogateway.cytoscape.internal.model.*
import eu.biogateway.cytoscape.internal.parser.BGSPARQLParser
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Utility
import java.awt.FlowLayout
import javax.swing.*


class BGMultiQueryPanel(val constraintPanel: BGQueryConstraintPanel): JPanel() {

    val deleteButtonTooltipText = "Delete this row."

    val variableManager = BGQueryVariableManager()
    val relationTypes = BGServiceManager.cache.relationTypeDescriptions


    init {
        layout = FlowLayout()
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
        return queryLine
    }

    fun addQueryLine(): BGMultiQueryAutocompleteLine {
        val queryLine = createQueryLine()
        queryLines.add(queryLine)
        this.add(queryLine)
        return queryLine
    }

    private fun addQueryLine(graph: BGSPARQLParser.BGQueryGraph): BGMultiQueryAutocompleteLine {
        val queryLine = createQueryLine()

        if (graph.relation.type != BGSPARQLParser.BGVariableType.URI) throw Exception("Relation type cannot be a variable!")

        val relationIdentifier = Utility.createRelationTypeIdentifier(graph.relation.value, graph.graph.value)
        val relationType = BGServiceManager.cache.relationTypeMap.get(relationIdentifier) ?: BGServiceManager.cache.getRelationTypesForURI(graph.relation.value)?.first()
        if (relationType == null){
            throw Exception("Relation name not found!")
        }

        queryLine.relationTypeComboBox.selectedItem = relationType
        when (graph.from.type) {
            BGSPARQLParser.BGVariableType.URI -> {
                queryLine.fromUri = graph.from.value
                queryLine.fromComboBox.selectedItem = Constants.BG_QUERYBUILDER_ENTITY_LABEL
                queryLine.fromTypeComboBox.selectedItem = BGNode(graph.from.value).type
            }
            BGSPARQLParser.BGVariableType.Variable -> {
                queryLine.fromComboBox.selectedItem = graph.from.value
            }
            BGSPARQLParser.BGVariableType.INVALID -> throw Exception("Unable to parse invalid values!")
        }

        when (graph.to.type) {
            BGSPARQLParser.BGVariableType.URI -> {
                queryLine.toUri = graph.to.value
                queryLine.toComboBox.selectedItem = Constants.BG_QUERYBUILDER_ENTITY_LABEL
                queryLine.toTypeComboBox.selectedItem = BGNode(graph.to.value).type

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
        // Make a copy to avoid ConcurrentModificationException while deleting.
        val queryLinesCopy = this.queryLines.toTypedArray().copyOf()
        for (line in queryLinesCopy) {
            this.removeQueryLine(line)
        }

        queryLines.clear()
        this.topLevelAncestor.repaint()
    }

    fun loadQueryGraphs(queryGraphs: Pair<Collection<BGSPARQLParser.BGQueryGraph>, List<BGSPARQLParser.BGGraphConstraint>?>) {
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
    }

    fun addMultiQueryWithURIs(uris: Collection<String>) {
        removeAllQueryLines()
        for (uri in uris) {
            val line = addQueryLine()
            //line.relationTypeComboBox.selectedItem = null
            line.fromTypeComboBox.selectedItem = BGNode(uri).type
            line.fromComboBox.selectedItem = Constants.BG_QUERYBUILDER_ENTITY_LABEL
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
        val nodeVariableTypes = HashMap<String, BGNodeType>()

        fun checkLineParameter(comboBox: JComboBox<String>, nodeType: BGNodeType?): String? {
            if (nodeType == null) return null
            if (comboBox.selectedItem == Constants.BG_QUERYBUILDER_ENTITY_LABEL) return null
            val variable = comboBox.selectedItem as? String ?: return null
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
                .sorted()
                .map { "?"+it }
                .fold("") { acc, s -> acc+" "+s }
        if (queryWildcards.isEmpty()) {
            queryWildcards = "<placeholder>"
        }
        val header = "#QUERY <http://www.semantic-systems-biology.org/biogateway/endpoint>\n"
        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT DISTINCT " + queryComponents.first + "\n" +
                "WHERE {\n" +
                "{ SELECT DISTINCT "+queryWildcards+"\n" +
                "WHERE {\n" +
                 queryComponents.second +
                "}}\n\n"+
                queryComponents.third + "}"

        return header+query
    }

    fun generateSPARQLCountQuery(): String {
        val graphQueries = generateReturnValuesAndGraphQueries()
        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
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

        for (line in queryLines) {
            val fromUri = line.fromUri ?: throw Exception("Invalid From URI!")
            //var relationType = line.relationType?.let { relationTypes.get(it) } ?: throw Exception("Invalid Relation Type!")
            val relationType = line.relationType ?: throw Exception("Invalid Relation Type!")
            val toUri = line.toUri ?: throw Exception("Invalid To URI!")
            val fromRDFUri = getRDFURI(fromUri)
            val toRDFUri = getRDFURI(toUri)

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

        val constraints = generateConstraintQueries(triples)

        return Triple(returnValues, graphQueries, constraints)
    }


    private fun generateConstraintQueries(triples: Collection<Triple<String, BGRelationType, String>>): String {

        // Graphs are key, then all the queries on the graphs.
        val constraintQueries = HashMap<String, HashSet<String>>()
        fun addToQueries(key: String, sparql: String) {
            if (!constraintQueries.containsKey(key)) {
                constraintQueries[key] = HashSet()
            }
            constraintQueries[key]?.add(sparql)
        }

        val usedVariables = variableManager.usedVariables.values.toHashSet().map { "?"+it }.toTypedArray()
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
//            val relevantSources = serviceManager.cache.activeSources.filter { it.relationTypes.contains(triple.second) }
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


        if (constraintValues.count() > 0) {
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