package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.query.BGRelationsQuery
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * Created by sholmas on 06/07/2017.
 */
class BGRelationSearchCMF(val descripton: String, val serviceManager: BGServiceManager): CyNodeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val nodeUri = netView?.model?.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get("identifier uri", String::class.java)
        var parentMenu = JMenu(descripton)


        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypes.values) {
            val item = JMenuItem(relationType.description)

            item.addActionListener(ActionListener {
                println("TODO: Should search for relations of type \""+relationType.description+"\" from node "+nodeUri)
            })

            parentMenu.add(item)
        }
        return CyMenuItem(parentMenu, 0F)
    }
}