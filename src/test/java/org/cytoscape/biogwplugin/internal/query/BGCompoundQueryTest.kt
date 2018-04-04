package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNodeType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BGCompoundQueryTest {

    val nodeUris = arrayListOf("http://identifiers.org/uniprot/O15525", "http://identifiers.org/uniprot/P63162", "http://identifiers.org/uniprot/Q5RL73")

    @Test
    internal fun findBinaryPPITest() {
        val serviceManager = BGServiceManager()
        val query = BGFindBinaryPPIsForMultipleNodesQuery(serviceManager, nodeUris) { query -> Thread(query).start() }
        Thread(query).start()

        val results = query.returnFuture.get()
        val relations = results.relationsData
        assert(relations.count() > 0)
        for (relation in relations) {
            assert(relation.fromNode.type == BGNodeType.Protein && relation.toNode.type == BGNodeType.Protein)
            assertNotNull(serviceManager.cache.getRelationTypesForURI(relation.relationType.uri))
        }
        assert(results.columnNames.size == 3)
    }

    @Test
    internal fun findCommonBinaryPPIsTest() {
        val serviceManager = BGServiceManager()
        val query = BGFindBinaryPPIsForMultipleNodesQuery(serviceManager, nodeUris) { query -> query.run() }
        query.minCommonRelations = 2
        Thread(query).start()

        val results = query.returnFuture.get()
        val relations = results.relationsData
        assert(relations.count() == 4)
        assert(results.columnNames.size == 4)
    }
}