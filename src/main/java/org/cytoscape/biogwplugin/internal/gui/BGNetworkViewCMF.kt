package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView

class BGNetworkViewCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNetworkViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?): CyMenuItem {

        if (netView != null) {
            //val selectedNodes = ArrayList<String>()
            val network = netView.model ?: throw Exception("Network model not found!")
            val selectedNodes = CyTableUtil.getNodesInState(network, "selected", true)

            if (selectedNodes.size < 1) {
                return CyMenuItem(null, gravity)
            }

            if (selectedNodes.size == 1) {
                val node = selectedNodes[0]
                val view = serviceManager.applicationManager.currentNetworkView.getNodeView(node)
                return BGNodeMenuActionsCMF(gravity, serviceManager).createMenuItem(netView, view)
            }

            return BGMultiNodeQueryCMF(gravity, serviceManager).createMenuItem(netView)
        }
        return CyMenuItem(null, gravity)
    }
}