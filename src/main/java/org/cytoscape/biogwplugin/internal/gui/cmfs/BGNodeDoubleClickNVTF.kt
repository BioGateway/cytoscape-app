package org.cytoscape.biogwplugin.internal.gui.cmfs

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.getUri
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyNode
import org.cytoscape.task.NodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor

class BGTask(val task: (() -> Unit)): AbstractTask() {
    var statusText: String? = null
    override fun run(taskMonitor: TaskMonitor?) {
        if (statusText != null) taskMonitor?.setTitle(statusText)
        task()
    }
}

class BGNodeDoubleClickNVTF(val serviceManager: BGServiceManager) : NodeViewTaskFactory {

    // TODO: This should be more dynamic.
    val HAS_AGENT_URI = "http://semanticscience.org/resource/SIO_000139"

    override fun createTaskIterator(nodeView: View<CyNode>, networkView: CyNetworkView): TaskIterator? {
        val task = BGTask() {
            serviceManager.server.networkBuilder.collapseEdgeWithNodes(networkView, nodeView, HAS_AGENT_URI)
        }
        return TaskIterator(task)
    }

    override fun isReady(nodeView: View<CyNode>, networkView: CyNetworkView): Boolean {
        val network = networkView.model ?: return false
        val nodeUri = network.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")
        val node = serviceManager.server.searchForExistingNode(nodeUri) ?: return false
        return true
    }
}
