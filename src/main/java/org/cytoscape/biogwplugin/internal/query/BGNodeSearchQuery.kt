package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGNodeSearchQuery(serviceManager: BGServiceManager, val queryString: String, returnType: BGReturnType): BGQuery(serviceManager, returnType) {

    init {
        taskMonitorTitle = "Searching for nodes..."
        parseType = BGParsingType.TO_ARRAY
    }

    override fun generateQueryString(): String {
        return queryString
    }
}