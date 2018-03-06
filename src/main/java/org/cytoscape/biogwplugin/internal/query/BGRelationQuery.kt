package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.util.Utility

class BGRelationQueryImplementation(serviceManager: BGServiceManager, val queryString: String, var returnType: BGReturnType): BGRelationQuery(serviceManager, returnType) {
    override fun generateQueryString(): String {
        return queryString
    }
}

abstract class BGRelationQuery(serviceManager: BGServiceManager, type: BGReturnType): BGQuery(serviceManager, type) {
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

