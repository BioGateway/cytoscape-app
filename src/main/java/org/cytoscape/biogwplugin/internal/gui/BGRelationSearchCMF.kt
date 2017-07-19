package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.query.BGFindRelationForNodeQuery
import org.cytoscape.biogwplugin.internal.query.BGRelationDirection
import org.cytoscape.biogwplugin.internal.query.BGReturnRelationsData
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Created by sholmas on 06/07/2017.
 */
class BGRelationSearchCMF(val descripton: String, val direction: BGRelationDirection, val serviceManager: BGServiceManager): CyNodeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val nodeUri = netView?.model?.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get("identifier uri", String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")
        var parentMenu = JMenu(descripton)


        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypes.values) {
            val item = JMenuItem(relationType.description)

            item.addActionListener(ActionListener {
                println("TODO: Should search for relations of type \""+relationType.description+"\" from node "+nodeUri)
                val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        val network = netView.model
                            serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
                        }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            })

            parentMenu.add(item)
        }
        return CyMenuItem(parentMenu, 0F)
    }
}