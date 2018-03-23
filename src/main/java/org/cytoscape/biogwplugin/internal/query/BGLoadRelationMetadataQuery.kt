package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadata
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadataType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.TimeUnit

/// Loads all specified metadata types for the specified relations, where the relations' type is supported by the metadata type.
class BGLoadRelationMetadataQuery(val serviceManager: BGServiceManager, val relations: Collection<BGRelation>, val activeMetadataTypes: Collection<BGRelationMetadataType>, val completion: () -> Unit): AbstractTask(), Runnable {

    var taskMonitor: TaskMonitor? = null

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    override fun run() {
        taskMonitor?.setTitle("Loading metadata...")
        val metadataRelations = HashMap<BGRelationMetadataType, HashSet<BGRelation>>()
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
                        relation.fromNode.uri,
                        relation.relationType.uri,
                        relation.toNode.uri,
                        relation.sourceGraph,
                        metadataType.relationUri)

                query.run()
                val result =  query.returnFuture.get(10, TimeUnit.SECONDS) as BGReturnMetadata
                // In the completion, add the metadata to the relations' metadata map.

                val dataType = metadataType.dataType

                val metaData = when (dataType) {
                    BGRelationMetadata.DataType.STRING -> {
                        // TODO: Handle arrays / multiple data points.
                        BGRelationMetadata(metadataType, result.values.first())
                    }
                    BGRelationMetadata.DataType.NUMBER -> {
                        BGRelationMetadata(metadataType, result.values.first().toDouble())

                    }}
                // Add this metadata entry into the relation's metadata map.
                relation.metadata[metadataType] = metaData
            }
        }
        completion()
    }
}