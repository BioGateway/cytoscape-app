package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.model.BGRelation
import eu.biogateway.app.internal.model.BGRelationType
import eu.biogateway.app.internal.parser.BGReturnType
import eu.biogateway.app.internal.util.Utility
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

class BGMultiNodeRelationQuery(val nodeUris: Collection<String>, val relationType: BGRelationType, val direction: BGRelationDirection): AbstractTask(), Runnable {
    private var taskMonitor: TaskMonitor? = null
    private var returnData: BGReturnRelationsData? = null
    private val completionBlocks = ArrayList<(BGReturnData?) -> Unit>()
    var returnDataFilter: ((BGRelation) -> Boolean)? = null

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
                    relation.extraTableData.add(it.size)
                    filteredRelations.add(relation)
                }
            }
        }
        return filteredRelations
    }


    override fun run() {

        var relations = HashSet<BGRelation>()
        var unloadedNodes = HashSet<BGNode>()

        for (nodeUri in nodeUris) {
            val query = BGFindRelationForNodeQuery(relationType, nodeUri, direction)
            query.returnDataFilter = returnDataFilter
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Expected relations data!")
                relations.addAll(returnData.relationsData)

                returnData.unloadedNodes?.let {
                    unloadedNodes.addAll(it)
                }
            }
            query.run()
        }
        var columnNames = arrayOf("From Node", "Relation Type", "To Node")
        returnData = BGReturnRelationsData(BGReturnType.RELATION_TRIPLE_GRAPHURI, columnNames)

        if (minCommonRelations != 0) {
            // It now only finds relations to nodes with ALL the searched nodes in common.
            //val minCommonRelations = nodeUris.size
            val commonRelations = findCommonRelations(relations, minCommonRelations)
            val filteredUnloadedNodes = Utility.removeNodesNotInRelationSet(unloadedNodes, commonRelations)
            returnData?.relationsData?.addAll(commonRelations)
            returnData?.unloadedNodes = filteredUnloadedNodes.toList()
            returnData?.columnNames = arrayOf("From Node", "Relation Type", "To Node", "Common Relations")

        } else if (minCommonRelations == -1) {
            val commonRelations = findCommonRelations(relations, minCommonRelations)
            val filteredUnloadedNodes = Utility.removeNodesNotInRelationSet(unloadedNodes, commonRelations)
            returnData?.relationsData?.addAll(commonRelations)
            returnData?.unloadedNodes = filteredUnloadedNodes.toList()
            returnData?.columnNames = arrayOf("From Node", "Relation Type", "To Node", "Common Relations")
        } else {
            returnData?.relationsData?.addAll(relations)
            returnData?.unloadedNodes = unloadedNodes.toList()
        }

        runCompletions()
    }
}