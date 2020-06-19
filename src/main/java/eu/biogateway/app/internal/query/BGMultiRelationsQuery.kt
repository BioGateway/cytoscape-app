package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.parser.BGReturnType

class BGMultiRelationsQuery(val queryString: String, var returnType: BGReturnType): BGQuery(returnType) {

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