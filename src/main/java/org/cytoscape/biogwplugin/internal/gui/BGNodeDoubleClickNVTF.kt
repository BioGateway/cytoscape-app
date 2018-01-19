package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.task.NodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator

class BGNodeDoubleClickNVTF(val serviceManager: BGServiceManager) : NodeViewTaskFactory {
    override fun createTaskIterator(nodeView: View<CyNode>, networkView: CyNetworkView): TaskIterator? {
        println("Double Click!")
        return null
    }

    override fun isReady(nodeView: View<CyNode>, networkView: CyNetworkView): Boolean {
        val network = networkView.model ?: return false
        val nodeUri = network.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")
        val node = serviceManager.server.searchForExistingNode(nodeUri) ?: return false
        return true
    }
}
