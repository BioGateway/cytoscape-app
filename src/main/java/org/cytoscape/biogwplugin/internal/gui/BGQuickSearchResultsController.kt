package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.table.DefaultTableModel

class BGQuickSearchResultsController(val serviceManager: BGServiceManager, private val nodesFound: HashMap<String, BGNode>, val completion: (BGNode) -> Unit) : ActionListener {

    private val view = BGQuickSearchResultsView(this)

    init {
        val table = view.searchResultsTable.model as DefaultTableModel
        val columnNames = arrayOf("Node URI", "Common Name", "Description", "Taxon")
        table.setColumnIdentifiers(columnNames)
        for (result in nodesFound.values) {
            table.addRow(result.nameStringArray())
        }
    }


    private fun useSelectedNode() {
        val selectedRows = view.searchResultsTable.selectedRows

        if (selectedRows.size == 0) {
            return
        }
        if (selectedRows.size > 1) {
            throw Exception("Multiple rows selected! Unable to pick one.")
        }
        val row = selectedRows.get(0)
        val uri = view.searchResultsTable.model.getValueAt(row, 0)
        val node = nodesFound[uri] ?: throw Exception("Invalid node! Node not found in return data!")
        completion(node)
    }

    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            if (e.actionCommand == BGQuickSearchResultsView.ACTION_SELECT_NODE) {
                useSelectedNode()
            }
        }
    }
}