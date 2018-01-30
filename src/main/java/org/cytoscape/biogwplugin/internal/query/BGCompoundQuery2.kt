package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.CompletableFuture

class BGCompoundQuery2<T: BGReturnData>(val serviceManager: BGServiceManager, val queries: Collection<BGQuery>, val type: BGReturnType): AbstractTask(), Runnable {
    override fun run(taskMonitor: TaskMonitor?) {
        run()
    }

    val completionFuture = CompletableFuture<BGReturnCompoundData>()

    override fun run() {

        val futures = ArrayList<CompletableFuture<T>>()

        val returnData = BGReturnCompoundData(type, arrayOf(""))


        // Run the queries

        for (query in queries) {
            Thread(query).start()
            futures.add(query.futureReturnData as CompletableFuture<T>)
        }

        for (future in futures) {
            val data = future.get() as? T ?: return

            if (data is BGReturnRelationsData) {
                returnData.relationsData.addAll(data.relationsData)
                data.unloadedNodes?.let {
                    returnData.unloadedNodes.addAll(it)
                }
            } else if (data is BGReturnNodeData) {
                returnData.nodes.putAll(data.nodeData)
            } else if (data is BGReturnMetadata) {
                returnData.metadata.addAll(data.values)
            }
        }
        // The above loop will wait for all queries to finish.
        completionFuture.complete(returnData)
    }
}