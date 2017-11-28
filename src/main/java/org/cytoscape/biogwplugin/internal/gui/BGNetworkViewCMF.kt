package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import javax.swing.JMenuItem

class BGNetworkViewCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNetworkViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?): CyMenuItem {

        if (netView != null) {


            //val selectedNodes = ArrayList<String>()
            val network = netView.model ?: throw Exception("Network model not found!")
            val selectedNodes = CyTableUtil.getNodesInState(network, "selected", true)

            if (selectedNodes.size < 1) {
                return CyMenuItem(createLookupNodeMenu(network, netView), gravity)
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

    private fun createLookupNodeMenu(network: CyNetwork, netView: CyNetworkView): JMenuItem {
        val item = JMenuItem("Add BioGateway node")
        item.addActionListener {
            BGNodeLookupController(serviceManager, null) { node ->
                if (node != null) {
                    serviceManager.server.networkBuilder.addBGNodesToNetwork(arrayListOf(node), network)
                    Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)
                }
            }
        }
        return item
    }

}