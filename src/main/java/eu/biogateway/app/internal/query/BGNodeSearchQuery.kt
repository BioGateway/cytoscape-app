package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.parser.BGReturnType

class BGNodeSearchQuery(val queryString: String, returnType: BGReturnType): BGQuery(returnType) {

    init {
        taskMonitorTitle = "Searching for nodes..."
        parseType = BGParsingType.TO_ARRAY
    }

    override fun generateQueryString(): String {
        return queryString
    }
}