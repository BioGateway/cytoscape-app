package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import javax.management.relation.Relation

class BGMultiNodeRelationQuery(val serviceManager: BGServiceManager, val nodeUris: Collection<String>, val relationType: BGRelationType, val direction: BGRelationDirection): AbstractTask(), Runnable {
    private var taskMonitor: TaskMonitor? = null
    private var returnData: BGReturnRelationsData? = null
    private val completionBlocks = ArrayList<(BGReturnData?) -> Unit>()

    var minCommonRelations = 0

    fun addCompletion(completion: (BGReturnData?) -> Unit) {
        completionBlocks.add(completion)
    }
    private fun runCompletions() {
        for (completion in completionBlocks) {
            completion(returnData)
        }
    }

    override fun run(taskMonitor: TaskMonitor?) {
        this.taskMonitor = taskMonitor
        run()
    }

    /// If minCommonSourceNodes is -1, it will be set to the highest value that would return some relations.
    private fun findCommonRelations(relations: Collection<BGRelation>, minCommonSourceNodes: Int): HashSet<BGRelation> {
        var nodeMap = HashMap<BGNode, HashSet<BGNode>>()

        for (relation in relations) {
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

        var filteredRelations = HashSet<BGRelation>()

        var minCommonSourceNodes = minCommonSourceNodes

        if (minCommonSourceNodes == -1) {
            val highestNumber = nodeMap.values.map { it.size }.max()
            println("Highest number of connected: " + highestNumber)
            highestNumber?.let {
                minCommonSourceNodes = it
            }
        }
        for (relation in relations) {
            val foundNode = when (direction) {
                BGRelationDirection.TO -> relation.fromNode
                BGRelationDirection.FROM -> relation.toNode
            }
            nodeMap.get(foundNode)?.let {
                if (it.size >= minCommonSourceNodes) {
                    filteredRelations.add(relation)
                }
            }
        }
        return filteredRelations
    }

    private fun removeNodesNotInRelationSet(nodes: Collection<BGNode>, relations: Collection<BGRelation>): Collection<BGNode> {
        var allNodes = relations.map { it.toNode }.toHashSet().union(relations.map { it.fromNode }.toHashSet())
        return nodes.filter { allNodes.contains(it) }.toHashSet()
    }

    override fun run() {

        var columnNames: Array<String>? = null
        var relations = HashSet<BGRelation>()
        var unloadedNodes = HashSet<BGNode>()

        for (nodeUri in nodeUris) {
            val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Expected relations data!")
                relations.addAll(returnData.relationsData)
                columnNames = returnData.columnNames
                returnData.unloadedNodes?.let {
                    unloadedNodes.addAll(it)
                }
            }
            query.run()
        }

        columnNames?.let {
            returnData = BGReturnRelationsData(BGReturnType.RELATION_TRIPLE, it)

            if (minCommonRelations != 0) {
                // It now only finds relations to nodes with ALL the searched nodes in common.
                //val minCommonRelations = nodeUris.size
                val commonRelations = findCommonRelations(relations, minCommonRelations)
                val filteredUnloadedNodes = removeNodesNotInRelationSet(unloadedNodes, commonRelations)
                returnData?.relationsData?.addAll(commonRelations)
                returnData?.unloadedNodes = filteredUnloadedNodes.toList()
            } else if (minCommonRelations == -1) {
                val commonRelations = findCommonRelations(relations, minCommonRelations)
                val filteredUnloadedNodes = removeNodesNotInRelationSet(unloadedNodes, commonRelations)
                returnData?.relationsData?.addAll(commonRelations)
                returnData?.unloadedNodes = filteredUnloadedNodes.toList()
            } else {
                returnData?.relationsData?.addAll(relations)
                returnData?.unloadedNodes = unloadedNodes.toList()
            }
        }
        runCompletions()
    }


}