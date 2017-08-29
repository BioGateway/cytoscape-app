package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.model.CyNetwork
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.table.DefaultTableModel

class BGRelationSearchResultsController(val serviceManager: BGServiceManager, val results: ArrayList<BGRelation>, val columnNames: Array<String>, val network: CyNetwork) : ActionListener {

    private val view: BGRelationSearchResultsView

    init {
        this.view = BGRelationSearchResultsView(this)

        val table = view.resultTable.model as DefaultTableModel
        table.setColumnIdentifiers(columnNames)

        for (result in results) {
            table.addRow(result.nameStringArray())
        }
    }


    private fun importSelected() {
        val relations = ArrayList<BGRelation>()
        for (row in view.resultTable.selectedRows) {
            relations.add(results[row])
        }
        serviceManager.server.networkBuilder.addRelationsToNetwork(network, relations)
    }




    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT) {
                importSelected()
            }
        }
    }

}