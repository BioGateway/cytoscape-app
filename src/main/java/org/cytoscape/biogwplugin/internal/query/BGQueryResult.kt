package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.model.BGRelation

/**
 * Created by sholmas on 23/05/2017.
 */

enum class ResultType() {
    NODE_DATA, RELATION_DATA, NODE_EDGE_NODE, NODE_EDGE_NODE_EDGE_NODE
}
enum class ResultStatus() {
    OK, ERROR, CANCELLED
}

open class BGQueryResult(var type: ResultType, var query: BGQuery, var resultStatus: ResultStatus)

class BGQueryResultNodeData(type: ResultType, query: BGQuery, resultStatus: ResultStatus, var resultData: ArrayList<String>) : BGQueryResult(type, query, resultStatus)
class BGQueryResultRelationsData(type: ResultType, query: BGQuery, resultStatus: ResultStatus, var resultData: ArrayList<BGRelation>) : BGQueryResult(type, query, resultStatus)
