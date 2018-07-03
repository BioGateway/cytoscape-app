package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.parser.BGReturnType

// queryType is the dictionary search type, corresponding to the REST call path for the dictionary server, i.e. "fetch".
class BGMultiNodeFetchMongoQuery(val searchTerms: Collection<String>, queryMethod: String, val nodeType: String? = null, val extraFields: Collection<String>? = null, returnType: BGReturnType = BGReturnType.NODE_LIST_DESCRIPTION): BGQuery(returnType, queryMethod) {

    override fun generateQueryString(): String {
        val terms = searchTerms.map { "\"$it\"" }.reduce { list, node -> list + ", "+node}
        val nodeDictName = nodeType ?: "all"
        val fieldString = extraFields?.fold(", \"extraFields\": [") { acc, s -> acc+"\""+s+"\"" }?.plus("]") ?: ""
        return "{ \"returnType\": \"tsv\", \"nodeType\": \"$nodeDictName\", \"terms\": [$terms]$fieldString}"
    }
}