package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager

class BGFindBinaryPPIsForMultipleNodesQuery(nodeUris: Collection<String>, executor: (Runnable) -> Unit): BGCompoundRelationQuery(
        nodeUris,
        arrayOf("Protein", "Relation", "Protein"), { node ->
        val query = BGFindBinaryPPIsQuery(node)
        executor(query)
        query.futureReturnData
    })