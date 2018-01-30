package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager

class BGFindBinaryPPIsForMultipleNodesQuery(serviceManager: BGServiceManager, nodeUris: Collection<String>, executor: (Runnable) -> Unit): BGCompoundRelationQuery(serviceManager,
        nodeUris,
        arrayOf("Protein", "Relation", "Protein"), { node ->
        val query = BGFindBinaryPPIsQuery(serviceManager, node)
        executor(query)
        query.futureReturnData
    })