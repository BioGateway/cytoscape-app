package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.multiquery.BGMultiQueryAutocompleteLine
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGSPARQLParser
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import java.awt.FlowLayout
import javax.swing.*


class BGMultiQueryPanel(val serviceManager: BGServiceManager): JPanel() {

    val deleteButtonTooltipText = "Delete this row."

    val variableManager = BGQueryVariableManager()
    val relationTypes = serviceManager.cache.relationTypeDescriptions

    init {
        layout = FlowLayout()
    }

    var queryLines = ArrayList<BGMultiQueryAutocompleteLine>()


    private fun createQueryLine(): BGMultiQueryAutocompleteLine {
        val fromField = JTextField()
        //fromField.preferredSize = Dimension(290, Utility.getJTextFieldHeight())
        fromField.columns = Constants.BG_QUERY_BUILDER_URI_FIELD_COLUMNS
        val toField = JTextField()
        //toField.preferredSize = Dimension(290, Utility.getJTextFieldHeight())
        toField.columns = Constants.BG_QUERY_BUILDER_URI_FIELD_COLUMNS

        val relationTypeBox = JComboBox(relationTypes.keys.toTypedArray())
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