package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.BGReturnType

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