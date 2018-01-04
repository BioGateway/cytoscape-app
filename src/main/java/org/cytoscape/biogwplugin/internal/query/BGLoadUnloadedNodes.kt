package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor
import java.awt.EventQueue
import javax.swing.JOptionPane


class BGLoadUnloadedNodes(val serviceManager: BGServiceManager, val unloadedNodes: List<BGNode>, private val queryCompletion: (Int) -> Unit): AbstractTask(), Runnable {

    companion object {
        fun createAndRun(serviceManager: BGServiceManager, unloadedNodes: List<BGNode>?, completion: (Int) -> Unit) {
            if (unloadedNodes != null && unloadedNodes.isNotEmpty()) {
                EventQueue.invokeLater {
                    val query = BGLoadUnloadedNodes(serviceManager, unloadedNodes, completion)
                    if (unloadedNodes.size > Constants.BG_LOAD_NODE_WARNING_LIMIT) {
                        val message = unloadedNodes.size.toString() + " nodes needs to be loaded from the server. Do you want to proceed?"
                        val optionsText = arrayOf("Ok", "Show unloaded nodes", "Cancel")
                        val response = JOptionPane.showOptionDialog(null, message, "Load nodes from server?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, optionsText, null)
                        when (response) {
                            0 -> serviceManager.taskManager.execute(TaskIterator(query))
                            1 -> completion(0)
                        }
                    } else {
                        //query.run()
                        serviceManager.taskManager.execute(TaskIterator(query))
                    }
                }
            } else {
                completion(0)
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

