package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.StringReader

class BGNodeSearchQuery(serviceManager: BGServiceManager, val queryString: String, returnType: BGReturnType): BGQuery(serviceManager, returnType) {

    init {
        taskMonitorTitle = "Searching for nodes..."
        parseType = BGParsingType.TO_ARRAY
    }

    override fun generateQueryString(): String {
        return queryString
    }
}