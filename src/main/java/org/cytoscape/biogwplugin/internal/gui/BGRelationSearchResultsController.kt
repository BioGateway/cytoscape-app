package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyNetwork
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import javax.swing.RowFilter
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class BGRelationSearchResultsController(val serviceManager: BGServiceManager, private val relationsFound: ArrayList<BGRelation>, val columnNames: Array<String>, val network: CyNetwork) : ActionListener, BGRelationResultViewTooltipDataSource {

    override fun getTooltipForResultRowAndColumn(row: Int, column: Int): String? {
        val modelRow = view.resultTable.convertRowIndexToModel(row)
        val relation = relationsFound[modelRow]
        if (column == 0) return relation.fromNode.description
        if (column == 1) return relation.relationType.description
        if (column == 2) return relation.toNode.description
        return null
    }



    private val view: BGRelationSearchResultsView = BGRelationSearchResultsView(this, this)

    init {
        val model = view.resultTable.model as DefaultTableModel
        model.setColumnIdentifiers(columnNames)

        showAllResults()
        Utility.fightForFocus(view.mainFrame)
     }

    fun showAllResults() {
        val model = view.resultTable.model as DefaultTableModel
        for (result in relationsFound) {
            model.addRow(result.nameStringArray())
        }
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

    private fun filterResults() {
        val filterText = view.filterTextField.text
        val model = view.resultTable.model as DefaultTableModel
    }


    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT) {
                importSelected()
            }
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT_TO_EXISTING) {
                importToExisting()
            }
            if (e.source == view.filterTextField) {
                filterResults()
            }
        }
    }




}