package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.biogwplugin.internal.util.sanitizeParameter
import org.cytoscape.model.CyNetwork
import org.cytoscape.work.TaskIterator

import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.ArrayList
import javax.swing.event.ChangeEvent

/**
 * Created by sholmas on 23/05/2017.
 */


class BGOptionalURIField(val textField: JTextField, val listener: ActionListener): JPanel() {
    val comboBox: JComboBox<String>
    init {
        val comboBoxOptions = arrayOf("Gene", "Protein")
        comboBox = JComboBox(comboBoxOptions)
        this.layout = FlowLayout()
        this.add(textField)
        this.add(comboBox)
        val searchButton = BGComponentButton("Lookup", this)
        searchButton.addActionListener(listener)
        searchButton.actionCommand = BGQueryBuilderController.ACTION_LOOKUP_NODE_URI
        this.add(searchButton)
    }
}

class BGRelationQueryRow(relationTypes: Array<String>, val listener: ActionListener): JPanel() {
    val fromNodeField = BGOptionalURIField(JTextField(), listener)
    val toNodeField = BGOptionalURIField(JTextField(), listener)
    val comboBox: JComboBox<String>
    init {
        this.layout = FlowLayout()
        comboBox = JComboBox(relationTypes)
        this.add(fromNodeField)
        this.add(comboBox)
        this.add(toNodeField)
    }
}

class BGRelationTypeField(val combobox: JComboBox<String>): JPanel() {
    var direction: BGRelationDirection = BGRelationDirection.TO
    init {
        this.layout = FlowLayout()
        this.add(combobox)
        val directionButton = JButton("↓")
        directionButton.addActionListener {
            directionButton.text = when (direction) {
                BGRelationDirection.TO -> {
                    direction = BGRelationDirection.FROM
                    "↑"
                }
                BGRelationDirection.FROM -> {
                    direction = BGRelationDirection.TO
                    "↓"
                }
            }
        }
        this.add(directionButton)
    }
}

private abstract class BGResultRow()

private class BGNodeResultRow(val node: BGNode): BGResultRow()
private class BGRelationResultRow(val relation: BGRelation): BGResultRow()

class BGComponentButton(label: String, val associatedComponent: JComponent): JButton(label)

class BGQueryBuilderController(private val serviceManager: BGServiceManager) : ActionListener, ChangeListener {

    private val view: BGQueryBuilderView

    //private var relationList = ArrayList<BGRelation>()

    private var currentQuery: QueryTemplate? = null

    private var currentReturnData: BGReturnData? = null
    //private var currentResultsInTable = HashMap<String, BGResultRow>()
    private var currentResultsInTable = HashMap<Int, BGResultRow>()

    private var  queries = HashMap<String, QueryTemplate>()

    init {
        this.view = BGQueryBuilderView(this)
        this.queries = serviceManager.server.cache.queryTemplates
        updateUIAfterXMLLoad()
    }

    private fun updateUIAfterXMLLoad() {
        view.querySelectionBox.removeAllItems()
        for (queryName in queries.keys) {
            view.querySelectionBox.addItem(queryName)
        }
        setupMultiQueryPanel()
    }

    private fun setupMultiQueryPanel() {
        val panel = BGMultiQueryPanel(serviceManager)
        panel.addQueryLine()
        view.setUpMultiQueryPanel(panel)
    }

    /*
    private fun setupChainedQuery() {
        val firstNode = BGQueryParameter("parameter0", "Node URI", BGQueryParameter.ParameterType.OPTIONAL_URI)
        chainedQuery.addParameter(firstNode)
        view.addChainedParameterField(firstNode)
        addMultiQueryLine()
//        val firstRow = BGQueryParameter("row1", "", BGQueryParameter.ParameterType.RELATION_QUERY_ROW)
//        for (relation in serviceManager.server.cache.relationTypeMap.values) {
//            firstRow.addOption(relation.name, relation.uri)
//        }
//        view.addChainedParameterField(firstRow)
    }
    */

    private fun readParameterComponents(parameters: Collection<BGQueryParameter>, parameterComponents: HashMap<String, JComponent>) {
        for (parameter in parameters) {
            val component = parameterComponents[parameter.id]

            when (parameter.type) {
                BGQueryParameter.ParameterType.TEXT -> parameter.value = (component as JTextField).text.sanitizeParameter()
                BGQueryParameter.ParameterType.CHECKBOX -> parameter.value = if ((component as JCheckBox).isSelected) "true" else "false"
                BGQueryParameter.ParameterType.COMBOBOX -> {
                    val box = component as JComboBox<String>
                    val selected = box.selectedItem as String
                    parameter.value = parameter.options[selected]
                }
                BGQueryParameter.ParameterType.RELATION_COMBOBOX -> {
                    val field = component as BGRelationTypeField
                    val selected = field.combobox.selectedItem as String
                    parameter.value = parameter.options[selected]
                    parameter.direction = field.direction

                }
                BGQueryParameter.ParameterType.UNIPROT_ID -> {
                    var uniprotID = (component as JTextField).text
                    if (!uniprotID.startsWith(UNIPROT_PREFIX)) {
                        uniprotID = UNIPROT_PREFIX + uniprotID
                    }
                    parameter.value = uniprotID.sanitizeParameter()
                }
                BGQueryParameter.ParameterType.ONTOLOGY -> {
                    var ontology = (component as JTextField).text
                    if (!ontology.startsWith(ONTOLOGY_PREFIX)) {
                        ontology = ONTOLOGY_PREFIX + ontology
                    }
                    parameter.value = ontology.sanitizeParameter()
                }
                BGQueryParameter.ParameterType.OPTIONAL_URI -> {
                    val optionalUriField = component as? BGOptionalURIField ?: throw Exception("Invalid component type!")
                    val uri = optionalUriField.textField.text
                    if (uri.startsWith("?")) {
                        parameter.value = uri
                    } else if (uri.startsWith("http://")) {
                        parameter.value = "<"+uri+">" // Virtuoso requires brackets if it's a real URL.
                    } else {
                        // It's just a protein name, try to generate the URI.
                        val baseUri = when (optionalUriField.comboBox.model.selectedItem) {
                            "Protein" -> "http://identifiers.org/uniprot/"
                            "Gene" -> "http://identifiers.org/ncbigene/"
                            else -> {
                                throw Exception("Invalid optionalUriField combobox value! Must be Gene or Protein!")
                            }
                        }
                        val uri = baseUri + uri
                        parameter.value = "<"+uri+">"
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun updateSelectedQuery() {
        currentQuery = queries[view.querySelectionBox.selectedItem as String]
        if (currentQuery != null) {
            view.generateParameterFields(currentQuery)
        }
    }

    private fun openXMLFile() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createQuery() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun runQuery() {
        val errorText = validatePropertyFields(currentQuery!!.parameters, view.parameterComponents)
        if (errorText != null) {
            JOptionPane.showMessageDialog(view.mainFrame, errorText)
        } else {
            readParameterComponents(currentQuery!!.parameters, view.parameterComponents)
            val queryString = createQueryString(currentQuery!!)
            view.sparqlTextArea.text = queryString ?: throw Exception("Query String cannot be empty!")

            //TODO: Make this independent from node search!
            val query: BGQuery?
            val queryType = currentQuery!!.returnType


            query = when(queryType){
                BGReturnType.NODE_LIST, BGReturnType.NODE_LIST_DESCRIPTION -> {
                    BGNodeSearchQuery(serviceManager, queryString, queryType, serviceManager.server.parser)
                }
                BGReturnType.RELATION_TRIPLE, BGReturnType.RELATION_TRIPLE_NAMED -> {
                    BGRelationsQuery(serviceManager, queryString, serviceManager.server.parser, queryType)
                }
                else -> {
                    throw Exception("Unexpected query type: "+queryType.toString())
                }
            }


            //query = BGGenericQuery(SERVER_PATH, queryString, serviceManager.server.parser, queryType)

            query.addCompletion {

                val data = when(queryType) {
                    BGReturnType.NODE_LIST, BGReturnType.NODE_LIST_DESCRIPTION -> {
                        it as? BGReturnNodeData ?: throw Exception("Expected Node Data in return!")
                    }
                    BGReturnType.RELATION_TRIPLE, BGReturnType.RELATION_TRIPLE_NAMED -> {
                        it as? BGReturnRelationsData ?: throw Exception("Expected Relation Data in return!")
                    }
                    else -> {
                        throw Exception("Unexpected query type: "+queryType.toString())
                    }
                }
                currentReturnData = data

                val tableModel = view.resultTable.model as DefaultTableModel
                tableModel.setColumnIdentifiers(data.columnNames)

                for (i in tableModel.rowCount -1 downTo 0) {
                    tableModel.removeRow(i)
                }

                if (data is BGReturnNodeData) {
                    setNodeTableData(data.nodeData.values)
                }
                if (data is BGReturnRelationsData) {
                    setRelationTableData(data.relationsData)
                }
                view.tabPanel.selectedIndex = 3 // Open the result tab.

                // Try the darnest to make the window appear on top!
                fightForFocus()
            }

            val iterator = TaskIterator(query)
            serviceManager.taskManager.execute(iterator)

        }
    }

    private fun fightForFocus() {
        EventQueue.invokeLater {
            view.mainFrame.toFront()
            view.mainFrame.isAlwaysOnTop = true
            view.mainFrame.isAlwaysOnTop = false
            view.mainFrame.requestFocus()
        }
    }

    private fun createQueryString(currentQuery: QueryTemplate): String? {
        var queryString = currentQuery.sparqlString

        // First do all checkboxes, then everything else.
        // This is because the checkboxes might add code containing variables that needs to be changed.
        for (parameter in currentQuery.parameters) {
            if (parameter.type == BGQueryParameter.ParameterType.CHECKBOX) {
                var value = when (parameter.value) {
                    "true" -> parameter.options["true"]
                    "false" -> parameter.options["false"]
                    else -> ""
                }
                val searchString = "@" + parameter.id
                if (value != null) {
                    queryString = queryString.replace(searchString, value)
                }
            }
        }
        for (parameter in currentQuery.parameters) {
            if (parameter.type != BGQueryParameter.ParameterType.CHECKBOX) {
                val value = parameter.value ?: throw NullPointerException("Parameter value cannot be null!")
                val searchString = "@"+parameter.id
                queryString = queryString.replace(searchString.toRegex(), value)
            }
        }

        return queryString
    }

    private fun importSelectedResults(network: CyNetwork?, returnType: BGReturnType) {
        var network = network
        val server = serviceManager.server
        // 1. Get the selected lines from the table.
        val nodes = HashMap<String, BGNode>()
        val relations = ArrayList<BGRelation>()
        val model = view.resultTable.model as DefaultTableModel
        for (rowNumber in view.resultTable.selectedRows) {

            val resultRow = currentResultsInTable[view.resultTable.convertRowIndexToModel(rowNumber)]

            when(returnType) {
                BGReturnType.NODE_LIST, BGReturnType.NODE_LIST_DESCRIPTION, BGReturnType.NODE_LIST_DESCRIPTION_TAXON -> {
                    val node = (resultRow as? BGNodeResultRow)?.node ?: throw Exception("Result must be a node!")
                    nodes.put(node.uri, node)
                }
                BGReturnType.RELATION_TRIPLE, BGReturnType.RELATION_TRIPLE_NAMED, BGReturnType.RELATION_MULTIPART_NAMED -> {
                    val relation = (resultRow as? BGRelationResultRow)?.relation ?: throw Exception("Result must be a relation!")
                    nodes.put(relation.fromNode.uri, relation.fromNode)
                    nodes.put(relation.fromNode.uri, relation.fromNode)
                    relations.add(relation)
                }
            }

        }
        // 2. The nodes have already been fetched. There should be a cache storing them somewhere.
        if (network == null) {
            network = serviceManager.server.networkBuilder.createNetworkFromBGNodes(nodes.values)
        } else {
            serviceManager.server.networkBuilder.addBGNodesToNetwork(nodes.values, network)
        }
        server.networkBuilder.addRelationsToNetwork(network, relations)
        serviceManager.networkManager.addNetwork(network)
        serviceManager.server.networkBuilder.destroyAndRecreateNetworkView(network, serviceManager)
    }


    private fun validatePropertyFields(parameters: Collection<BGQueryParameter>, parameterComponents: HashMap<String, JComponent>): String? {
        for (parameter in parameters) {
            val component = parameterComponents[parameter.id]
                if (parameter.type === BGQueryParameter.ParameterType.OPTIONAL_URI) {
                    val optionalUriField = component as? BGOptionalURIField ?: throw Exception("Invalid component type!")
                    val uri = optionalUriField.textField.text
                    if (uri.sanitizeParameter().isEmpty()) {
                        // Empty string.
                        optionalUriField.textField.text = "?" + parameter.id
                    } else if (uri.startsWith("?")) {
                        optionalUriField.textField.text = uri.sanitizeParameter()
                    } else {
                        // TODO: Should have a "validate" button instead, letting the user choose to check.
                        // Validate the URI.
//                        val validated = Utility.validateURI(uri) // UGLY HACK! Should be asynchronous:
//                        if (!validated) {
//                            return "Unknown URI!"
//                        }

                    }
                } else if (component is JTextField) {
                    val field = component
                    field.text = Utility.sanitizeParameter(field.text)
                    if (field.text.isEmpty()) {
                        return "All required text fields must be filled out!"
                    }
                }
            }
        return null
    }

    private fun validateUris() {
        if (view.tabPanel.selectedIndex == 0) {
            val errorText = validateMultiQuery()
            if (errorText != null) {
                JOptionPane.showMessageDialog(view.mainFrame, errorText)
            }
        }
    }

    private fun validateMultiQuery(): String? {

        for (line in view.multiQueryPanel.queryLines) {
            val fromUri = line.fromUri ?: return "The URI can not be left blank when not using variables."
            val toUri = line.toUri ?: return "The URI can not be left blank when not using variables."

            if (!fromUri.startsWith("?") && !fromUri.startsWith("<http://")) return "The From URI is invalid."
            if (!toUri.startsWith("?") && !toUri.startsWith("<http://")) return "The To URI is invalid."
        }
        return null
    }

    fun lookupNodeUri(button: BGComponentButton) {
        val component = button.associatedComponent as? BGOptionalURIField
        component?.let {
            val optionalUriComponent = it
            val searchString = optionalUriComponent.textField.text
            val nodeType = when (optionalUriComponent.comboBox.selectedItem) {
                "Protein" -> BGNodeType.Protein
                "Gene" -> BGNodeType.Gene
                else -> {
                    throw Exception("Invalid node type selected in combobox!")
                }
            }
            val query = BGQuickFetchNodeQuery(serviceManager, searchString, nodeType, serviceManager.server.parser)
            query.addCompletion {
                val results = query.returnData as? BGReturnNodeData ?: throw Exception("Invalid return data!")
                val nodeSelectionViewController = BGQuickSearchResultsController(serviceManager, results.nodeData, {
                    val node = it
                    // Set the value of the field to the uri of the node found and selected.
                    optionalUriComponent.textField.text = node.uri
                    fightForFocus()
                })
            }
            serviceManager.taskManager.execute(TaskIterator(query))
        }
    }


    override fun actionPerformed(e: ActionEvent) {
        when (e.actionCommand) {
            ACTION_OPEN_XML_FILE -> openXMLFile()
            ACTION_CREATE_QUERY -> createQuery()
            ACTION_RUN_QUERY -> runQuery()
            ACTION_CHANGED_QUERY -> updateSelectedQuery()
            ACTION_IMPORT_TO_SELECTED -> {
                val network = serviceManager.applicationManager.currentNetwork
                importSelectedResults(network, currentQuery!!.returnType)
            }
            ACTION_IMPORT_TO_NEW -> importSelectedResults(null, currentQuery!!.returnType)
            ACTION_ADD_MULTIQUERY_LINE -> addMultiQueryLine()
            ACTION_REMOVE_MULTIQUERY_LINE -> removeMultiQueryLine()
            ACTION_RUN_MULTIQUERY -> runMultiQuery()
            ACTION_VALIDATE_URIS -> validateUris()
            ACTION_LOOKUP_NODE_URI -> {
                val button = e.source as? BGComponentButton ?: throw Exception("Expected BGComponentButton")
                lookupNodeUri(button)
            }
            ACTION_FILTER_EDGES_TO_EXISTING -> {
                val box = e.source as? JCheckBox ?: throw Exception("Expected JCheckBox!")
                filterRelationsToNodesInCurrentNetwork(box.isSelected)
            }
            else -> {
            }
        }
    }

    private fun filterRelationsToNodesInCurrentNetwork(filterOn: Boolean) {

        val returnData = currentReturnData as? BGReturnRelationsData ?: return
        val relationsFound = returnData.relationsData

        if (filterOn) {
            val network = serviceManager.applicationManager.currentNetwork
            val allNodeUris = network.defaultNodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI).getValues(String::class.java)
            var relations = ArrayList<BGRelation>()
            for (result in relationsFound) {
                if (allNodeUris.contains(result.toNode.uri) || allNodeUris.contains(result.fromNode.uri)) {
                    relations.add(result)
                }
            }
            setRelationTableData(relations)
        } else {
            setRelationTableData(relationsFound)
        }
    }

    private fun setNodeTableData(nodes: Collection<BGNode>) {
        val tableModel = view.resultTable.model as DefaultTableModel

        currentResultsInTable.clear()
        for (i in tableModel.rowCount -1 downTo 0) {
            tableModel.removeRow(i)
        }
        for (node in nodes) {
            val row = arrayOf(node.uri, node.name, node.description)
            tableModel.addRow(row)
            currentResultsInTable[tableModel.rowCount-1] = BGNodeResultRow(node)
        }
    }

    private fun setRelationTableData(relations: Collection<BGRelation>) {
        val tableModel = view.resultTable.model as DefaultTableModel

        currentResultsInTable.clear()
        for (i in tableModel.rowCount -1 downTo 0) {
            tableModel.removeRow(i)
        }
        for (relation in relations) {
            val fromNodeName = relation.fromNode.name ?: relation.fromNode.uri
            val relationName = relation.relationType.name
            val toNodeName = relation.toNode.name ?: relation.toNode.uri
            val row = arrayOf(fromNodeName, relationName, toNodeName)
            tableModel.addRow(row)
            currentResultsInTable[tableModel.rowCount-1] = BGRelationResultRow(relation)
        }
    }

    override fun stateChanged(e: ChangeEvent?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun runMultiQuery() {

        val errorText = validateMultiQuery()
        if (errorText != null) {
            JOptionPane.showMessageDialog(view.mainFrame, errorText)
        } else {

            val queryString = view.multiQueryPanel.generateSPARQLQuery()
            view.sparqlTextArea.text = queryString

            val queryType = BGReturnType.RELATION_MULTIPART_NAMED

            val query = BGMultiRelationsQuery(serviceManager, queryString, serviceManager.server.parser, queryType)

            query.addCompletion {
                val data = it as? BGReturnRelationsData ?: throw Exception("Expected Relation Data in return!")
                currentReturnData = data

                val tableModel = view.resultTable.model as DefaultTableModel
                tableModel.setColumnIdentifiers(data.columnNames)

                setRelationTableData(data.relationsData)

                view.tabPanel.selectedIndex = 3 // Open the result tab.

                // Try the darnest to make the window appear on top!
                fightForFocus()
            }
            val iterator = TaskIterator(query)
            serviceManager.taskManager.execute(iterator)
        }
    }

    private fun removeMultiQueryLine() {
        view.removeLastMultiQueryLine()
    }

    private fun addMultiQueryLine() {
        view.addMultiQueryLine();
    }


    companion object {
        public val SERVER_PATH = "http://www.semantic-systems-biology.org/biogateway/endpoint"
        public val ACTION_OPEN_XML_FILE = "openXMLFile"
        public val ACTION_PARSE_XML = "parseXML"
        public val ACTION_CREATE_QUERY = "crateQuery"
        public val ACTION_CHANGED_QUERY = "changedQueryComboBox"
        public val ACTION_RUN_QUERY = "runBiogwQuery"
        public val ACTION_IMPORT_TO_SELECTED = "importToSelectedNetwork"
        public val ACTION_IMPORT_TO_NEW = "importToNewNetwork"
        public val ACTION_VALIDATE_URIS = "validateUris"
        public val ACTION_RUN_MULTIQUERY = "runMultiQuery"
        public val ACTION_ADD_MULTIQUERY_LINE = "addChainRelation"
        public val ACTION_REMOVE_MULTIQUERY_LINE = "removeChainRelation"
        public val CHANGE_TAB_CHANGED = "tabbedPaneHasChanged"
        public val UNIPROT_PREFIX = "http://identifiers.org/uniprot/"
        public val ONTOLOGY_PREFIX = "http://purl.obolibrary.org/obo/"
        public val ACTION_LOOKUP_NODE_URI = "Lookup Node Uri from name"
        public val ACTION_FILTER_EDGES_TO_EXISTING = "filter relations to exsisting nodes"
    }
}
