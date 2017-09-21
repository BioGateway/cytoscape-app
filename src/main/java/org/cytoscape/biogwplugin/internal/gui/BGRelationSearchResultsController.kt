package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyNetwork
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.table.DefaultTableModel

class BGRelationSearchResultsController(val serviceManager: BGServiceManager, private val relationsFound: ArrayList<BGRelation>, val columnNames: Array<String>, val network: CyNetwork) : ActionListener {

    private val view: BGRelationSearchResultsView = BGRelationSearchResultsView(this)

    init {

        val table = view.resultTable.model as DefaultTableModel
        table.setColumnIdentifiers(columnNames)

        for (result in relationsFound) {
            table.addRow(result.nameStringArray())
        }
        view.mainFrame.toFront()
    }


    private fun importSelected() {
        val relations = ArrayList<BGRelation>()
        for (row in view.resultTable.selectedRows) {
            relations.add(relationsFound[view.resultTable.convertRowIndexToModel(row)])
        }
        serviceManager.server.networkBuilder.addRelationsToNetwork(network, relations)
    }

    private fun importToExisting() {

        val allNodeUris = network.defaultNodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI).getValues(String::class.java)
        var relations = ArrayList<BGRelation>()

        for (result in relationsFound) {
            if (allNodeUris.contains(result.toNode.uri) && allNodeUris.contains(result.fromNode.uri)) {
                relations.add(result)
            }
        }
        serviceManager.server.networkBuilder.addRelationsToNetwork(network, relations)
    }



    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT) {
                importSelected()
            }
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT_TO_EXISTING) {
                importToExisting()
            }
        }
    }



}