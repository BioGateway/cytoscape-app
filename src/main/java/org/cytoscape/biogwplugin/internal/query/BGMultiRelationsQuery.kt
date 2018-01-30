package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGMultiRelationsQuery(serviceManager: BGServiceManager, val queryString: String, var returnType: BGReturnType): BGQuery(serviceManager, returnType) {

    override fun generateQueryString(): String {
        return queryString
    }

    init {
        taskMonitorTitle = "Searching for relations..."
        parsingBlock = {
            returnData = parser.parseRelations(it, returnType, taskMonitor)
                taskMonitor?.setTitle("Loading results...")
                runCompletions()
        }
    }

}