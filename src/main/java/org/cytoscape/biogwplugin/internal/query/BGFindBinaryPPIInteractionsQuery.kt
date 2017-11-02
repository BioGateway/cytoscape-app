package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

class BGFindBinaryPPIInteractionsQuery(serviceManager: BGServiceManager, val nodeUri: String): BGQuery(serviceManager, BGReturnType.RELATION_TRIPLE, serviceManager.server.parser) {

    init {
        parsingBlock = {
            parser.parseRelations(it, type, taskMonitor) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
        taskMonitorText = "Searching for binary protein interactions..."
    }

    override var queryString: String
        get() = generateQueryString(nodeUri)
        set(value) {}

    private fun generateQueryString(nodeUri: String): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
                "PREFIX fromNode: <"+nodeUri+">\n" +
                "SELECT DISTINCT ?toNode <http://purl.obolibrary.org/obo/RO_0002436> fromNode: \n" +
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

//        return "BASE <http://www.semantic-systems-biology.org/>\n" +
//                "PREFIX has_agent: <http://semanticscience.org/resource/SIO_000139>\n" +
//                "PREFIX fromNode: <"+nodeUri+">\n" +
//                "SELECT DISTINCT ?toNode <http://purl.obolibrary.org/obo/RO_0002436> fromNode: \n" +
//                "WHERE {\n" +
//                "FILTER (fromNode: != ?toNode)" +
//                "GRAPH <intact> {\n" +
//                "?ppi has_agent: ?toNode .\n" +
//                "?ppi has_agent: fromNode: .\n" +
//                "}}\n" +
//                "GROUP BY ?toNode\n" +
//                "HAVING (COUNT(distinct ?toNode) < 2)\n"
    }
}

class BGFindBinaryPPIInteractionsForMultipleNodesQuery(val serviceManager: BGServiceManager, val nodeUris: Collection<String>): AbstractTask() {
    private var returnData: BGReturnRelationsData? = null
    private val completionBlocks = ArrayList<(BGReturnData?) -> Unit>()

    fun addCompletion(completion: (BGReturnData?) -> Unit) {
        completionBlocks.add(completion)
    }
    private fun runCompletions() {
        for (completion in completionBlocks) {
            completion(returnData)
        }
    }

    override fun run(taskMonitor: TaskMonitor?) {
        var relations = HashSet<BGRelation>()
        var unloadedNodes = HashSet<BGNode>()

        for (nodeUri in nodeUris) {
            val query = BGFindBinaryPPIInteractionsQuery(serviceManager, nodeUri)
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
        returnData?.relationsData?.addAll(relations)
        returnData?.unloadedNodes = unloadedNodes.toList()

        runCompletions()
    }
}