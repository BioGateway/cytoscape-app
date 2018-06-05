package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.InputStreamReader

class BGNodeFetchMongoQuery(val nodeUri: String): BGQuery(BGReturnType.NODE_LIST_DESCRIPTION, "fetch") {

    override fun generateQueryString(): String {

        return "{ \"returnType\": \"tsv\", \"terms\": [\"" + nodeUri + "\"] }"
    }
}

class BGMultiNodeFetchMongoQuery(val searchTerms: Collection<String>, queryType: String): BGQuery(BGReturnType.NODE_LIST_DESCRIPTION, queryType) {

    override fun generateQueryString(): String {
        val terms = searchTerms.map { "\"$it\"" }.reduce { list, node -> list + ", "+node}
        return "{ \"returnType\": \"tsv\", \"terms\": [$terms]}"
    }
}

class BGNodeFetchQuery(val nodeUri: String): BGQuery(BGReturnType.NODE_LIST_DESCRIPTION ) {

    /// This is running synchronously and without the main HTTPClient.

    override fun run() {
        taskMonitor?.setTitle("Searching for nodes...")
        val stream = encodeUrl()?.openStream()
        if (stream != null) {
            taskMonitor?.setTitle("Loading results...")
            val reader = BufferedReader(InputStreamReader(stream))
            returnData = parser.parseNodesToTextArray(reader, type)
            runCompletions()
            futureReturnData.complete(returnData)
        }
    }

    /*
    override fun generateQueryString(): String {
        return "{ \"returnType\": \"tsv\", \"uris\": [\""+nodeUri+"\"] }"
    }
*/

    override fun generateQueryString(): String {
        return "BASE   <http://www.semantic-systems-biology.org/> \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  \n" +
                "PREFIX term_id: <" + nodeUri + ">  \n" +
                "SELECT DISTINCT term_id: ?label ?description\n" +
                "WHERE {\n" +
                "term_id: skos:prefLabel ?label .\n" +
                "term_id: skos:definition ?description .\n" +
                "} \n"
    }
}