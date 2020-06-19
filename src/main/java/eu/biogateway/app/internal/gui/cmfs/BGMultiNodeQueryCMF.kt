package eu.biogateway.app.internal.gui.cmfs

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.gui.BGQueryBuilderController
import eu.biogateway.app.internal.gui.BGRelationSearchResultsController
import eu.biogateway.app.internal.model.BGRelationType
import eu.biogateway.app.internal.query.*
import eu.biogateway.app.internal.util.Constants
import eu.biogateway.app.internal.util.Utility
import org.cytoscape.group.CyGroup
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator
import javax.swing.*

class BGMultiNodeQueryCMF(val gravity: Float): CyNetworkViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?): CyMenuItem {

        if (netView != null) {
            //val selectedNodes = ArrayList<String>()
            val network = netView.model ?: throw Exception("Network model not found!")
            val selectedNodes = CyTableUtil.getNodesInState(network, "selected", true)

            var selectedUris = ArrayList<String>()

            for (cyNode in selectedNodes) {
                val nodeUri = netView.model.defaultNodeTable.getRow(cyNode.suid).get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java)
                selectedUris.add(nodeUri)
            }

            val parentMenu = JMenu("BioGateway")

            parentMenu.add(createRelationSearchMenu("Fetch relations FROM selected", netView, selectedUris, BGRelationDirection.FROM, false))
            parentMenu.addSeparator()
            parentMenu.add(createRelationSearchMenu("Fetch relations TO selected", netView, selectedUris, BGRelationDirection.TO, false))
            parentMenu.addSeparator()
            parentMenu.add(createRelationSearchMenu("Find common relations FROM selected", netView, selectedUris, BGRelationDirection.FROM, true))
            parentMenu.addSeparator()
            parentMenu.add(createRelationSearchMenu("Find common relations TO selected", netView, selectedUris, BGRelationDirection.TO, true))

            parentMenu.addSeparator()
            parentMenu.add(createOpenQueryBuilderWithSelectedURIsMenu(netView, selectedUris))

            return CyMenuItem(parentMenu, gravity)
        }
        return CyMenuItem(null, gravity)
    }


    private fun createOpenQueryBuilderWithSelectedURIsMenu(netView: CyNetworkView, nodeUris: Collection<String>): JMenuItem {
        val item = JMenuItem("Use selected URIs in query builder")
        item.addActionListener {
            val queryBuilder = BGQueryBuilderController()
            queryBuilder.addMultiQueryLinesForURIs(nodeUris)

        }
        return item
    }

    private fun createRelationSearchQuery(relationType: BGRelationType, netView: CyNetworkView, nodeUris: Collection<String>, direction: BGRelationDirection, onlyCommonRelations: Boolean, group: CyGroup? = null): BGMultiNodeRelationQuery? {
        // TODO: Refactor to Kotlin Flow
        val query = BGMultiNodeRelationQuery(nodeUris, relationType, direction)
        query.addCompletion {
            val returnData = it as? BGReturnRelationsData
            if (returnData != null) {
                val network = netView.model
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                BGLoadNodeDataFromBiogwDict.createAndRun(returnData.unloadedNodes, 300) {
                    println("Loaded "+it.toString()+ " nodes.")
                    BGRelationSearchResultsController(returnData, returnData.columnNames, network)
                }
            }
        }

        if (group != null) {
            val groupNodeURIs = Utility.getNodeURIsForGroup(group)
            query.returnDataFilter = { relation ->
                (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
            }
        }

        if (onlyCommonRelations) {

            val optionPanePanel = JPanel()
            optionPanePanel.add(JLabel("What is the minimum number\nof common relations?"))
            val inputTextField = JTextField(5)
            optionPanePanel.add(inputTextField)

            val options = arrayOf("Ok", "Cancel", "Most in common")

            val result = JOptionPane.showOptionDialog(null, optionPanePanel, "Minimum number of common relations?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null)

            if (result == JOptionPane.OK_OPTION) {
                val minCommonRelations = inputTextField.text
                if (minCommonRelations.matches(Regex("^\\d+$"))) {
                    query.minCommonRelations = minCommonRelations.toInt()
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid integer.")
                    return null
                }
            }
            if (result == 2) {
                query.minCommonRelations = -1
            }

        } else {
            query.minCommonRelations = 0
        }
        return query
    }

    private fun createRelationSearchMenu(description: String, netView: CyNetworkView, nodeUris: Collection<String>, direction: BGRelationDirection, onlyCommonRelations: Boolean, lookForGroups: Boolean = false): JMenuItem {

        val parentMenu = JMenu(description)

        for (relationType in BGServiceManager.config.activeRelationTypes.sortedBy { it.number }) {
            val item = JMenuItem(relationType.description)

            item.addActionListener {
                var group: CyGroup? = null
                if (lookForGroups) {
                    group = Utility.selectGroupPopup(netView.model)
                    if (group == null) return@addActionListener
                }
                val query = createRelationSearchQuery(relationType, netView, nodeUris, direction, onlyCommonRelations, group)
                BGServiceManager.taskManager?.execute(TaskIterator(query))
            }
            parentMenu.add(item)
        }
        return parentMenu
    }
}