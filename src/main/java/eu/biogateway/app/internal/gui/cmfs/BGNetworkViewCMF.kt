package eu.biogateway.app.internal.gui.cmfs

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.gui.BGNodeLookupController
import eu.biogateway.app.internal.util.Utility
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import javax.swing.JMenuItem

class BGNetworkViewCMF(val gravity: Float): CyNetworkViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?): CyMenuItem {

        if (netView != null) {
            val network = netView.model ?: throw Exception("Network model not found!")
            val selectedNodes = CyTableUtil.getNodesInState(network, "selected", true)

            if (selectedNodes.size < 1) {
                return CyMenuItem(createLookupNodeMenu(network, netView), gravity)
            }
            if (selectedNodes.size == 1) {
                val node = selectedNodes[0]
                val view = BGServiceManager.applicationManager?.currentNetworkView?.getNodeView(node)
                return BGNodeMenuActionsCMF(gravity).createMenuItem(netView, view)
            }
            return BGMultiNodeQueryCMF(gravity).createMenuItem(netView)
        }
        return CyMenuItem(null, gravity)
    }

    private fun createLookupNodeMenu(network: CyNetwork, netView: CyNetworkView): JMenuItem {
        val item = JMenuItem("Add BioGateway node")
        item.addActionListener {
            BGNodeLookupController(null) { node ->
                if (node != null) {
                    BGServiceManager.dataModelController.networkBuilder.addBGNodesToNetwork(arrayListOf(node), network)
                    Utility.reloadCurrentVisualStyleCurrentNetworkView()
                }
            }
        }
        return item
    }

}