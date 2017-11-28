package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.query.BGNodeFetchQuery
import org.cytoscape.biogwplugin.internal.query.BGNodeURILookupQuery
import org.cytoscape.biogwplugin.internal.query.BGParsingType
import org.cytoscape.biogwplugin.internal.query.BGReturnNodeData
import org.cytoscape.biogwplugin.internal.util.Utility
import java.awt.EventQueue
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.table.DefaultTableModel

class BGNodeLookupController(val serviceManager: BGServiceManager, parentComponent: JComponent?, defaultURI: String? = null, val completion: (BGNode?) -> Unit): ActionListener {

    private val view = BGNodeLookupView(this, parentComponent)
    private var nodesFound = HashMap<String, BGNode>()

    init {
        val table = view.resultTable.model as DefaultTableModel
        defaultURI?.let {
            view.searchField.text = it
            view.nameOrURIComboBox.selectedIndex = 1
        }
        val columnNames = arrayOf("Node URI", "Common Name", "Description", "Taxon")
        table.setColumnIdentifiers(columnNames)
    }

    private fun loadResultsIntoTable(nodesFound: HashMap<String, BGNode>) {
        val tableModel = view.resultTable.model as DefaultTableModel

        for (i in tableModel.rowCount -1 downTo 0) {
            tableModel.removeRow(i)
        }

        this.nodesFound = nodesFound
        for (result in nodesFound.values) {
            tableModel.addRow(result.nameStringArray())
        }

        // GIVE ME FOREGROUND! (Can't believe Cytoscape is so difficult at this...)
        EventQueue.invokeLater {
            view.mainFrame.toFront()
            view.mainFrame.isAlwaysOnTop = true
            view.mainFrame.isAlwaysOnTop = false
            view.mainFrame.requestFocus()
        }
    }

    private fun searchForNodes() {

        val searchString = view.searchField.text
        val useRegex = view.regexCheckBox.isSelected
        //val nodeType = view.nodeTypeComboBox.selectedItem as? BGNodeType ?:

        val nodeType = when (view.nodeTypeComboBox.selectedItem as String) {
            "Protein" -> BGNodeType.Protein
            "Gene" -> BGNodeType.Gene
            "GO Term" -> BGNodeType.GO
            "Taxon" -> BGNodeType.Taxon
            else -> BGNodeType.Undefined
        }

        when (view.nameOrURIComboBox.selectedIndex) {
            0 -> {
                // Label search
                val query = BGNodeURILookupQuery(serviceManager, searchString, useRegex, nodeType)
                query.addCompletion {
                    val data = it as? BGReturnNodeData ?: return@addCompletion
                    if (data.nodeData.count() == 0) {
                        JOptionPane.showMessageDialog(view.mainFrame, "No entities found.")
                    }
                    loadResultsIntoTable(data.nodeData)
                }
                // TODO: Use the built-in task manager?
                query.run()
            }
            1 -> {
                // URI Lookup
                if (searchString.startsWith("http://")){
                    lookupURIString(searchString)
                } else {
                    JOptionPane.showMessageDialog(view.mainFrame, "Invalid URI!")
                }
            }
            2 -> {
                // UniprotID lookup:
                lookupURIString(Utility.generateUniprotURI(searchString))
            }
            3 -> {
                // GO term lookup:
                lookupURIString(Utility.generateGOTermURI(searchString))
            }
            else -> {}
        }
    }

    private fun lookupURIString(searchString: String) {
        val query = BGNodeFetchQuery(serviceManager, searchString, serviceManager.server.parser, BGReturnType.NODE_LIST_DESCRIPTION)
        query.parseType = BGParsingType.TO_ARRAY
        query.addCompletion {
            val data = it as? BGReturnNodeData ?: return@addCompletion
            if (data.nodeData.count() == 0) {
                JOptionPane.showMessageDialog(view.mainFrame, "No entities found.")
            }
            loadResultsIntoTable(data.nodeData)
        }
        query.run()
    }

    private fun useSelectedNode() {
        val selectedRows = view.resultTable.selectedRows

        if (selectedRows.size == 0) {
            return
        }
        if (selectedRows.size > 1) {
            throw Exception("Multiple rows selected! Unable to pick one.")
        }
        val row = view.resultTable.convertRowIndexToModel(selectedRows.get(0))
        val uri = view.resultTable.model.getValueAt(row, 0)

        val node = nodesFound[uri] ?: throw Exception("Invalid node! Node not found in return data!")
        completion(node)
        view.mainFrame.dispatchEvent(WindowEvent(view.mainFrame, WindowEvent.WINDOW_CLOSING))
    }

    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            if (e.actionCommand == BGNodeLookupView.ACTION_SELECT_NODE) {
                useSelectedNode()
            }
            if (e.actionCommand == BGNodeLookupView.ACTION_SEARCH) {
                searchForNodes()
            }
        }
    }
}