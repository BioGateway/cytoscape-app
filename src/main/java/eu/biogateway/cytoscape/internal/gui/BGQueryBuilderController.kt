package eu.biogateway.cytoscape.internal.gui

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.gui.multiquery.BGAutocompleteComboBox
import eu.biogateway.cytoscape.internal.model.*
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import eu.biogateway.cytoscape.internal.parser.BGSPARQLParser
import eu.biogateway.cytoscape.internal.query.*
import eu.biogateway.cytoscape.internal.server.BGSuggestion
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Utility
import eu.biogateway.cytoscape.internal.util.sanitizeParameter
import org.cytoscape.model.CyNetwork
import org.cytoscape.work.TaskIterator
import java.awt.Color
import java.awt.EventQueue
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

interface BGRelationResultViewTooltipDataSource {
    fun getTooltipForResultRowAndColumn(row: Int, column: Int): String?
}

class BGOptionalURIField(val textField: JTextField): JPanel() {
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
            val lookupController = BGNodeLookupController(this) {
                if (it != null) {
                    textField.text = it.uri
                    textField.toolTipText = it.description
                }
            }
        }
        this.add(uriSearchButton)
    }
}

class BGRelationQueryRow(relationTypes: Array<String>): JPanel() {
    val fromNodeField = BGOptionalURIField(JTextField())
    val toNodeField = BGOptionalURIField(JTextField())
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

class BGQueryBuilderController() : ActionListener, ChangeListener, BGRelationResultViewTooltipDataSource {

    override fun getTooltipForResultRowAndColumn(row: Int, column: Int): String? {
        val modelRow = view.resultTable.convertRowIndexToModel(row)
        val relationRow = currentResultsInTable.get(modelRow)
        val relation = (relationRow as? BGRelationResultRow)?.relation
        relation?.let {
            if (column == 0) return it.fromNode.description
            if (column == 1) return it.relationType.description
            if (column == 2) return it.toNode.description
        }
        return null
    }

    private var preferences = Preferences.userRoot().node(javaClass.name)
    private val importConfidenceValues = true
    private val view: BGQueryBuilderView

    //private var relationList = ArrayList<BGRelation>()

    private var currentQuery: BGQueryTemplate? = null
    private var currentQueryType: BGReturnType? = null

    private var currentReturnData: BGReturnData? = null
    //private var currentResultsInTable = HashMap<String, BGResultRow>()
    private var currentResultsInTable = HashMap<Int, BGResultRow>()
    private var currentBulkImportNodes = HashMap<Int, BGNode>()

    private var  queries = HashMap<String, BGQueryTemplate>()

    init {
        this.view = BGQueryBuilderView(this, this)
        this.queries = BGServiceManager.dataModelController.config.queryTemplates
        updateUIAfterXMLLoad()

        view.exampleQueryBox.addActionListener {
            val selected = view.exampleQueryBox.selectedItem as BGExampleQuery
            if (!selected.placeholder) {
                loadSPARQLString(selected.sparql)
            }
        }
    }

    private fun updateUIAfterXMLLoad() {
        view.querySelectionBox.removeAllItems()
        for (queryName in queries.keys) {
            view.querySelectionBox.addItem(queryName)
        }
        if (queries.size == 0) {
            view.tabPanel.remove(view.queryPanel)
        }
        view.exampleQueryBox.removeAllItems()
        view.exampleQueryBox.addItem(BGExampleQuery("Load example query...", "", true))
        for (example in BGServiceManager.config.exampleQueries) {
            view.exampleQueryBox.addItem(example)
        }
        view.bulkImportTypeComboBox.removeAllItems()
        for (searchType in BGServiceManager.config.searchTypes) {
            view.bulkImportTypeComboBox.addItem(searchType)
        }

        view.setUpMultiQueryPanel()

    }

    /*
    private fun setupMultiQueryPanel() {
        val panel = BGMultiQueryPanel(serviceManager)
        panel.addQueryLine()
        view.setUpMultiQueryPanel(panel)
    }
    */

    private fun readParameterComponents(parameters: Collection<BGQueryParameter>, parameterComponents: HashMap<String, JComponent>) {
        for (parameter in parameters) {
            val component = parameterComponents[parameter.id]

            when (parameter.type) {
                BGQueryParameter.ParameterType.TEXT -> parameter.value = (component as JTextField).text.sanitizeParameter()
                BGQueryParameter.ParameterType.CHECKBOX -> parameter.value = if ((component as JCheckBox).isSelected) "true" else "false"
                BGQueryParameter.ParameterType.COMBOBOX -> {
                    // This should be a checked cast, as it *SHOULD* throw an exception if the cast fails...
                    @Suppress("UNCHECKED_CAST")
                    val box: JComboBox<String> = component as? JComboBox<String> ?: throw Exception("Component "+component+" expected to be JComboBox<String>!")
                    val selected = box.selectedItem as String
                    parameter.value = parameter.options[selected]
                }
                BGQueryParameter.ParameterType.RELATION_COMBOBOX -> {
                    val field = component as BGRelationTypeField
                    val selected = field.combobox.selectedItem as String
                    parameter.value = parameter.options[selected]
                    parameter.direction = field.direction
                }
                BGQueryParameter.ParameterType.PROTEIN, BGQueryParameter.ParameterType.GO_TERM, BGQueryParameter.ParameterType.TAXON, BGQueryParameter.ParameterType.GENE -> {
                    val searchField = (component as? BGAutocompleteComboBox) ?: throw Exception("Expected component to be BGAutocompleteComboBox!")
                    parameter.value = searchField.selectedUri?.sanitizeParameter()
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
                            "Protein" -> Constants.PROT_PREFIX
                            "Gene" -> Constants.GENE_PREFIX
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
                    BGNodeSearchQuery(queryString, queryType)
                }
                BGReturnType.RELATION_TRIPLE_GRAPHURI, BGReturnType.RELATION_TRIPLE_NAMED -> {
                    BGRelationQueryImplementation(queryString, queryType)
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
                    BGReturnType.RELATION_TRIPLE_GRAPHURI, BGReturnType.RELATION_TRIPLE_NAMED -> {
                        it as? BGReturnRelationsData ?: throw Exception("Expected Relation Data in return!")
                    }
                    else -> {
                        throw Exception("Unexpected query type: " + queryType.toString())
                    }
                }
                data.filterWith(BGServiceManager.config.activeNodeFilters)
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
                    BGLoadUnloadedNodes.createAndRun(data.unloadedNodes) {
                        setRelationTableData(data.relationsData)
                        view.tabPanel.selectedComponent = view.resultPanel// Open the result tab.
                        Utility.fightForFocus(view.mainFrame)
                    }
                    return@addCompletion
                } else {
                    view.tabPanel.selectedComponent = view.resultPanel// Open the result tab.
                    Utility.fightForFocus(view.mainFrame)
                }
            }
            val iterator = TaskIterator(query)
            BGServiceManager.taskManager?.execute(iterator)

        }
    }

    private fun createQueryString(currentQuery: BGQueryTemplate): String? {
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

    private fun importSelectedResults(net: CyNetwork?) {
        var network = net // Need to redeclare it to make it mutable.
        val server = BGServiceManager.dataModelController
        // 1. Get the selected lines from the table.
        val nodes = HashMap<String, BGNode>()
        val relations = ArrayList<BGRelation>()
//        val model = view.resultTable.model as DefaultTableModel

        val returnType = currentQuery?.returnType ?: currentQueryType

        for (rowNumber in view.resultTable.selectedRows) {

            val resultRow = currentResultsInTable[view.resultTable.convertRowIndexToModel(rowNumber)]

            when(returnType) {
                BGReturnType.NODE_LIST, BGReturnType.NODE_LIST_DESCRIPTION, BGReturnType.NODE_LIST_DESCRIPTION_TAXON -> {
                    val node = (resultRow as? BGNodeResultRow)?.node ?: throw Exception("Result must be a node!")
                    nodes.put(node.uri, node)
                }
                BGReturnType.RELATION_TRIPLE_GRAPHURI, BGReturnType.RELATION_TRIPLE_NAMED, BGReturnType.RELATION_MULTIPART -> {
                    val relation = (resultRow as? BGRelationResultRow)?.relation ?: throw Exception("Result must be a relation!")
                    nodes.put(relation.fromNode.uri, relation.fromNode)
                    nodes.put(relation.fromNode.uri, relation.fromNode)
                    relations.add(relation)
                }
                else -> {
                }
            }
        }

        if (nodes.isEmpty()) return

        var shouldCreateNetworkView = false

        if (network == null) {
            network = server.networkBuilder.createNetwork()
            shouldCreateNetworkView = true
        }

        fun buildNetwork() {
            when (returnType) {
                BGReturnType.NODE_LIST, BGReturnType.NODE_LIST_DESCRIPTION, BGReturnType.NODE_LIST_DESCRIPTION_TAXON -> {
                    server.networkBuilder.addBGNodesToNetwork(nodes.values, network)
                }
                BGReturnType.RELATION_TRIPLE_GRAPHURI, BGReturnType.RELATION_TRIPLE_NAMED, BGReturnType.RELATION_MULTIPART -> {
                    server.networkBuilder.addRelationsToNetwork(network, relations)
                }
                else -> {
                }
            }

            if (shouldCreateNetworkView) {
                BGServiceManager.networkManager?.addNetwork(network)
                EventQueue.invokeLater {
                    server.networkBuilder.createNetworkView(network)
                }
            } else {
                Utility.reloadCurrentVisualStyleCurrentNetworkView()
            }
        }

        /*
        if (importConfidenceValues) {
            val searchRelations = relations.filter { it.relationType.identifier.equals("intact:http://purl.obolibrary.org/obo/RO_0002436") }
            val query = BGFetchConfidenceValues(serviceManager, "Loading confidence values...", searchRelations)
            query.completion = {
                buildNetwork()
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
        } else {
            buildNetwork()
        }
        */

        val query = BGLoadRelationMetadataQuery(relations, BGServiceManager.config.activeEdgeMetadataTypes) {
            buildNetwork()
            BGServiceManager.dataModelController.networkBuilder.reloadMetadataForNodesInNetwork(network)
        }
        BGServiceManager.execute(query)
    }

    private fun validatePropertyFields(parameters: Collection<BGQueryParameter>, parameterComponents: HashMap<String, JComponent>): String? {
        for (parameter in parameters) {
            val component = parameterComponents[parameter.id]
            if (component == null) {
                return "Component not found!"
            } else if (!component.isEnabled) {
                continue
            } else if (parameter.type === BGQueryParameter.ParameterType.OPTIONAL_URI) {
                val optionalUriField = component as? BGOptionalURIField ?: throw Exception("Invalid component type!")
                val uri = optionalUriField.textField.text
                if (uri.sanitizeParameter().isEmpty()) {
                    // Empty string.
                    optionalUriField.textField.text = "?" + parameter.id
                } else if (uri.startsWith("?")) {
                    optionalUriField.textField.text = uri.sanitizeParameter()
                } else if (component is JTextField) {
                    val field = component
                    field.text = Utility.sanitizeParameter(field.text)
                    if (field.text.isEmpty()) {
                        return "All required text fields must be filled out!"
                    }
                } else if (component is BGAutocompleteComboBox) {
                    val searchBox = component
                    if (searchBox.selectedUri.isNullOrBlank()) {
                        return "All required text fields must be filled out!"
                    }
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
            val fromUri = line.fromUri ?: return "The from field can not be left blank when not using variables."
            val toUri = line.toUri ?: return "The to field can not be left blank when not using variables."

            if (!fromUri.startsWith("?") && !fromUri.startsWith("http://")) return "The From entity is invalid."
            if (!toUri.startsWith("?") && !toUri.startsWith("http://")) return "The To entity is invalid."
        }
        view.multiQueryPanel.validateNodeTypeConsistency()?.let { return it }
        BGServiceManager.controlPanel?.queryConstraintPanel?.validateConstraints()?.let { return it }
        return null
    }

    private fun filterRelationsToNodesInCurrentNetwork(fromFilter: Boolean, toFilter: Boolean) {

        val returnData = currentReturnData as? BGReturnRelationsData ?: return
        val relationsFound = returnData.relationsData

        if (fromFilter || toFilter) {
            val network = BGServiceManager.applicationManager?.currentNetwork
            val allNodeUris = network?.defaultNodeTable?.getColumn(Constants.BG_FIELD_IDENTIFIER_URI)?.getValues(String::class.java)
            var relations = ArrayList<BGRelation>()
            for (result in relationsFound) {
                if (allNodeUris != null) {
                    if (toFilter && fromFilter) {
                        if (allNodeUris.contains(result.toNode.uri) && allNodeUris.contains(result.fromNode.uri)) {
                            relations.add(result)
                        }
                    } else if (toFilter && allNodeUris.contains(result.toNode.uri) || fromFilter && allNodeUris.contains(result.fromNode.uri)) {
                        relations.add(result)
                    }
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
            var fromNodeName = relation.fromNode.name ?: relation.fromNode.uri
            relation.fromNode.taxon?.name?.let { taxon ->
                fromNodeName += " - $taxon"
            }
            val relationName = relation.relationType.name
            var toNodeName = relation.toNode.name ?: relation.toNode.uri
            relation.toNode.taxon?.name?.let { taxon ->
                toNodeName += " - $taxon"
            }
            val row = arrayOf(fromNodeName, relationName, toNodeName)
            tableModel.addRow(row)
            currentResultsInTable[tableModel.rowCount-1] = BGRelationResultRow(relation)
        }
    }

    private fun parseSPARQLCode() {

        val sparqlCode = view.sparqlTextArea.text
        if (sparqlCode.isEmpty()) return

        val queryGraphs = BGSPARQLParser.parseSPARQLCode(sparqlCode, BGServiceManager.config.relationTypeMap)

        if (queryGraphs.first.isEmpty()) {
            JOptionPane.showMessageDialog(view.mainFrame, "Unable to parse any queries from current SPARQL.")
        }
        view.multiQueryPanel.loadQueryGraphs(queryGraphs)
        view.tabPanel.selectedComponent = view.buildQueryPanel

    }

    private fun convertToSharingSPARQLSnippet() {
        val errorText = validateMultiQuery()
        if (errorText != null) {
            JOptionPane.showMessageDialog(view.mainFrame, errorText)
        } else {
            val queryString = view.multiQueryPanel.generateSimplifiedSPARQL()
            view.sparqlTextArea.text = queryString
        }
    }

    override fun stateChanged(e: ChangeEvent?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadSPARQLFromFile() {
        val file = openFileChooser() ?: return
        val sparqlCode = file.readText()
        loadSPARQLString(sparqlCode)
    }

    private fun loadSPARQLString(sparqlCode: String) {
        val queryGraphs = BGSPARQLParser.parseSPARQLCode(sparqlCode, BGServiceManager.config.relationTypeMap)
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

    fun openFileChooser(): File? {
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

    private fun runBulkImport() {
        var queryType: BGSearchType

        var nodeList = view.bulkImportTextPane.text.split("\n")
        nodeList = nodeList.map { Utility.sanitizeParameter(it) }.filter { it.isNotEmpty() }

        if (nodeList.size > Constants.BG_BULK_IMPORT_WARNING_LIMIT) {
            val message = "Bulk importing this many nodes at once might take a long time, or not succeed. \n Consider to import fewer nodes in each step. \n\nAre you sure you want to continue?"
            val response = JOptionPane.showOptionDialog(null, message, "Proceed with bulk import?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null)
            if (response != JOptionPane.OK_OPTION) {
                return
            }
        }

        val selectedType = view.bulkImportTypeComboBox.selectedItem as? BGSearchType

        selectedType?.let {
            val suggestions = BGServiceManager.endpoint.findNodesForSearchType(nodeList, it)
            val nodes = suggestions.map { BGNode(it) }
            setBulkImportTableData(nodes)
            setBulkImportInputPaneColors(suggestions, selectedType)
            Utility.fightForFocus(view.mainFrame)
        }

        /*
        val nodeType: BGNodeType = when (selectedType) {
            "Gene Symbols" -> {
                queryType = QueryType.GENE_SYMBOL
                BGServiceManager.config.nodeTypes.get("gene") ?: throw Exception("Invalid node type!")
            }
            "Protein names" -> {
                queryType = QueryType.NAME_SEARCH
                BGServiceManager.config.nodeTypes.get("protein") ?: throw Exception("Invalid node type!")
            }
            "Uniprot IDs" -> {
                queryType = QueryType.UNIPROT_LOOKUP
                BGServiceManager.config.nodeTypes.get("protein") ?: throw Exception("Invalid node type!")
            }
            "Entrez Gene IDs" -> {
                queryType = QueryType.ENTREZ_LOOKUP
                BGServiceManager.config.nodeTypes.get("gene") ?: throw Exception("Invalid node type!")
            }
            "ENSEMBL IDs" -> {
                queryType = QueryType.ENSEMBL_SEARCH
                BGServiceManager.config.nodeTypes.get("gene") ?: throw Exception("Invalid node type!")
            }
            "GO terms" -> {
                queryType = QueryType.GO_LOOKUP
                BGServiceManager.config.nodeTypes.get("go_term") ?: throw Exception("Invalid node type!")
            }
            else -> {
                //BGNodeType.Undefined
                throw Exception("Invalid node type!")
            }
        }

        val queryCompletion: (BGReturnData?) -> Unit = {
            val data = it as? BGReturnNodeData ?: throw Exception("Expected Node Data in return!")
            val nodes = data.nodeData.values
            setBulkImportTableData(nodes)
            setBulkImportInputPaneColors(nodes.map { BGSuggestion(it) }, queryType)
            Utility.fightForFocus(view.mainFrame)
        }

        when (queryType) {
            QueryType.GENE_SYMBOL -> {
//                val query = BGMultiNodeFetchMongoQuery(nodeList, "genesForSymbols", null, null, BGReturnType.NODE_LIST_DESCRIPTION_STATUS)
//                query.addCompletion(queryCompletion)
//                BGServiceManager.execute(query)
                searchForSuggestionsForField(nodeList, nodeType, "prefLabel") {
                    setBulkImportTableData(it.map { BGNode(it) })
                    setBulkImportInputPaneColors(it, queryType)
                    Utility.fightForFocus(view.mainFrame)
                }
            }
            QueryType.NAME_SEARCH -> {
                val query = BGBulkImportNodesQuery(nodeList, nodeType)
                query.addCompletion(queryCompletion)
                BGServiceManager.taskManager?.execute(TaskIterator(query))
            }
            BGQueryBuilderController.QueryType.UNIPROT_LOOKUP -> {
                val uniprotNodeList = nodeList.map { Utility.generateUniprotURI(it) }
                val query = BGBulkImportNodesFromURIs(nodeType, uniprotNodeList)
                query.addCompletion(queryCompletion)
                BGServiceManager.taskManager?.execute(TaskIterator(query))
            }
            BGQueryBuilderController.QueryType.GO_LOOKUP -> {
                val goNodeList = nodeList.map { Utility.generateGOTermURI(it) }
                val query = BGBulkImportNodesFromURIs(nodeType, goNodeList)
                query.addCompletion(queryCompletion)
                BGServiceManager.taskManager?.execute(TaskIterator(query))
            }
            BGQueryBuilderController.QueryType.ENTREZ_LOOKUP -> {
//                val entrezNodesList = nodeList.map { Utility.generateEntrezURI(it) }
//                val query = BGBulkImportNodesFromURIs(nodeType, entrezNodesList)
//                query.addCompletion(queryCompletion)
//                BGServiceManager.taskManager?.execute(TaskIterator(query))

                searchForSuggestionsForField(nodeList, nodeType, "ncbi_id") {
                    setBulkImportTableData(it.map { BGNode(it) })
                    setBulkImportInputPaneColors(it, queryType)
                    Utility.fightForFocus(view.mainFrame)
                }

            }
            BGQueryBuilderController.QueryType.ENSEMBL_SEARCH -> {
//                val query = BGBulkImportENSEMBLNodesQuery(nodeList, nodeType)
//                query.addCompletion(queryCompletion)
//                BGServiceManager.taskManager?.execute(TaskIterator(query))

                searchForEnsembleIDs(nodeList, nodeType) {
                    setBulkImportTableData(it.map { BGNode(it) })
                    setBulkImportInputPaneColors(it, queryType)
                    Utility.fightForFocus(view.mainFrame)
                }
            }
            BGQueryBuilderController.QueryType.NOT_SET -> {
                throw Exception("Invalid query type!")
            }
        }

         */
    }

    private fun searchForEnsembleIDs(enembleIds: Collection<String>, type: BGNodeType, completion: (Collection<BGSuggestion>) -> Unit) {
        val results = ArrayList<BGSuggestion>()
        for (id in enembleIds) {
            val suggestions = BGServiceManager.endpoint.getSuggestionsForFieldValue("ensembl_id", id, type.id.toLowerCase())
            results.addAll(suggestions)
        }
        completion(results)
    }

    private fun searchForSuggestionsForField(values: Collection<String>, type: BGNodeType, field: String, completion: (Collection<BGSuggestion>) -> Unit) {
        val results = ArrayList<BGSuggestion>()
        for (id in values) {
            val suggestions = BGServiceManager.endpoint.getSuggestionsForFieldValue(field, id, type.id.toLowerCase(), 100)
            results.addAll(suggestions)
        }
        completion(results)
    }


    private fun bulkImportToNetwork(currentNetwork: CyNetwork? = null) {
        var network = currentNetwork

        val nodes = HashMap<String, BGNode>()
        for (rowNumber in view.bulkImportResultTable.selectedRows) {
            val node = currentBulkImportNodes.get(view.bulkImportResultTable.convertRowIndexToModel(rowNumber))
            if (node != null) {
                nodes.put(node.uri, node)
            }
        }

        if (nodes.isEmpty()) return

        var shouldCreateNetworkView = false

        if (network == null) {
            network = BGServiceManager.dataModelController.networkBuilder.createNetwork()
            shouldCreateNetworkView = true
        }

        BGServiceManager.dataModelController.networkBuilder.addBGNodesToNetwork(nodes.values, network)

        if (shouldCreateNetworkView) {
            BGServiceManager.networkManager?.addNetwork(network)
            EventQueue.invokeLater {
                network?.let {
                    BGServiceManager.dataModelController.networkBuilder.createNetworkView(it)
                }
            }
        } else {
            Utility.reloadCurrentVisualStyleCurrentNetworkView()
        }
    }

    private fun setBulkImportTableData(nodes: Collection<BGNode>) {
        val tableModel = view.bulkImportResultTable.model as DefaultTableModel
        tableModel.setColumnIdentifiers(arrayOf("Node Name", "Description", "Taxon", "URI"))

        currentBulkImportNodes.clear()
        for (i in tableModel.rowCount -1 downTo 0) {
            tableModel.removeRow(i)
        }
        for (node in nodes) {
            val nodeName = node.name ?: node.uri
            val description = node.description ?: ""
            val taxon = node.taxon?.name ?: "Unspecified"
            val row = arrayOf(nodeName, description, taxon, node.uri)
            tableModel.addRow(row)
            currentBulkImportNodes[tableModel.rowCount-1] = node
        }
    }

    private fun setBulkImportInputPaneColors(suggestions: Collection<BGSuggestion>, queryType: BGSearchType) {

        var numberOfMatches = 0
        val darkGreen = Color(34,139,34)
        val darkRed = Color(178,34,34)

        val nodeNames = suggestions.map { it.prefLabel ?: "" }.filter { !it.isEmpty() }.toHashSet()
        val nodeUris = suggestions.map { it._id }.toHashSet()
        val searchLines = view.bulkImportTextPane.text.split("\n").map { Utility.sanitizeParameter(it) }.filter { it.isNotEmpty() }
        // view.bulkImportTextPane.text = ""

        numberOfMatches = suggestions.size

        // TODO: This must be Refactored as the QueryType enum is deprecated.
        /*
        for (line in searchLines) {
            val match = when (queryType) {
                BGQueryBuilderController.QueryType.NAME_SEARCH -> nodeNames.contains(line)
                BGQueryBuilderController.QueryType.UNIPROT_LOOKUP -> nodeUris.contains(Utility.generateUniprotURI(line))
                BGQueryBuilderController.QueryType.GO_LOOKUP -> nodeUris.contains(Utility.generateGOTermURI(line))
                BGQueryBuilderController.QueryType.ENTREZ_LOOKUP -> suggestions.map { it.ncbi_id }.contains(line)
                BGQueryBuilderController.QueryType.ENSEMBL_SEARCH -> suggestions.map { it.ensembl_id }.contains(line)
                BGQueryBuilderController.QueryType.NOT_SET -> false
                BGQueryBuilderController.QueryType.GENE_SYMBOL -> nodeNames.contains(line)
            }

            if (match) {
                view.appendToPane(view.bulkImportTextPane, line+"\n", Color.BLACK)
                numberOfMatches++
            } else {
                view.appendToPane(view.bulkImportTextPane, line+"\n", darkRed)
            }
        } */
        view.bulkSearchResultLabel.text = "$numberOfMatches nodes found."
    }

    fun addMultiQueryLinesForURIs(uris: Collection<String>) {
        view.multiQueryPanel.addMultiQueryWithURIs(uris)
    }

    private fun runMultiQuery() {

        val errorText = validateMultiQuery()
        if (errorText != null) {
            JOptionPane.showMessageDialog(view.mainFrame, errorText)
        } else {

            val relationCount = Utility.countMatchingRowsQuery(view.multiQueryPanel.generateSPARQLCountQuery()) ?: throw Exception("Unable to get relation count.")

            if (relationCount > Constants.BG_RELATION_COUNT_WARNING_LIMIT) {
                val message = "Estimated "+relationCount.toString()+" relations must be evaluated to complete this query. This might take a very long time or time out. Are you sure you want to proceed?"
                val response = JOptionPane.showOptionDialog(null, message, "Proceed with query?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null)
                if (response != JOptionPane.OK_OPTION) {
                    return
                }
            }

            val queryString = view.multiQueryPanel.generateSPARQLQuery()
            view.sparqlTextArea.text = queryString

            val queryType = BGReturnType.RELATION_MULTIPART
            val query = BGMultiRelationsQuery(queryString, queryType)
            currentQueryType = queryType

            query.addCompletion {
                val data = it as? BGReturnRelationsData ?: throw Exception("Expected Relation Data in return!")
                currentReturnData = data

                val tableModel = view.resultTable.model as DefaultTableModel
                tableModel.setColumnIdentifiers(data.columnNames)

                BGLoadNodeDataFromBiogwDict.createAndRun(data.unloadedNodes, 500) {
                    setRelationTableData(data.relationsData)
                    view.mainFrame.title = "BioGateway Query Builder - "+data.relationsData.size+" relations found."
                    view.tabPanel.selectedComponent = view.resultPanel // Open the result tab.
                    // Try the darnest to make the window appear on top!
                    Utility.fightForFocus(view.mainFrame)
                }
            }
            val iterator = TaskIterator(query)
            BGServiceManager.taskManager?.execute(iterator)
        }
    }

    private fun addMultiQueryLine() {
        view.addMultiQueryLine()
    }

    private fun selectRelationsInPaths() {
        // Keep working with the model's indices, translate from table immediately.
        view.clearFilterField()


        // 1. Get the selected rows.
        val selectedRows = view.resultTable.selectedRows.map { view.resultTable.convertRowIndexToModel(it) }.toHashSet()

        (currentResultsInTable[1] as? BGRelationResultRow)?.let {
            it.relation
        }
        // 2. Get the relations of these rows.
        val selectedRelations = selectedRows.map { currentResultsInTable[it] as? BGRelationResultRow }
                .filterNotNull()
                .map { it.relation }

        // 3. Get all relations in the paths of these relations.
        val relationsInPath = selectedRelations.map {  it.paths.map { it.relations }.reduce { acc, hashSet -> acc.union(hashSet).toHashSet() }}
                .reduce { acc, hashSet ->  acc.union(hashSet).toHashSet()}

        // 4. Get all the rows of these relations
        val rowsInPath = currentResultsInTable.filter {
            val relationRow = it.value as? BGRelationResultRow
            relationsInPath.contains(relationRow?.relation)
        }

        // 5. Select those rows.
        for (modelRow in rowsInPath.keys) {
            val row = view.resultTable.convertRowIndexToView(modelRow)
            view.resultTable.addRowSelectionInterval(row, row)
        }


//        var upstreamRelationRows = findUpstreamsRelations(selectedRows).toHashSet()
//
//        var newRowCount = upstreamRelationRows.size
//        var infiniteLoopLimiter = 100 // In case something bad happens, it will only loop 100 times before giving up.
//
//        var newRows = upstreamRelationRows
//
//        while (newRowCount != 0 && infiniteLoopLimiter-- > 0) {
//            newRows = findUpstreamsRelations(newRows).toHashSet()
//            newRowCount = newRows.subtract(upstreamRelationRows).size
//            upstreamRelationRows = upstreamRelationRows.union(newRows).toHashSet()
//        }
//
//        for (modelRow in upstreamRelationRows) {
//            val row = view.resultTable.convertRowIndexToView(modelRow)
//            view.resultTable.addRowSelectionInterval(row, row)
//        }
    }

    private fun findRelationsInCommonPaths(selectedRows: Collection<Int>): Collection<Int> {
        var matchingRows = ArrayList<Int>()
        for (rowNumber in selectedRows) {
            val resultRow = currentResultsInTable[rowNumber]
            val relation = (resultRow as? BGRelationResultRow)?.relation
            if (relation != null) {
                val relationsInPath = relation.paths.fold(HashSet<BGRelation>(), {acc, bgPath ->  acc.union(bgPath.relations).toHashSet()})

//                val fromNodeUri = relation.fromNode.uri
//                matchingRows.addAll(getRowsWithRelationsTo(fromNodeUri))
            }
        }
        return matchingRows
    }


    private fun getIndexesForRelations(rows: Collection<Int>, relations: Collection<BGRelation>): Collection<Int> {
        var matchingRows = ArrayList<Int>()
        for (row in rows) {
            val resultRow = currentResultsInTable[row]
            val relation = (resultRow as? BGRelationResultRow)?.relation
            if (relations.contains(relation)) matchingRows.add(row)
        }
        return matchingRows
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

    override fun actionPerformed(e: ActionEvent) {
        when (e.actionCommand) {
            ACTION_RUN_QUERY -> runQuery()
            ACTION_CHANGED_QUERY -> updateSelectedQuery()
            ACTION_IMPORT_TO_SELECTED -> {
                val network = BGServiceManager.applicationManager?.currentNetwork
                importSelectedResults(network)
            }
            ACTION_IMPORT_TO_NEW -> importSelectedResults(null)
            ACTION_ADD_MULTIQUERY_LINE -> addMultiQueryLine()
            ACTION_RUN_MULTIQUERY -> runMultiQuery()
            ACTION_GENERATE_SPARQL -> {
                if (generateSPARQLCode() != null) view.tabPanel.selectedComponent = view.sparqlPanel
            }
            ACTION_PARSE_SPARQL -> parseSPARQLCode()
            ACTION_CLEAN_UP_SPARQL -> convertToSharingSPARQLSnippet()
            ACTION_WRITE_SPARQL -> saveSPARQLToFile()
            ACTION_LOAD_SPARQL -> loadSPARQLFromFile()
            ACTION_FILTER_EDGES_TO_EXISTING -> {
                val fromFilter = view.filterRelationsFROMExistingCheckBox.isSelected
                val toFilter = view.filterRelationsToExistingCheckBox.isSelected
                filterRelationsToNodesInCurrentNetwork(fromFilter, toFilter)
            }
            ACTION_SELECT_UPSTREAM_RELATIONS -> selectRelationsInPaths()
            ACTION_RUN_BULK_IMPORT -> runBulkImport()
            ACTION_BULK_IMPORT_TO_NEW_NETWORK -> bulkImportToNetwork()
            ACTION_BULK_IMPORT_TO_CURRENT_NETWORK -> bulkImportToNetwork(BGServiceManager.applicationManager?.currentNetwork)
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
        val ACTION_RUN_BULK_IMPORT = "runBulkImport"
        val ACTION_BULK_IMPORT_TO_NEW_NETWORK = "importSelectedFromBulkImportToNewNetwork"
        val ACTION_BULK_IMPORT_TO_CURRENT_NETWORK = "importSelectedFromBulkImportToCurrentNetwork"
        val ACTION_PARSE_SPARQL = "parseSPARQL"
        val ACTION_CLEAN_UP_SPARQL = "cleanUpSPARQL"
        val ACTION_LOAD_SPARQL = "loadSPARQLFromFile"
        val ACTION_WRITE_SPARQL = "writeSPARQLToFile"
        val ACTION_RUN_MULTIQUERY = "runMultiQuery"
        val ACTION_ADD_MULTIQUERY_LINE = "addMultiRelation"
        val ACTION_SELECT_UPSTREAM_RELATIONS = "selectRelationsInPaths"
        val ONTOLOGY_PREFIX = "http://purl.obolibrary.org/obo/"
        val ACTION_FILTER_EDGES_TO_EXISTING = "filter relations to exsisting nodes"
    }
}
