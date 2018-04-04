package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGRelation
import eu.biogateway.cytoscape.internal.model.BGRelationMetadata
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.CompletableFuture

@Deprecated("Use the generic relation metadata load functionailty instead.")
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
                relation.metadata[BGRelationMetadata.CONFIDENCE_VALUE] = BGRelationMetadata(BGRelationMetadata.DataType.NUMBER, it)
            }
        }
        completableFuture.complete(true)
        completion?.invoke()
    }
}