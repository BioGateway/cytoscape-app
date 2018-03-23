package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadata
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.CompletableFuture

class BGFetchConfidenceValues(val serviceManager: BGServiceManager, val title: String, val relations: Collection<BGRelation>): AbstractTask() {

    var completableFuture = CompletableFuture<Boolean>()
    var completion: (() -> Unit)? = null

    override fun run(taskMonitor: TaskMonitor?) {
        taskMonitor?.setTitle(title)
        for ((i, relation) in relations.withIndex()) {
            val progress = i.toDouble() / relations.size.toDouble()
            taskMonitor?.setProgress(progress)
            taskMonitor?.setStatusMessage(i.toString() + " of "+ relations.size.toString())
            serviceManager.dataModelController.getConfidenceScoreForRelation(relation)?.let {
                relation.metadata["confidence"] = BGRelationMetadata(BGRelationMetadata.DataType.NUMBER, it)
            }
        }
        completableFuture.complete(true)
        completion?.invoke()
    }
}