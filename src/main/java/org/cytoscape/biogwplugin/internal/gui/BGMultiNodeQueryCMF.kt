package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator
import java.awt.SystemColor.menu
import java.awt.event.ActionListener
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
            parentMenu.addSeparator()
            parentMenu.add(createOpenQueryBuilderWithSelectedURIsMenu(netView, selectedUris))


            return CyMenuItem(parentMenu, gravity)
        }
        return CyMenuItem(null, gravity)
    }

    private fun createPPISearchMenu(description: String, network: CyNetwork, nodeUris: Collection<String>, onlyCommonRelations: Boolean): JMenuItem? {
        var foundProteins = false
        for (uri in nodeUris) {
            if (uri.contains("uniprot")) foundProteins = true
        }
        if (foundProteins) {
            val ppiItem = JMenuItem(description)
            ppiItem.addActionListener {
                val query = BGFindBinaryPPIInteractionsForMultipleNodesQuery(serviceManager, nodeUris)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                        BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                            println("Loaded "+it.toString()+ " nodes.")
                            BGRelationSearchResultsController(serviceManager, returnData, returnData.columnNames, network)
                        } }}
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
                            return@addActionListener
                        }
                    }
                    if (result == 2) {
                        query.minCommonRelations = -1
                    }

                } else {
                    query.minCommonRelations = 0
                }
                serviceManager.taskManager.execute(TaskIterator(query))
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

    private fun createRelationSearchMenu(description: String, netView: CyNetworkView, nodeUris: Collection<String>, direction: BGRelationDirection, onlyCommonRelations: Boolean): JMenuItem {

        val parentMenu = JMenu(description)

        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypeMap.values.sortedBy { it.number }) {
            val item = JMenuItem(relationType.name)

            item.addActionListener(ActionListener {
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
                            return@ActionListener
                        }
                    }
                    if (result == 2) {
                        query.minCommonRelations = -1
                    }

                } else {
                    query.minCommonRelations = 0
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            })
            parentMenu.add(item)
        }
        return parentMenu
    }
}