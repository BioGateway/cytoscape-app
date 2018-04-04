package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNode
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

internal class BGFetchAggregatedRelationForNodeQueryTest {

    private val ppiNode = BGNode("http://identifiers.org/intact/EBI-2000925")
    private val goaNode = BGNode("http://www.semantic-systems-biology.org/ssb/GOA_Q16236-involved_in-0034599")
    private val tftgNode = BGNode("http://www.semantic-systems-biology.org/ssb/15574414_3_MAFG_MAFG")


    @Test
    internal fun goaRelationTest() {
        val query = BGFetchAggregatedRelationForNodeQuery(BGServiceManager(), goaNode)
        Thread(query).start()
        validate(query.futureReturnData, 1)
    }

    @Test
    internal fun TFTGRelationTest() {
        val query = BGFetchAggregatedRelationForNodeQuery(BGServiceManager(), tftgNode)
        Thread(query).start()
        validate(query.futureReturnData, 1)
    }


    private fun validate(result: CompletableFuture<BGReturnData>, expectedResultCount: Int) {
        val data = result.get() as BGReturnRelationsData
        assert(data.relationsData.count() == expectedResultCount)
    }

}