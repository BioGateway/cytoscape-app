package eu.biogateway.cytoscape.internal.gui.cmfs

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import eu.biogateway.cytoscape.internal.BGServiceManager
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View

class BGNodeViewCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNodeViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        if (netView != null) {
            //val selectedNodes = ArrayList<String>()
            val network = netView.model ?: throw Exception("Network model not found!")
            val selectedNodes = CyTableUtil.getNodesInState(network, "selected", true)

            if (selectedNodes.size < 2) {
                return BGNodeMenuActionsCMF(gravity, serviceManager).createMenuItem(netView, nodeView)
            }

            return BGMultiNodeQueryCMF(gravity, serviceManager).createMenuItem(netView)
        }
        return CyMenuItem(null, gravity)
    }
}