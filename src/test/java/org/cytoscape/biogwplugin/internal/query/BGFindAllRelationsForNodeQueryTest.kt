package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import org.junit.jupiter.api.Test

internal class BGFindAllRelationsForNodeQueryTest {

    val ppiUri = "http://identifiers.org/intact/EBI-3932015"


    @Test
    internal fun PPIFromTest() {
        val query = BGFindAllRelationsForNodeQuery(ppiUri, BGRelationDirection.FROM)
        Thread(query).start()
        val data = query.futureReturnData.get() as BGReturnRelationsData
        println("[${this::class.java}] : Found "+ data.relationsData.count()+" relations.")
        assert(data.relationsData.count() > 0)
    }
}