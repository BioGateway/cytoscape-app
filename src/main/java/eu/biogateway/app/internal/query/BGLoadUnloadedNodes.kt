package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.util.Constants
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor
import java.awt.EventQueue
import javax.swing.JOptionPane



class BGLoadNodeDataFromBiogwDict(val unloadedNodes: List<BGNode>, val bulkSize: Int, private val queryCompletion: (Int) -> Unit): AbstractTask(), Runnable {

    companion object {
        fun createAndRun(unloadedNodes: List<BGNode>?, bulkSize: Int, completion: (Int) -> Unit) {
            if (unloadedNodes != null && unloadedNodes.isNotEmpty()) {
                val query = BGLoadNodeDataFromBiogwDict(unloadedNodes, bulkSize, completion)
                BGServiceManager.taskManager?.execute(TaskIterator(query))
            } else {
                completion(0)
            }
        }
    }

    var taskMonitor: TaskMonitor? = null
    var isCancelled = false

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    override fun run() {
        if (bulkSize < 1) {
            loadAllNodesIn(unloadedNodes)
        }
        else if (bulkSize == 1){
            loadInSeriesDictAndSPARQL()
        } else {
            loadInBulk()
        }
        queryCompletion(unloadedNodes.size)
    }


    private fun loadInBulk() {
        // Chop the array of URIs into an array of arrays, with bulkSize URIs in each.
        val chunks = unloadedNodes.chunked(bulkSize)
        for ((index, chunk) in chunks.withIndex()) {
            setProgress((index + 1)*bulkSize)
            loadAllNodesIn(chunk)
        }
    }

    private fun loadAllNodesIn(nodes: Collection<BGNode>) {
        val loadedNodes = BGServiceManager.dataModelController.loadNodesFromServerSynchronously(nodes.filter { !it.isLoaded })
    }

    private fun setProgress(progress: Int) {
        taskMonitor?.setTitle("Loading node " + progress+ " of " + unloadedNodes.size + "...")
        taskMonitor?.setProgress(progress.toDouble() / unloadedNodes.size.toDouble())
    }

    private fun loadInSeriesDictAndSPARQL() {
        var index = 0
        taskMonitor?.setTitle("Loading nodes")


        synchronized(this.isCancelled) {
            while ((!this.isCancelled) && (index < unloadedNodes.size)) {
                val node = unloadedNodes[index]
                setProgress(index + 1)
                if (!node.isLoaded) BGServiceManager.dataModelController.loadDataForNode(node)
                index++
            }
        }
        queryCompletion(unloadedNodes.size)
    }
}

@Deprecated("Must be rewritten to handle the BiogwDict")
class BGLoadUnloadedNodes(val unloadedNodes: List<BGNode>, private val queryCompletion: (Int) -> Unit): AbstractTask(), Runnable {

    companion object {
        fun createAndRun(unloadedNodes: List<BGNode>?, completion: (Int) -> Unit) {
            if (unloadedNodes != null && unloadedNodes.isNotEmpty()) {
                EventQueue.invokeLater {
                    val query = BGLoadUnloadedNodes(unloadedNodes, completion)
                    if (unloadedNodes.size > Constants.BG_LOAD_NODE_WARNING_LIMIT) {
                        val message = unloadedNodes.size.toString() + " nodes needs to be loaded from the BioGateway Server. Do you want to proceed?"
                        val optionsText = arrayOf("Ok", "Show unloaded nodes", "Cancel")
                        val response = JOptionPane.showOptionDialog(null, message, "Load nodes from BioGateway?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, optionsText, null)
                        when (response) {
                            0 -> BGServiceManager.taskManager?.execute(TaskIterator(query))
                            1 -> completion(0)
                        }
                    } else {
                        //query.run()
                        BGServiceManager.taskManager?.execute(TaskIterator(query))
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
                if (!node.isLoaded) BGServiceManager.dataModelController.loadDataForNode(node)
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

