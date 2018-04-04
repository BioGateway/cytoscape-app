package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGPrimitiveRelation
import eu.biogateway.cytoscape.internal.model.BGRelation
import eu.biogateway.cytoscape.internal.model.BGRelationMetadata
import eu.biogateway.cytoscape.internal.model.BGRelationMetadataType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.TimeUnit

/// Loads all specified metadata types for the specified relations, where the relations' type is supported by the metadata type.
class BGLoadRelationMetadataQuery(val serviceManager: BGServiceManager, val relations: Collection<BGPrimitiveRelation>, val activeMetadataTypes: Collection<BGRelationMetadataType>, val completion: () -> Unit): AbstractTask(), Runnable {

    var taskMonitor: TaskMonitor? = null

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    override fun run() {
        taskMonitor?.setTitle("Loading metadata...")
        val metadataRelations = HashMap<BGRelationMetadataType, HashSet<BGPrimitiveRelation>>()
        // Create a map between metadata types and the relations they support.

        for (metadataType in activeMetadataTypes) {
            if (!metadataRelations.containsKey(metadataType)) metadataRelations[metadataType] = HashSet()
            for (relation in relations) {
                if (metadataType.supportedRelations.contains(relation.relationType)) {
                    metadataRelations[metadataType]!!.add(relation)
                }
            }
        }

        val relationCount = metadataRelations.values.fold(0) { acc, hashSet -> acc + hashSet.size }
        var counter = 1

        // For each metadata type, query for each of their relations.

        for ((metadataType, toFetchRelations) in metadataRelations.iterator()) {
            for (relation in toFetchRelations) {

                taskMonitor?.setProgress(counter.toDouble()/relationCount.toDouble())
                counter++

                val query = BGFetchMetadataQuery(serviceManager,
                        relation.fromNodeUri,
                        relation.relationType.uri,
                        relation.toNodeUri,
                        relation.sourceGraph,
                        metadataType.relationUri)

                query.run()
                val result =  query.returnFuture.get(10, TimeUnit.SECONDS) as BGReturnMetadata
                // In the completion, add the metadata to the relations' metadata map.

                val dataType = metadataType.dataType

                val value = result.values.firstOrNull()

                val metaData = when (dataType) {
                    BGRelationMetadata.DataType.STRING -> {
                        // TODO: Handle arrays / multiple data points.
                        if (value != null) BGRelationMetadata(metadataType, value) else null
                    }
                    BGRelationMetadata.DataType.NUMBER -> {
                        if (value != null) BGRelationMetadata(metadataType, value.toDouble()) else null

                    }}
                // Add this metadata entry into the relation's metadata map.
                metaData?.let {
                    relation.metadata[metadataType] = it
                }
            }
        }
        completion()
    }
}