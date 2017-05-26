package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.query.QueryParameter
import org.cytoscape.biogwplugin.internal.query.QueryTemplate
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
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.ArrayList

/**
 * Created by sholmas on 23/05/2017.
 */


class BGQueryBuilderController(private val serviceManager: BGServiceManager) : ActionListener, ChangeListener {
    private val view: BGCreateQueryView

    private var currentQuery: QueryTemplate? = null
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
    }

    private fun readParameterComponents() {
        for (parameter in currentQuery!!.parameters) {
            val component = view.getParameterComponents()[parameter.id]

            when (parameter.type) {
                QueryParameter.ParameterType.TEXT -> parameter.value = (component as JTextField).text.sanitizeParameter()
                QueryParameter.ParameterType.CHECKBOX -> parameter.value = if ((component as JCheckBox).isSelected) "true" else "false"
                QueryParameter.ParameterType.COMBOBOX -> {
                    val box = component as JComboBox<String>
                    val selected = box.selectedItem as String
                    parameter.value = parameter.options[selected]
                }
                QueryParameter.ParameterType.UNIPROT_ID -> {
                    var uniprotID = (component as JTextField).text
                    if (!uniprotID.startsWith(UNIPROT_PREFIX)) {
                        uniprotID = UNIPROT_PREFIX + uniprotID
                    }
                    parameter.value = uniprotID.sanitizeParameter()
                }
                QueryParameter.ParameterType.ONTOLOGY -> {
                    var ontology = (component as JTextField).text
                    if (!ontology.startsWith(ONTOLOGY_PREFIX)) {
                        ontology = ONTOLOGY_PREFIX + ontology
                    }
                    parameter.value = ontology.sanitizeParameter()
                }
                QueryParameter.ParameterType.OPTIONAL_URI -> {
                    val uri = (component as JTextField).text
                    parameter.value = uri
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
        try {
            val inputStream = FileInputStream(openFileChooser()!!)
            parseXMLFile(inputStream)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    private fun parseXMLFile(inputStream: FileInputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createQuery() {

    }

    private fun runQuery() {
        if (validatePropertyFields()) {
            readParameterComponents()
            val queryString = createQueryString(currentQuery!!)
            view.sparqlTextArea.text = queryString

            /*
            val query = BGNodeSearchQuery(SERVER_PATH, queryString, serviceManager)
            val callback = {
                val tableModel = view.resultTable.model as DefaultTableModel

                // Clear all rows before adding new data. Delete from the back to the front.
                for (i in tableModel.rowCount - 1 downTo -1 + 1) {
                    tableModel.removeRow(i)
                }

                for (node in query.returnData) {
                    val row = arrayOf(node.commonName, node.URI)
                    tableModel.addRow(row)
                }
                view.tabPanel.selectedIndex = 2
                EventQueue.invokeLater {
                    view.mainFrame.toFront()
                    view.mainFrame.isAlwaysOnTop = true
                    view.mainFrame.isAlwaysOnTop = false
                    view.mainFrame.requestFocus()
                }
            }
            // TODO: FIX CALLBACK!
            //query.addCallback()

            val iterator = TaskIterator(query)
            serviceManager.taskManager.execute(iterator)
            */
        } else {
            JOptionPane.showMessageDialog(view.mainFrame, "All text fields must be filled out!")
        }
    }

    private fun createQueryString(currentQuery: QueryTemplate): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun importSelectedResults(network: CyNetwork?) {
        var network = network

        // 1. Get the selected lines from the table.
        val selectedURIs = ArrayList<String>()
        val model = view.resultTable.model as DefaultTableModel
        for (row in view.resultTable.selectedRows) {
            val uri = model.getValueAt(row, 1) as String
            selectedURIs.add(uri)
        }

        // 2. The nodes have already been fetched. There should be a cache storing them somewhere.

    }


    private fun validatePropertyFields(): Boolean {
        for (parameter in currentQuery!!.parameters) {
            val component = view.getParameterComponents()[parameter.id]
            if (component is JTextField) {
                if (parameter.type === QueryParameter.ParameterType.OPTIONAL_URI) {
                    val field = component
                    val uri = field.text
                    if (uri.sanitizeParameter().isEmpty()) {
                        // Empty string.
                        field.text = "?" + parameter.id
                    } else if (uri.startsWith("?")) {
                        field.text = uri.sanitizeParameter()
                    } else {
                        // Validate the URI.
                        val validated = Utility.instance.validateURI(uri) // UGLY HACK! Should be asynchronous:
                        if (!validated) {
                            return false
                        }
                    }
                } else {
                    val field = component
                    field.text = Utility.instance.sanitizeParameter(field.text)
                    if (field.text.isEmpty()) {
                        return false
                    }
                }
            }
        }
        return true
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
                importSelectedResults(network)
            }
            ACTION_IMPORT_TO_NEW -> importSelectedResults(null)
            else -> {
            }
        }
    }

    override fun stateChanged(e: ChangeEvent) {

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
        public val CHANGE_TAB_CHANGED = "tabbedPaneHasChanged"
        public val UNIPROT_PREFIX = "http://identifiers.org/uniprot/"
        public val ONTOLOGY_PREFIX = "http://purl.obolibrary.org/obo/"
    }
}
