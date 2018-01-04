package org.cytoscape.biogwplugin.internal.gui.cmfs

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.BGQueryBuilderController
import org.cytoscape.biogwplugin.internal.gui.BGRelationSearchResultsController
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.group.CyGroup
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator
import javax.swing.*

class BGMultiNodeQueryCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNetworkViewContextMenuFactory {
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

            createPPISearchMenu("Look for binary PPIs", network, selectedUris, false)?.let {
                parentMenu.addSeparator()
                parentMenu.add(it)
            }
            createPPISearchMenu("Look for common binary PPIs", network, selectedUris, true)?.let {
                parentMenu.addSeparator()
                parentMenu.add(it)
            }

            // Exmerimental functionality allowing users to search for relations to or from a CyGroup:
//            createSearchToGroupMenu("Search to group", netView, network, selectedUris)?.let {
//                parentMenu.addSeparator()
//                parentMenu.add(it)
//            }
            parentMenu.addSeparator()
            parentMenu.add(createOpenQueryBuilderWithSelectedURIsMenu(netView, selectedUris))

            return CyMenuItem(parentMenu, gravity)
        }
        return CyMenuItem(null, gravity)
    }

    /// Creates menu items allowing users to search for relations to or from nodes in a CyGroup.
    private fun createSearchToGroupMenu(description: String, netView: CyNetworkView, network: CyNetwork, selectedUris: Collection<String>): JMenu? {

        val groupSearchMenu = JMenu(description)

        groupSearchMenu.add(createRelationSearchMenu("Fetch relations FROM selected", netView, selectedUris, BGRelationDirection.FROM, false, true))
        groupSearchMenu.add(createRelationSearchMenu("Fetch relations TO selected", netView, selectedUris, BGRelationDirection.TO, false, true))
        groupSearchMenu.add(createRelationSearchMenu("Find common relations FROM selected", netView, selectedUris, BGRelationDirection.FROM, true, true))
        groupSearchMenu.add(createRelationSearchMenu("Find common relations TO selected", netView, selectedUris, BGRelationDirection.TO, true, true))

        createPPISearchMenu("Look for binary PPIs", network, selectedUris, false, true)?.let {
            groupSearchMenu.add(it)
        }
        createPPISearchMenu("Look for common binary PPIs", network, selectedUris, true, true)?.let {
            groupSearchMenu.add(it)
        }
        return groupSearchMenu
    }

    private fun createPPISearchQuery(nodeUris: Collection<String>, network: CyNetwork, onlyCommonRelations: Boolean = false, group: CyGroup? = null): BGFindBinaryPPIInteractionsForMultipleNodesQuery? {
        val query = BGFindBinaryPPIInteractionsForMultipleNodesQuery(serviceManager, nodeUris)
        query.addCompletion {
            val returnData = it as? BGReturnRelationsData
            if (returnData != null) {
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                    println("Loaded "+it.toString()+ " nodes.")
                    BGRelationSearchResultsController(serviceManager, returnData, returnData.columnNames, network)
                } }}
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


    private fun createPPISearchMenu(description: String, network: CyNetwork, nodeUris: Collection<String>, onlyCommonRelations: Boolean, lookForGroups: Boolean = false): JMenuItem? {
        var foundProteins = false
        for (uri in nodeUris) {
            if (uri.contains("uniprot")) foundProteins = true
        }
        if (foundProteins) {
            val ppiItem = JMenuItem(description)
            ppiItem.addActionListener {
                var group: CyGroup? = null
                if (lookForGroups) {
                    group = Utility.selectGroupPopup(serviceManager, network)
                    if (group == null) return@addActionListener
                }
                val query = createPPISearchQuery(nodeUris, network, onlyCommonRelations, group)
                if (query != null) {
                    serviceManager.taskManager.execute(TaskIterator(query))
                }
            }
            return ppiItem
        }
        return null
    }

    private fun createOpenQueryBuilderWithSelectedURIsMenu(netView: CyNetworkView, nodeUris: Collection<String>): JMenuItem {
        val item = JMenuItem("Use selected URIs in query builder")
        item.addActionListener {
            val queryBuilder = BGQueryBuilderController(serviceManager)
            queryBuilder.addMultiQueryLinesForURIs(nodeUris)

        }
        return item
    }

    private fun createRelationSearchQuery(relationType: BGRelationType, netView: CyNetworkView, nodeUris: Collection<String>, direction: BGRelationDirection, onlyCommonRelations: Boolean, group: CyGroup? = null): BGMultiNodeRelationQuery? {
        val query = BGMultiNodeRelationQuery(serviceManager, nodeUris, relationType, direction)
        query.addCompletion {
            val returnData = it as? BGReturnRelationsData
            if (returnData != null) {
                val network = netView.model
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                    println("Loaded "+it.toString()+ " nodes.")
                    BGRelationSearchResultsController(serviceManager, returnData, returnData.columnNames, network)
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

        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypeMap.values.sortedBy { it.number }) {
            val item = JMenuItem(relationType.description)

            item.addActionListener {
                var group: CyGroup? = null
                if (lookForGroups) {
                    group = Utility.selectGroupPopup(serviceManager, netView.model)
                    if (group == null) return@addActionListener
                }
                val query = createRelationSearchQuery(relationType, netView, nodeUris, direction, onlyCommonRelations, group)
                serviceManager.taskManager.execute(TaskIterator(query))
            }
            parentMenu.add(item)
        }
        return parentMenu
    }
}