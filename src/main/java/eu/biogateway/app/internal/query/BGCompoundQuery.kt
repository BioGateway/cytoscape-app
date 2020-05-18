package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.model.BGRelation
import eu.biogateway.app.internal.parser.BGReturnType
import eu.biogateway.app.internal.util.Utility
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.util.concurrent.CompletableFuture

abstract class BGCompoundRelationQuery(val nodeUris: Collection<String>, val columnNames: Array<String>, val createFuture: (String) -> CompletableFuture<BGReturnData>): AbstractTask(), Runnable {
    var returnFuture = CompletableFuture<BGReturnRelationsData>()
    var minCommonRelations = 0

    override fun run(taskMonitor: TaskMonitor?) {
        run()
    }


    override fun run() {
        val relationSet = HashSet<BGRelation>() // Using a set to avoid duplicates.
        val unloadedNodes = HashSet<BGNode>()

        // The createFuture function is provided when creating the query, and abstracts the query type out of this class.
        val futures = nodeUris.map { createFuture(it) }

        for (future in futures) {
            val data = future.get() as BGReturnRelationsData
            relationSet.addAll(data.relationsData)
            data.unloadedNodes?.let {
                unloadedNodes.addAll(it)
            }
        }

        val returnData = BGReturnRelationsData(BGReturnType.RELATION_TRIPLE_GRAPHURI, columnNames)

        if (minCommonRelations != 0) {
            val commonRelations = findCommonRelations(relationSet)
            val filteredUnloadedNodes = Utility.removeNodesNotInRelationSet(unloadedNodes, commonRelations)
            returnData.relationsData.addAll(commonRelations)
            returnData.unloadedNodes = if (filteredUnloadedNodes.count() > 0) filteredUnloadedNodes.toList() else null
            returnData.resultTitle = "Result data for over "+minCommonRelations+" common relations."
            returnData.columnNames = columnNames.plus("Common Relations")
        } else {
            returnData.relationsData.addAll(relationSet)
            returnData.unloadedNodes = if (unloadedNodes.count() > 0) unloadedNodes.toList() else null
        }

        returnFuture.complete(returnData)
    }

    private fun findCommonRelations(relations: Collection<BGRelation>): HashSet<BGRelation> {
        val nodeMap = HashMap<BGNode, HashSet<BGNode>>()

        for (relation in relations) {
            val toNodeIsSearchNode = nodeUris.contains(relation.toNode.uri)
            val direction = when (toNodeIsSearchNode) {
                true -> BGRelationDirection.TO
                false -> BGRelationDirection.FROM
            }

            val searchNode = when (direction) {
                BGRelationDirection.TO -> relation.toNode
                BGRelationDirection.FROM -> relation.fromNode
            }
            val foundNode = when (direction) {
                BGRelationDirection.TO -> relation.fromNode
                BGRelationDirection.FROM -> relation.toNode
            }

            // Init the hashset so we avoid nulls.
            if (!nodeMap.contains(foundNode)) {
                nodeMap[foundNode] = HashSet()
            }
            nodeMap[foundNode]?.add(searchNode)
        }

        val filteredRelations = HashSet<BGRelation>()

        if (minCommonRelations == -1) {
            val highestNumber = nodeMap.values.map { it.size }.max()
            println("Highest number of connected: " + highestNumber)
            highestNumber?.let {
                minCommonRelations = it
            }
        }

        for (relation in relations) {
            val toNodeIsSearchNode = nodeUris.contains(relation.toNode.uri)
            val direction = when (toNodeIsSearchNode) {
                true -> BGRelationDirection.TO
                false -> BGRelationDirection.FROM
            }

            val foundNode = when (direction) {
                BGRelationDirection.TO -> relation.fromNode
                BGRelationDirection.FROM -> relation.toNode
            }

            nodeMap.get(foundNode)?.let {
                if (it.size >= minCommonRelations) {
                    filteredRelations.add(relation)
                    relation.extraTableData.add(it.size)
                }
            }
        }
        return filteredRelations
    }
}