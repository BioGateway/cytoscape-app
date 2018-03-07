package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.BGColorComboBoxRenderer
import org.cytoscape.biogwplugin.internal.gui.BGColorableText
import org.cytoscape.biogwplugin.internal.model.BGQueryConstraint
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGSPARQLParser
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import java.awt.FlowLayout
import javax.swing.*


class BGMultiQueryPanel(val serviceManager: BGServiceManager, val constraintPanel: BGQueryConstraintPanel): JPanel() {

    val deleteButtonTooltipText = "Delete this row."

    val variableManager = BGQueryVariableManager()
    val relationTypes = serviceManager.cache.relationTypeDescriptions


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

        val queryLine = BGMultiQueryAutocompleteLine(serviceManager, relationTypeBox, variableManager)

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

        queryLine.relationTypeComboBox.selectedItem = relationType

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

    fun generateSPARQLQuery(): String {
        val queryComponents = generateReturnValuesAndGraphQueries()

        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT DISTINCT " + queryComponents.first + "\n" +
                "WHERE {\n" +
                queryComponents.second +
                queryComponents.third +
                "}"

        return query
    }

    fun generateSPARQLCountQuery(): String {
        val graphQueries = generateReturnValuesAndGraphQueries()
        val query = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT COUNT (*) \n" +
                "WHERE {\n" +
                graphQueries.second +
                graphQueries.third +
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

            val graphName = relationType.defaultGraphName ?: generateGraphName(numberOfGraphQueries, relationType)

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

        val constraintValues = constraintPanel.getConstraintValues()

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

        val constraintHeader = "\n#QueryConstraints:\n"+constraintValues
                .filter { it.value.isEnabled }
                .map { "#Constraint: "+it.key.id+"="+it.value.stringValue+"\n"}
                .reduce { acc, s -> acc+s }

        var constraintQueryString = ""

        for (graph in constraintQueries.keys) {

            val lines = constraintQueries[graph] ?: continue

            constraintQueryString += "\n" +
                    "GRAPH <"+graph+"> { \n"
            for (line in lines) {
                constraintQueryString += line+"\n"
            }
            constraintQueryString += "}\n"
        }
        return constraintHeader+constraintQueryString
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