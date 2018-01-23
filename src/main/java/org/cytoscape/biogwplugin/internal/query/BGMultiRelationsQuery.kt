package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGMultiRelationsQuery(serviceManager: BGServiceManager, override var queryString: String, var returnType: BGReturnType): BGQuery(serviceManager, returnType) {

    init {
        taskMonitorTitle = "Searching for relations..."
        parsingBlock = {
            parser.parseRelations(it, returnType, taskMonitor) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                taskMonitor?.setTitle("Loading results...")
                runCompletions()
            }
        }
    }

}