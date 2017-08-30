package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import javax.swing.JMenu
import javax.swing.JMenuItem

class BGChangeEdgeTypeCMF(val gravity: Float, val serviceManager: BGServiceManager): CyEdgeViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {
        val parentMenu = JMenu("Change to...")
        val edgeSuid = edgeView?.model?.suid
        val edgeTable = netView?.model?.defaultEdgeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get("identifier uri", String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")

        for (key in serviceManager.cache.relationTypes.keys) {
            val relationType = serviceManager.cache.relationTypes.get(key)
            if (relationType != null) {
                val item = JMenuItem(relationType.description)
                item.addActionListener {
                    if (key == edgeUri) {
                        return@addActionListener
                    }
                    // Change the relation in the edge table:
                    val edgeId = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_EDGE_ID, String::class.java)
                    if (edgeId != null) {
                        edgeTable?.getRow(edgeSuid)?.set(Constants.BG_FIELD_EDGE_ID,  edgeId.replace(edgeUri, relationType.uri))
                    }

                    edgeTable?.getRow(edgeSuid)?.set(Constants.BG_FIELD_NAME, relationType.description)
                    edgeTable?.getRow(edgeSuid)?.set("shared name", relationType.description)
                    edgeTable?.getRow(edgeSuid)?.set(Constants.BG_FIELD_IDENTIFIER_URI, relationType.uri)



                }
                parentMenu.add(item)
            }
        }

        return CyMenuItem(parentMenu, gravity)
    }
}