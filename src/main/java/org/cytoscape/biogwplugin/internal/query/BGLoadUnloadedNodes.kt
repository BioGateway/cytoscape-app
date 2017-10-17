package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor
import java.awt.EventQueue
import javax.swing.JOptionPane


class BGLoadUnloadedNodes(val serviceManager: BGServiceManager, val unloadedNodes: ArrayList<BGNode>, private val queryCompletion: (Int) -> Unit): AbstractTask(), Runnable {


    companion object {
        fun createAndRun(serviceManager: BGServiceManager, unloadedNodes: ArrayList<BGNode>?, completion: (Int) -> Unit) {
            if (unloadedNodes != null) {
                EventQueue.invokeLater {
                    val query = BGLoadUnloadedNodes(serviceManager, unloadedNodes, completion)
                    if (unloadedNodes.size > Constants.BG_LOAD_NODE_WARNING_LIMIT) {
                        val message = unloadedNodes.size.toString() + " nodes needs to be loaded from the server. Do you want to proceed?"
                        val response = JOptionPane.showOptionDialog(null, message, "Load nodes from server?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null)
                        if (response == JOptionPane.OK_OPTION) {
                            serviceManager.taskManager.execute(TaskIterator(query))
                        }
                    } else {
                        query.run()
                    }
                }
            }
        }
    }

    var taskMonitor: TaskMonitor? = null
    var isCancelled = false


    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor =taskMonitor
        this.run()
    }

    override fun run() {
        var index = 0
        taskMonitor?.setTitle("Loading nodes")

        synchronized(this.isCancelled) {
            while ((!this.isCancelled) && (index < unloadedNodes.size)) {
                val node = unloadedNodes[index]
                taskMonitor?.setTitle("Loading node " + (index + 1) + " of " + unloadedNodes.size + "...")
                taskMonitor?.setProgress(index.toDouble() / unloadedNodes.size.toDouble())
                if (!node.isLoaded) serviceManager.server.loadDataForNode(node)
                index++
            }
        }
        queryCompletion(unloadedNodes.size)
    }

    override fun cancel() {
        super.cancel()
        this.isCancelled = true
    }
}

