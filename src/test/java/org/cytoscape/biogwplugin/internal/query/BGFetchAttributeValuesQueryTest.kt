package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.junit.jupiter.api.Test

internal class BGFetchAttributeValuesQueryTest {

    private val nodeUri = "http://identifiers.org/uniprot/O15525"
    private val relationUri = "http://www.w3.org/2004/02/skos/core#prefLabel"

    @Test
    internal fun fetchDescriptionTest() {
        val query = BGFetchAttributeValuesQuery(BGServiceManager(), nodeUri, relationUri, "cco", BGRelationDirection.FROM)
        Thread(query).start()

        val data = query.futureReturnData.get() as BGReturnMetadata

        assert(data.values.contains("MAFG_HUMAN"))

    }
}