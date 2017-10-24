package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem

class BGMultiNodeQueryCMF(val gravity: Float, val description: String, val direction: BGRelationDirection, val serviceManager: BGServiceManager): CyNetworkViewContextMenuFactory {
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

            val menu = createRelationSearchMenu(description, netView, selectedUris, direction)
            return CyMenuItem(menu, gravity)
        }
        return CyMenuItem(null, gravity)
    }


    private fun createRelationSearchMenu(description: String, netView: CyNetworkView, nodeUris: Collection<String>, direction: BGRelationDirection): JMenu {

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
                        if (returnData.relationsData.size == 0) throw Exception("No relationsFound found.")
                        val columnNames = arrayOf("from node","relation type", "to node")
                        BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                            println("Loaded "+it.toString()+ " nodes.")
                            BGRelationSearchResultsController(serviceManager, returnData.relationsData, columnNames, network)
                        }
                    }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            })
            parentMenu.add(item)
        }
        return parentMenu
    }
}