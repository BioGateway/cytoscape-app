package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.junit.jupiter.api.Test

internal class BGFindAllRelationsForNodeQueryTest {

    val ppiUri = "http://identifiers.org/intact/EBI-3932015"


    @Test
    internal fun PPIFromTest() {
        val query = BGFindAllRelationsForNodeQuery(BGServiceManager(), ppiUri, BGRelationDirection.FROM)
        Thread(query).start()
        val data = query.futureReturnData.get() as BGReturnRelationsData
        println("[${this::class.java}] : Found "+ data.relationsData.count()+" relations.")
        assert(data.relationsData.count() > 0)
    }
}