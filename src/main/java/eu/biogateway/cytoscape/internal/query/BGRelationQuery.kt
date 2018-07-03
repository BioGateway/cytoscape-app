package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGRelation
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import eu.biogateway.cytoscape.internal.util.Utility

class BGRelationQueryImplementation(val queryString: String, var returnType: BGReturnType): BGRelationQuery(returnType) {
    override fun generateQueryString(): String {
        return queryString
    }
}

abstract class BGRelationQuery(type: BGReturnType): BGQuery(type) {
    var returnDataFilter: ((BGRelation) -> Boolean)? = null

    init {
        taskMonitorTitle = "Searching for relations..."
        parsingBlock = {
            val returnRelationsData = parser.parseRelations(it, type, taskMonitor)
                returnDataFilter?.let {
                    returnRelationsData.relationsData = ArrayList(returnRelationsData.relationsData.filter(it))
                    returnRelationsData.unloadedNodes?.let {
                        returnRelationsData.unloadedNodes = Utility.removeNodesNotInRelationSet(it, returnRelationsData.relationsData).toList()
                    }
                }
                returnData = returnRelationsData
                runCompletions()
            }
    }
}

