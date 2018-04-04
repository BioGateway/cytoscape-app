package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager

class BGFindBinaryPPIsForMultipleNodesQuery(serviceManager: BGServiceManager, nodeUris: Collection<String>, executor: (Runnable) -> Unit): BGCompoundRelationQuery(serviceManager,
        nodeUris,
        arrayOf("Protein", "Relation", "Protein"), { node ->
        val query = BGFindBinaryPPIsQuery(serviceManager, node)
        executor(query)
        query.futureReturnData
    })