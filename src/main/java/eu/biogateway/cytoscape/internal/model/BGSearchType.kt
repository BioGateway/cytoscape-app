package eu.biogateway.cytoscape.internal.model

class BGSearchType(val id: String, val title: String, val nodeType: BGNodeType, val returnType: String = "json", val restPath: String, val arraySearch: Boolean = false, val httpMethod: HTTPOperation, var prefix: String? = null, var parameters: String? = null) {
    enum class HTTPOperation {
        GET,
        POST
    }
    override fun toString(): String {
        return title
    }

    fun run(searchValues: String) {

    }

    private fun runWithArray(values: Array<String>) {
        //        val query = BGMultiNodeFetchMongoQuery(nodeList, "genesForSymbols", null, null, BGReturnType.NODE_LIST_DESCRIPTION_STATUS)
//                query.addCompletion(queryCompletion)
//                BGServiceManager.execute(query)
    }

    private fun runWithValue(value: String) {

    }

}