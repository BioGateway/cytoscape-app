package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

class BGFindBinaryPPIInteractionsQuery(serviceManager: BGServiceManager, val nodeUri: String): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE, serviceManager.server.parser) {

    init {
        taskMonitorTitle = "Searching for binary protein interactions..."
    }

    override var queryString: String = ""
        get() = generateQueryString(nodeUri)

    private fun generateQueryString(nodeUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
                "PREFIX fromNode: <"+nodeUri+">\n" +
                "SELECT DISTINCT ?toNode <intact> <http://purl.obolibrary.org/obo/RO_0002436> fromNode: \n" +
                "WHERE {\n" +
                "FILTER (?count = 2)\n" +
                "FILTER (fromNode: != ?toNode)\n" +
                "GRAPH <intact> {\n" +
                "?ppi has_agent: ?toNode } \n" +
                "{ SELECT ?ppi COUNT(?node) AS ?count\n" +
                "WHERE {\n" +
                "GRAPH <intact> {\n" +
                "?ppi has_agent: ?node .\n" +
                "?ppi has_agent: fromNode: .\n" +
                "}}}}"
    }
}

class BGFindBinaryPPIInteractionsForMultipleNodesQuery(val serviceManager: BGServiceManager, val nodeUris: Collection<String>): AbstractTask() {
    private var returnData: BGReturnRelationsData? = null
    private val completionBlocks = ArrayList<(BGReturnData?) -> Unit>()
    var minCommonRelations = 0
    var returnDataFilter: ((BGRelation) -> Boolean)? = null


    fun addCompletion(completion: (BGReturnData?) -> Unit) {
        completionBlocks.add(completion)
    }
    private fun runCompletions() {
        for (completion in completionBlocks) {
            completion(returnData)
        }
    }

    private fun findCommonRelations(relations: Collection<BGRelation>): HashSet<BGRelation> {
        var nodeMap = HashMap<BGNode, HashSet<BGNode>>()
        // TODO: Update the code to remove this direction. It's just there to satisfy the copy-pasted code below. All PPI relations are bi-directional anyway.

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

        var filteredRelations = HashSet<BGRelation>()

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


    override fun run(taskMonitor: TaskMonitor?) {
        var relations = HashSet<BGRelation>()
        var unloadedNodes = HashSet<BGNode>()

        for (nodeUri in nodeUris) {
            val query = BGFindBinaryPPIInteractionsQuery(serviceManager, nodeUri)
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

        val columnNames = arrayOf("Protein", "Relation", "Protein")
        returnData = BGReturnRelationsData(BGReturnType.RELATION_TRIPLE, columnNames)
        if (minCommonRelations != 0) {
            // It now only finds relations to nodes with ALL the searched nodes in common.

            val commonRelations = findCommonRelations(relations)
            val filteredUnloadedNodes = Utility.removeNodesNotInRelationSet(unloadedNodes, commonRelations)
            returnData?.relationsData?.addAll(commonRelations)
            returnData?.unloadedNodes = filteredUnloadedNodes.toList()
            returnData?.resultTitle = "Result data for over "+minCommonRelations+" common relations."
            returnData?.columnNames = arrayOf("From Node", "Relation Type", "To Node", "Common Relations")
        } else {
            returnData?.relationsData?.addAll(relations)
            returnData?.unloadedNodes = unloadedNodes.toList()
        }

        runCompletions()
    }
}