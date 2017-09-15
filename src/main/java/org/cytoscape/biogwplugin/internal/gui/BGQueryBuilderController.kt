package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.biogwplugin.internal.util.sanitizeParameter
import org.cytoscape.model.CyNetwork
import org.cytoscape.work.TaskIterator

import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.filechooser.FileFilter
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.util.ArrayList

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

class BGComponentButton(label: String, val associatedComponent: JComponent): JButton(label)

class BGQueryBuilderController(private val serviceManager: BGServiceManager) : ActionListener, ChangeListener {
    private val view: BGCreateQueryView

    //private var relationList = ArrayList<BGRelation>()

    private var currentQuery: QueryTemplate? = null
    private var chainedQuery = QueryTemplate("", "", "", BGReturnType.RELATION_MULTIPART_NAMED)

    private var currentReturnData: BGReturnData? = null
    private var  queries = HashMap<String, QueryTemplate>()

    init {
        this.view = BGCreateQueryView(this)
        this.queries = serviceManager.server.cache.queryTemplates
        updateUIAfterXMLLoad()
    }



    private fun updateUIAfterXMLLoad() {
        view.querySelectionBox.removeAllItems()
        for (queryName in queries.keys) {
            view.querySelectionBox.addItem(queryName)
        }
        setupChainedQuery()
    }

    private fun setupChainedQuery() {
        val firstNode = BGQueryParameter("parameter0", "Node URI", BGQueryParameter.ParameterType.OPTIONAL_URI)
        chainedQuery.addParameter(firstNode)
        view.addChainedParameterField(firstNode)
        addChainedParameterLine()
//        val firstRow = BGQueryParameter("row1", "", BGQueryParameter.ParameterType.RELATION_QUERY_ROW)
//        for (relation in serviceManager.server.cache.relationTypes.values) {
//            firstRow.addOption(relation.description, relation.uri)
//        }
//        view.addChainedParameterField(firstRow)
    }

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
                    for ((uri, node) in data.nodeData) {
                        val row = arrayOf(uri, node.name, node.description)
                        tableModel.addRow(row)
                    }
                }
                if (data is BGReturnRelationsData) {

                    for (relation in data.relationsData) {

                        var row = relation.stringArray()
                        val fromNodeName = relation.fromNode.name ?: relation.fromNode.uri
                        val relationName = relation.relationType.description
                        val toNodeName = relation.toNode.name ?: relation.toNode.uri
                        row = arrayOf(fromNodeName, relationName, toNodeName)
                        tableModel.addRow(row)
                    }
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
        for (row in view.resultTable.selectedRows) {

            when(returnType) {
                BGReturnType.NODE_LIST -> {
                    val uri = model.getValueAt(row, 0) as String
                    val name = model.getValueAt(row, 1) as String
                    val node = server.getNodeFromCache(BGNode(uri, name))
                    nodes.put(uri, node)
                }
                BGReturnType.NODE_LIST_DESCRIPTION -> {
                    val uri = model.getValueAt(row, 0) as String
                    val name = model.getValueAt(row, 1) as String
                    val description = model.getValueAt(row, 2) as String
                    val node = server.getNodeFromCache(BGNode(uri, name, description))
                    nodes.put(uri, node)
                }
                BGReturnType.RELATION_TRIPLE -> {
                    /*
                    // TODO: We seem to lose the relation data after importing it, because the only thing we do is populating this table.
                    val fromNodeUri = model.getValueAt(row, 0) as String
                    val relationUri = model.getValueAt(row, 1) as String
                    val toNodeUri = model.getValueAt(row, 2) as String
                    val fromNode = server.getNodeFromCache(BGNode(fromNodeUri))
                    val toNode = server.getNodeFromCache(BGNode(toNodeUri))
                    var relationType = server.cache.relationTypes.get(relationUri)

                    if (relationType == null) {
                        relationType = BGRelationType(relationUri, relationUri)
                    }
                    */
                    //val relation = BGRelation(fromNode, relationType, toNode)

                    val returnData = currentReturnData as? BGReturnRelationsData
                    returnData?.let {
                        //TODO: WARNING! BUG! When sorting the table, the row is no longer the same as relation index!
                        val relation = returnData.relationsData[row]
                        nodes.put(relation.fromNode.uri, relation.fromNode)
                        nodes.put(relation.toNode.uri, relation.toNode)
                        relations.add(relation)
                    }
                }
                BGReturnType.RELATION_TRIPLE_NAMED -> {

                }
                BGReturnType.RELATION_MULTIPART_NAMED -> {
                    val returnData = currentReturnData as? BGReturnRelationsData
                    returnData?.let {
                        val relation = returnData.relationsData[row]
                        nodes.put(relation.fromNode.uri, relation.fromNode)
                        nodes.put(relation.toNode.uri, relation.toNode)
                        relations.add(relation)
                    }
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
        println("TODO: Validate those URIs!")
    }

    private fun openFileChooser(): File? {
        val chooser = JFileChooser()

        val filter = object : FileFilter() {
            override fun getDescription(): String {
                return "XML File"
            }

            override fun accept(f: File): Boolean {

                if (f.name.toLowerCase().endsWith("xml")) return true
                if (f.isDirectory) return true
                return false
            }
        }

        chooser.fileFilter = filter

        val choice = chooser.showOpenDialog(view.mainFrame)
        if (choice != JFileChooser.APPROVE_OPTION) return null
        val chosenFile = chooser.selectedFile

        return chosenFile
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
            ACTION_ADD_CHAIN_RELATION -> addChainedParameterLine()
            ACTION_REMOVE_CHAIN_RELATION -> removeLastChainedParameter()
            ACTION_RUN_CHAIN_QUERY -> runChainQuery()
            ACTION_VALIDATE_URIS -> validateUris()
            ACTION_LOOKUP_NODE_URI -> {
                val button = e.source as? BGComponentButton ?: throw Exception("Expected BGComponentButton")
                val component = button.associatedComponent as? BGOptionalURIField
                component?.let {
                    val optionalUriComponent = it
                    val searchString = optionalUriComponent.textField.text
                    val nodeType = when (optionalUriComponent.comboBox.selectedItem) {
                        "Protein" -> BGNodeType.PROTEIN
                        "Gene" -> BGNodeType.GENE
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
            else -> {
            }
        }
    }

    private fun addChainedParameterLine() {

        val parameterCount = chainedQuery.parameters.count()
        var relationParameter = BGQueryParameter("parameter"+(parameterCount+1), "Relation", BGQueryParameter.ParameterType.RELATION_COMBOBOX)
        var nodeParameter = BGQueryParameter("parameter"+(parameterCount+2), "Node URI", BGQueryParameter.ParameterType.OPTIONAL_URI)

        for (relation in serviceManager.server.cache.relationTypes.values) {
            relationParameter.addOption(relation.description, relation.uri)
        }
        chainedQuery.addParameter(relationParameter)
        chainedQuery.addParameter(nodeParameter)
        view.addChainedParameterLine(relationParameter, nodeParameter)

    }

    private fun runChainQuery() {
        validatePropertyFields(chainedQuery.parameters, view.chainedParametersComponents)
        readParameterComponents(chainedQuery.parameters, view.chainedParametersComponents)
        chainedQuery.sparqlString = generateChainQuery(chainedQuery)
        val queryString = createQueryString(chainedQuery)
        view.sparqlTextArea.text = queryString ?: throw Exception("Query String cannot be empty!")

        val query = BGChainedRelationsQuery(serviceManager, queryString, serviceManager.server.parser, BGReturnType.RELATION_MULTIPART_NAMED)
        query.addCompletion {
            val data = it as? BGReturnRelationsData ?: throw Exception("Expected relations data in return!")

            currentReturnData = data

            val tableModel = view.resultTable.model as DefaultTableModel
            val columnNames = arrayOf("from node uri", "from node name", "relation", "to node uri", "to node name")
            tableModel.setColumnIdentifiers(columnNames)

            for (i in tableModel.rowCount -1 downTo 0) {
                tableModel.removeRow(i)
            }

            for (relation in data.relationsData) {
                val fromNodeName = relation.fromNode.name ?: relation.fromNode.uri
                val relationName = relation.relationType.description
                val toNodeName = relation.toNode.name ?: relation.toNode.uri
                val row = arrayOf(relation.fromNode.uri, fromNodeName, relationName, relation.toNode.uri, toNodeName)
                tableModel.addRow(row)
            }

            view.tabPanel.selectedIndex = 3
            // Try the darnest to make the window appear on top!
            fightForFocus()
        }
        val iterator = TaskIterator(query)
        serviceManager.taskManager.execute(iterator)
    }

    private fun removeLastChainedParameter() {

        val lastParameterIndex = chainedQuery.parameters.count() -1
        val secondLastParameterIndex = chainedQuery.parameters.count() -2
        val lastParameter = chainedQuery.parameters.get(lastParameterIndex)
        val secondLastParameter = chainedQuery.parameters.get(secondLastParameterIndex)

        chainedQuery.parameters.removeAt(lastParameterIndex)
        chainedQuery.parameters.removeAt(secondLastParameterIndex)

        view.removeParameterField(lastParameter)
        view.removeParameterField(secondLastParameter)
    }

    override fun stateChanged(e: ChangeEvent) {

    }

    private fun generateChainQuery(chainQueryTemplate: QueryTemplate): String {
        var nodes = ArrayList<String>()
        val relations = ArrayList<Pair<String, BGRelationDirection>>()
        var returnNames = ArrayList<String>()
        var returnSparqlString = ""

        for (parameter in chainQueryTemplate.parameters) {
            if (parameter.type == BGQueryParameter.ParameterType.OPTIONAL_URI) {
                nodes.add(parameter.id)
                returnSparqlString += " @"+parameter.id+" ?name_"+parameter.id
            }
            if (parameter.type == BGQueryParameter.ParameterType.RELATION_COMBOBOX) {
                val parameterTag = "<@"+parameter.id+">"
                val direction = parameter.direction ?: BGRelationDirection.TO
                relations.add(Pair(parameterTag, direction))
                returnNames.add(parameterTag)
                returnSparqlString += " "+parameterTag
            }
        }

        var nodeCounter = 0
        var relationCounter = 0
        var graphCounter = 0
        var graphsQueryString = ""

        while(nodeCounter < nodes.size && relationCounter < relations.size) {
            val relation = relations.get(relationCounter)
            val firstNode = nodes.get(nodeCounter)
            nodeCounter += 1
            relationCounter += 1
            val secondNode = nodes.get(nodeCounter)

            when (relation.second) {
                BGRelationDirection.TO -> graphsQueryString += generateChainQuerySparqlGraph(graphCounter, firstNode, relation.first, secondNode)
                BGRelationDirection.FROM -> graphsQueryString += generateChainQuerySparqlGraph(graphCounter, secondNode, relation.first, firstNode)
            }
            graphCounter += 1
        }

        return generateChainQuerySparql(returnSparqlString, graphsQueryString, generateChainQuerySparqlNameGraphs(nodes))
    }

    private fun generateChainQuerySparqlGraph(graphNumber: Int, first: String, relation: String, second: String): String {
        return "GRAPH ?graph"+graphNumber+" {\n" +
                "@"+first+" "+relation+" @"+second+" .\n" +
                "}\n"
    }

    private fun generateChainQuerySparqlNameGraphs(nodeNames: List<String>): String {
        var nameQueryLines = ""
        for (name in nodeNames) {
            nameQueryLines += "@"+name+" skos:prefLabel ?name_"+name+" .\n"
        }

        return "GRAPH ?nameGraph {\n" +
                nameQueryLines +
                "}\n" +
                "GRAPH ?nameGraph2 {\n" +
                nameQueryLines +
                "}"
    }

    private fun generateChainQuerySparql(returnNames: String, graphQueries: String, nameQueries: String): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "SELECT DISTINCT" + returnNames + "\n" +
                "WHERE {\n" +
                graphQueries +
                nameQueries +
                "}"
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
        public val ACTION_RUN_CHAIN_QUERY = "runChainQuery"
        public val ACTION_ADD_CHAIN_RELATION = "addChainRelation"
        public val ACTION_REMOVE_CHAIN_RELATION = "removeChainRelation"
        public val CHANGE_TAB_CHANGED = "tabbedPaneHasChanged"
        public val UNIPROT_PREFIX = "http://identifiers.org/uniprot/"
        public val ONTOLOGY_PREFIX = "http://purl.obolibrary.org/obo/"
        public val ACTION_LOOKUP_NODE_URI = "Lookup Node Uri from name"
    }
}
