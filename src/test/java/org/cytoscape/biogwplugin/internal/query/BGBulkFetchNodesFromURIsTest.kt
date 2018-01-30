package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.util.Utility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

internal class BGBulkFetchNodesFromURIsTest {

    private val uniprotIDs = arrayListOf("O15525", "P05181", "Q08257", "P09601")
    private val GOTerms = arrayListOf("GO_0034599", "GO_0036091", "GO_0055114", "GO_0043619")

    private var serviceManager = BGServiceManager()

    @AfterEach
    internal fun tearDown() {
        // Reset the serviceManager
        serviceManager = BGServiceManager()
    }

    @Test
    internal fun importUniprotNodesTest() {
        val uniprotNodeList = uniprotIDs.map { Utility.generateUniprotURI(it) }
        val query = BGBulkImportNodesFromURIs(serviceManager, BGNodeType.Protein, uniprotNodeList)
        Thread(query).start()
        validate(query.futureReturnData, 4)
    }

    @Test
    internal fun importGOTermsTest() {
        val goNodeList = GOTerms.map { Utility.generateGOTermURI(it) }
        val query = BGBulkImportNodesFromURIs(serviceManager, BGNodeType.GO, goNodeList)
        Thread(query).start()
        validate(query.futureReturnData, 4)
    }

    private fun validate(resultFuture: CompletableFuture<BGReturnData>, expectedResultCount: Int): Boolean {

        val data = resultFuture.get() as BGReturnNodeData
        return (data.nodeData.values.count() == expectedResultCount)
    }


}