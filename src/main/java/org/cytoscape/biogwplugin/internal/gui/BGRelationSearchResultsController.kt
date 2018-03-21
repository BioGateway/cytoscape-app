package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.query.BGFetchConfidenceValues
import org.cytoscape.biogwplugin.internal.query.BGReturnRelationsData
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyNetwork
import org.cytoscape.work.TaskIterator
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import javax.swing.table.DefaultTableModel

class BGRelationSearchResultsController(val serviceManager: BGServiceManager, private val returnData: BGReturnRelationsData, val columnNames: Array<String>, val network: CyNetwork) : ActionListener, BGRelationResultViewTooltipDataSource {

    private val relationsFound = returnData.relationsData
    val importConfidenceValues = true

    override fun getTooltipForResultRowAndColumn(row: Int, column: Int): String? {
        val modelRow = view.resultTable.convertRowIndexToModel(row)
        val relation = relationsFound[modelRow]
        if (column == 0) return relation.fromNode.description
        if (column == 1) return relation.relationType.description
        if (column == 2) return relation.toNode.description
        return null
    }



    private val view = BGRelationSearchResultsView(this, this)

    init {

        returnData.resultTitle?.let {
            view.mainFrame.title = it
        }

        val model = view.resultTable.model as DefaultTableModel
        model.setColumnIdentifiers(columnNames)

        showAllResults()
        Utility.fightForFocus(view.mainFrame)
     }

    private fun showAllResults() {
        val model = view.resultTable.model as DefaultTableModel
        for (result in relationsFound) {
            model.addRow(result.asArray())
        }
    }


    private fun importSelected() {
        val relations = ArrayList<BGRelation>()
        for (row in view.resultTable.selectedRows) {
            relations.add(relationsFound[view.resultTable.convertRowIndexToModel(row)])
        }


        if (importConfidenceValues) {
            val searchRelations = relations.filter { it.relationType.identifier.equals("intact:http://purl.obolibrary.org/obo/RO_0002436") }
            val query = BGFetchConfidenceValues(serviceManager, "Loading confidence values...", searchRelations)
            query.completion = {
                this.serviceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, relations)
                Utility.reloadCurrentVisualStyleCurrentNetworkView(this.serviceManager)
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
        } else {
            serviceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, relations)
            Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)
        }
    }

    private fun importBetweenExistingNodes() {

        val allNodeUris = network.defaultNodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI).getValues(String::class.java)
        var relations = ArrayList<BGRelation>()

        for (result in relationsFound) {
            if (allNodeUris.contains(result.toNode.uri) && allNodeUris.contains(result.fromNode.uri)) {
                relations.add(result)
            }
        }

        if (importConfidenceValues) {
            val searchRelations = relations.filter { it.relationType.identifier.equals("intact:http://purl.obolibrary.org/obo/RO_0002436") }
            val query = BGFetchConfidenceValues(serviceManager, "Loading confidence values...", searchRelations)
            query.completion = {
                this.serviceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, relations)
                Utility.reloadCurrentVisualStyleCurrentNetworkView(this.serviceManager)
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
        } else {

            serviceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, relations)
            Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)
        }
    }

    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT) {
                importSelected()
            }
            if (e.actionCommand == BGRelationSearchResultsView.ACTION_IMPORT_BETWEEN_EXISTING) {
                importBetweenExistingNodes()
            }
        }
    }




}