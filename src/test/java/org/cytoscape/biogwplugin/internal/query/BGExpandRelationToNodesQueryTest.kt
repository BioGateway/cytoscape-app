package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BGExpandRelationToNodesQueryTest {

    var serviceManager = BGServiceManager()

    @Test
    internal fun expandPPIRelationTest() {
        val relationTypeUri = "http://purl.obolibrary.org/obo/RO_0002436"
        val relationType = serviceManager.cache.getRelationTypeForURIandGraph(relationTypeUri, "intact")
        assertNotNull(relationType)
        val fromUri = "http://identifiers.org/uniprot/O15525"
        val toUri = "http://identifiers.org/uniprot/Q16236"
        val query = BGExpandRelationToNodesQuery(serviceManager, fromUri, toUri, relationType!!, BGReturnType.RELATION_TRIPLE_GRAPHURI)
        val resultFuture = query.futureReturnData

        val thread = Thread(query).start()

        val result = resultFuture.get()
        assertNotNull(result)
        val data = result as BGReturnRelationsData
        assertNotNull(data)
        assert(data.relationsData.count() == 4)
    }
}