package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNodeType
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

internal class BGBulkImportNodesQueryTest {

    private val geneNames = arrayListOf("FOXP3", "TP53", "NOS2", "HMOX1")

    @Test
    internal fun geneNameSearchTest() {

        val query = BGBulkImportNodesQuery(geneNames, BGNodeType.Gene)
        Thread(query).start()
        validate(query.futureReturnData, 4)
    }

    private fun validate(resultFuture: CompletableFuture<BGReturnData>, expectedResultCount: Int): Boolean {
        val data = resultFuture.get() as BGReturnNodeData
        return (data.nodeData.values.count() == expectedResultCount)
    }
}

/*

   when (queryType) {
           QueryType.NAME_SEARCH -> {
               val query = BGBulkImportNodesQuery(serviceManager, nodeList, nodeType)
               query.addCompletion(queryCompletion)
               serviceManager.taskManager?.execute(TaskIterator(query))
           }
           BGQueryBuilderController.QueryType.UNIPROT_LOOKUP -> {
               val uniprotNodeList = nodeList.map { Utility.generateUniprotURI(it) }
               val query = BGBulkImportNodesFromURIs(serviceManager, nodeType, uniprotNodeList)
               query.addCompletion(queryCompletion)
               serviceManager.taskManager?.execute(TaskIterator(query))
           }
           BGQueryBuilderController.QueryType.GO_LOOKUP -> {
               val goNodeList = nodeList.map { Utility.generateGOTermURI(it) }
               val query = BGBulkImportNodesFromURIs(serviceManager, nodeType, goNodeList)
               query.addCompletion(queryCompletion)
               serviceManager.taskManager?.execute(TaskIterator(query))
           }
           BGQueryBuilderController.QueryType.NOT_SET -> {
               throw Exception("Invalid query dataType!")
           }
       }



    */