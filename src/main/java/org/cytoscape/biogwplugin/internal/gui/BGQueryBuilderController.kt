package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.biogwplugin.internal.util.sanitizeParameter
import org.cytoscape.model.CyNetwork
import org.cytoscape.work.TaskIterator
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.util.ArrayList
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.filechooser.FileFilter
import javax.swing.table.DefaultTableModel
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.set
import javax.swing.JFileChooser


class BGOptionalURIField(val textField: JTextField, val serviceManager: BGServiceManager): JPanel() {
    val comboBox: JComboBox<String>
    init {
        val comboBoxOptions = arrayOf("Gene", "Protein")
        comboBox = JComboBox(comboBoxOptions)
        this.layout = FlowLayout()
        this.add(textField)

        val searchIcon = ImageIcon(this.javaClass.classLoader.getResource("search.png"))
        val uriSearchButton = JButton(searchIcon)
        uriSearchButton.toolTipText = "Search for entity URIs."
        uriSearchButton.addActionListener {
            val lookupController = BGURILookupController(serviceManager, this) {
                if (it != null) {
                    textField.text = it.uri
                    textField.toolTipText = it.description
                }
            }
        }
        this.add(uriSearchButton)
    }
}

class BGRelationQueryRow(relationTypes: Array<String>, serviceManager: BGServiceManager): JPanel() {
    val fromNodeField = BGOptionalURIField(JTextField(), serviceManager)
    val toNodeField = BGOptionalURIField(JTextField(), serviceManager)
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

private abstract class BGResultRow

private class BGNodeResultRow(val node: BGNode): BGResultRow()
private class BGRelationResultRow(val relation: BGRelation): BGResultRow()

class BGComponentButton(label: String, val associatedComponent: JComponent): JButton(label)

class BGQueryBuilderController(private val serviceManager: BGServiceManager) : ActionListener, ChangeListener {

    private var preferences = Preferences.userRoot().node(javaClass.name)

    private val view: BGQueryBuilderView

    //private var relationList = ArrayList<BGRelation>()

    private var currentQuery: QueryTemplate? = null

    private var currentReturnData: BGReturnData? = null
    //private var currentResultsInTable = HashMap<String, BGResultRow>()
    private var currentResultsInTable = HashMap<Int, BGResultRow>()

    private var  queries = HashMap<String, QueryTemplate>()

    init {
        this.view = BGQueryBuilderView(this, serviceManager)
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
                        val parameterUri = baseUri + uri
                        parameter.value = "<"+parameterUri+">"
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

            query.addCompletion {

                val data = when (queryType) {
                    BGReturnType.NODE_LIST, BGReturnType.NODE_LIST_DESCRIPTION -> {
                        it as? BGReturnNodeData ?: throw Exception("Expected Node Data in return!")
                    }
                    BGReturnType.RELATION_TRIPLE, BGReturnType.RELATION_TRIPLE_NAMED -> {
                        it as? BGReturnRelationsData ?: throw Exception("Expected Relation Data in return!")
                    }
                    else -> {
                        throw Exception("Unexpected query type: " + queryType.toString())
                    }
                }
                currentReturnData = data

                val tableModel = view.resultTable.model as DefaultTableModel
                tableModel.setColumnIdentifiers(data.columnNames)

                for (i in tableModel.rowCount - 1 downTo 0) {
                    tableModel.removeRow(i)
                }

                if (data is BGReturnNodeData) {
                    setNodeTableData(data.nodeData.values)
                }
                if (data is BGReturnRelationsData) {
                    BGLoadUnloadedNodes.createAndRun(serviceManager, data.unloadedNodes) {
                        setRelationTableData(data.relationsData)
                        view.tabPanel.selectedIndex = TAB_PANEL_RESULTS_INDEX
                        Utility.fightForFocus(view.mainFrame)
                    }
                    return@addCompletion
                } else {
                    view.tabPanel.selectedIndex = TAB_PANEL_RESULTS_INDEX // Open the result tab.
                    Utility.fightForFocus(view.mainFrame)
                }
            }
            val iterator = TaskIterator(query)
            serviceManager.taskManager.execute(iterator)

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

    private fun generateSPARQLCode(): String? {
            val errorText = validateMultiQuery()
            if (errorText != null) {
                JOptionPane.showMessageDialog(view.mainFrame, errorText)
            } else {
                val queryString = view.multiQueryPanel.generateSPARQLQuery()
                view.sparqlTextArea.text = queryString
                return queryString
            }
        return null
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

    private fun parseSPARQLCode() {

        val sparqlCode = view.sparqlTextArea.text
        if (sparqlCode.isEmpty()) return

        val queryGraphs = BGSPARQLParser.parseSPARQLCode(sparqlCode, serviceManager.cache.relationTypeMap)

        if (queryGraphs.isEmpty()) {
            JOptionPane.showMessageDialog(view.mainFrame, "Unable to parse any queries from current SPARQL.")
        }
        view.multiQueryPanel.loadQueryGraphs(queryGraphs)
        view.tabPanel.selectedIndex = TAB_PANEL_BUILD_QUERY_INDEX
    }

    override fun stateChanged(e: ChangeEvent?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadSPARQLFromFile() {
        val file = openFileChooser() ?: return
        val sparqlCode = file.readText()

        val queryGraphs = BGSPARQLParser.parseSPARQLCode(sparqlCode, serviceManager.cache.relationTypeMap)
        view.multiQueryPanel.loadQueryGraphs(queryGraphs)
    }

    private fun saveSPARQLToFile() {
        val sparqlCode = generateSPARQLCode() ?: return
        val file = saveFileChooser() ?: return
        file.writeText(sparqlCode)
    }

    private fun saveFileChooser(): File? {
        val lastDir = preferences.get(Constants.BG_PREFERENCES_LAST_FOLDER, File(".").absolutePath)

        val chooser = when (lastDir != null) {
            true -> JFileChooser(lastDir)
            false -> JFileChooser()
        }

        val filter = object : FileFilter() {
            override fun getDescription(): String {
                return "Biogateway SPARQL File"
            }

            override fun accept(f: File): Boolean {
                if (f.name.toLowerCase().endsWith(Constants.BG_FILE_EXTENSION)) return true
                if (f.isDirectory) return true
                return false
            }
        }
        chooser.fileFilter = filter
        val choice = chooser.showSaveDialog(view.mainFrame)
        if (choice == JFileChooser.APPROVE_OPTION) {
            preferences.put(Constants.BG_PREFERENCES_LAST_FOLDER, chooser.selectedFile.parent)
            val path = chooser.selectedFile.absolutePath
            if (!path.endsWith("."+Constants.BG_FILE_EXTENSION)) {
                val file = File(path + "."+ Constants.BG_FILE_EXTENSION)
                return file
            }
            return chooser.selectedFile
        }
        return null
    }


    private fun openFileChooser(): File? {
        val lastDir = preferences.get(Constants.BG_PREFERENCES_LAST_FOLDER, File(".").absolutePath)

        val chooser = when (lastDir != null) {
            true -> JFileChooser(lastDir)
            false -> JFileChooser()
        }

        val filter = object : FileFilter() {
            override fun getDescription(): String {
                return "Biogateway SPARQL File"
            }

            override fun accept(f: File): Boolean {
                if (f.name.toLowerCase().endsWith(Constants.BG_FILE_EXTENSION)) return true
                if (f.isDirectory) return true
                return false
            }
        }
        chooser.fileFilter = filter
        val choice = chooser.showOpenDialog(view.mainFrame)
        if (choice == JFileChooser.APPROVE_OPTION) {
            preferences.put(Constants.BG_PREFERENCES_LAST_FOLDER, chooser.selectedFile.parent)
            return chooser.selectedFile
        }
        return null
    }




    private fun runMultiQuery() {

        val errorText = validateMultiQuery()
        if (errorText != null) {
            JOptionPane.showMessageDialog(view.mainFrame, errorText)
        } else {

            val relationCount = Utility.countMatchingRowsQuery(serviceManager, view.multiQueryPanel.generateSPARQLCountQuery()) ?: throw Exception("Unable to get relation count.")

            if (relationCount > Constants.BG_RELATION_COUNT_WARNING_LIMIT) {
                val message = "Estimated "+relationCount.toString()+" relations found. This might take a very long time or time out. Are you sure you want to proceed?"
                val response = JOptionPane.showOptionDialog(null, message, "Proceed with query?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null)
                if (response != JOptionPane.OK_OPTION) {
                    return
                }
            }

            val queryString = view.multiQueryPanel.generateSPARQLQuery()
            view.sparqlTextArea.text = queryString

            val queryType = BGReturnType.RELATION_MULTIPART_NAMED

            val query = BGMultiRelationsQuery(serviceManager, queryString, serviceManager.server.parser, queryType)

            query.addCompletion {
                val data = it as? BGReturnRelationsData ?: throw Exception("Expected Relation Data in return!")
                currentReturnData = data

                val tableModel = view.resultTable.model as DefaultTableModel
                tableModel.setColumnIdentifiers(data.columnNames)

                BGLoadUnloadedNodes.createAndRun(serviceManager, data.unloadedNodes) {
                    setRelationTableData(data.relationsData)
                    view.tabPanel.selectedIndex = TAB_PANEL_RESULTS_INDEX // Open the result tab.
                    // Try the darnest to make the window appear on top!
                    Utility.fightForFocus(view.mainFrame)
                }
            }
            val iterator = TaskIterator(query)
            serviceManager.taskManager.execute(iterator)
        }
    }

    private fun addMultiQueryLine() {
        view.addMultiQueryLine()
    }


    private fun selectUpstreamRelations() {
        // Keep working with the model's indices, translate from table immediately.
        view.clearFilterField()

        val selectedRows = view.resultTable.selectedRows.map { view.resultTable.convertRowIndexToModel(it) }.toHashSet()

        var upstreamRelationRows = findUpstreamsRelations(selectedRows).toHashSet()

        var newRowCount = upstreamRelationRows.size
        var infiniteLoopLimiter = 100 // In case something bad happens, it will only loop 100 times before giving up.

        var newRows = upstreamRelationRows

        while (newRowCount != 0 && infiniteLoopLimiter-- > 0) {
            newRows = findUpstreamsRelations(newRows).toHashSet()
            newRowCount = newRows.subtract(upstreamRelationRows).size
            upstreamRelationRows = upstreamRelationRows.union(newRows).toHashSet()
        }

        for (modelRow in upstreamRelationRows) {
            val row = view.resultTable.convertRowIndexToView(modelRow)
            view.resultTable.addRowSelectionInterval(row, row)
        }
    }

    private fun findUpstreamsRelations(selectedRows: Collection<Int>): Collection<Int> {
        var matchingRows = ArrayList<Int>()
        for (rowNumber in selectedRows) {
            val resultRow = currentResultsInTable[rowNumber]
            val relation = (resultRow as? BGRelationResultRow)?.relation
            if (relation != null) {
                val fromNodeUri = relation.fromNode.uri
                matchingRows.addAll(getRowsWithRelationsTo(fromNodeUri))
                }
        }
        return matchingRows
    }

    // These rows are in the Model space. Must be translated to use in Table space.
    private fun getRowsWithRelationsTo(uri: String): Collection<Int> {
        var matchingRows = ArrayList<Int>()
        for (row in currentResultsInTable.keys) {
            val relation = (currentResultsInTable[row] as? BGRelationResultRow)?.relation
            if (relation != null) {
                if (relation.toNode.uri == uri) {
                    matchingRows.add(row)
                }
            }
        }
        return matchingRows
    }

    /*

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


     */

    override fun actionPerformed(e: ActionEvent) {
        when (e.actionCommand) {
            ACTION_RUN_QUERY -> runQuery()
            ACTION_CHANGED_QUERY -> updateSelectedQuery()
            ACTION_IMPORT_TO_SELECTED -> {
                val network = serviceManager.applicationManager.currentNetwork
                importSelectedResults(network, currentQuery!!.returnType)
            }
            ACTION_IMPORT_TO_NEW -> importSelectedResults(null, currentQuery!!.returnType)
            ACTION_ADD_MULTIQUERY_LINE -> addMultiQueryLine()
            ACTION_RUN_MULTIQUERY -> runMultiQuery()
            ACTION_GENERATE_SPARQL -> {
                if (generateSPARQLCode() != null) view.tabPanel.selectedIndex = TAB_PANEL_SPARQL_INDEX
            }
            ACTION_PARSE_SPARQL -> parseSPARQLCode()
            ACTION_WRITE_SPARQL -> saveSPARQLToFile()
            ACTION_LOAD_SPARQL -> loadSPARQLFromFile()
            ACTION_FILTER_EDGES_TO_EXISTING -> {
                val box = e.source as? JCheckBox ?: throw Exception("Expected JCheckBox!")
                filterRelationsToNodesInCurrentNetwork(box.isSelected)
            }
            ACTION_SELECT_UPSTREAM_RELATIONS -> selectUpstreamRelations()
            else -> {
            }
        }
    }


    companion object {
        val ACTION_CHANGED_QUERY = "changedQueryComboBox"
        val ACTION_RUN_QUERY = "runBiogwQuery"
        val ACTION_IMPORT_TO_SELECTED = "importToSelectedNetwork"
        val ACTION_IMPORT_TO_NEW = "importToNewNetwork"
        val ACTION_GENERATE_SPARQL = "generateSPARQL"
        val ACTION_PARSE_SPARQL = "parseSPARQL"
        val ACTION_LOAD_SPARQL = "loadSPARQLFromFile"
        val ACTION_WRITE_SPARQL = "writeSPARQLToFile"
        val ACTION_RUN_MULTIQUERY = "runMultiQuery"
        val ACTION_ADD_MULTIQUERY_LINE = "addMultiRelation"
        val ACTION_SELECT_UPSTREAM_RELATIONS = "selectUpstreamRelations"
        val UNIPROT_PREFIX = "http://identifiers.org/uniprot/"
        val ONTOLOGY_PREFIX = "http://purl.obolibrary.org/obo/"
        val ACTION_FILTER_EDGES_TO_EXISTING = "filter relations to exsisting nodes"
        val TAB_PANEL_BUILD_QUERY_INDEX = 0
        val TAB_PANEL_PREDEFINED_INDEX = 1
        val TAB_PANEL_SPARQL_INDEX = 2
        val TAB_PANEL_RESULTS_INDEX = 3
    }
}
