package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.*
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.TimeUnit

class BGLoadNodeMetadataQuery(val uris: Collection<String>, val activeMetadataTypes: Collection<BGNodeMetadataType>, val completion: (Map<String, Set<BGNodeMetadata>>) -> Unit): AbstractTask(), Runnable {

    var taskMonitor: TaskMonitor? = null

    var results = HashMap<String, HashSet<BGNodeMetadata>>()

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    override fun run() {
        taskMonitor?.setTitle("Loading node metadata...")

        val metadataNodes = HashMap<BGNodeMetadataType, HashSet<String>>()

        for (metadataType in activeMetadataTypes) {
            if (!metadataNodes.containsKey(metadataType)) metadataNodes[metadataType] = HashSet()
            for (uri in uris) {
                val nodeType = BGNodeTypeNew.getNodeTypeForUri(uri) ?: continue
                if (nodeType == metadataType.nodeType) {
                    metadataNodes[metadataType]!!.add(uri)
                }
            }
        }

        val nodeCount = metadataNodes.values.fold(0) { acc, set -> acc + set.size }
        var counter = 1


        for ((metadataType, uris) in metadataNodes.iterator()) {
            for (uri in uris) {
                if (cancelled) {
                    return
                }

                taskMonitor?.setProgress(counter.toDouble()/nodeCount.toDouble())
                taskMonitor?.setStatusMessage("Loading "+counter+" of "+nodeCount+" ...")
                counter++

                val result = metadataType.runQueries(uri) ?: continue

                // TODO: Use the result
                if (!results.containsKey(uri)) results[uri] = HashSet()

                results[uri]!!.add(result)
            }
        }
        completion(results)
    }
}