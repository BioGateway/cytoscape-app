package org.cytoscape.biogwplugin.internal.query
import org.cytoscape.biogwplugin.internal.parser.BGQueryType
import java.io.InputStream
import java.net.URL
import java.net.URLEncoder

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGRelationDirection {
    TO, FROM
}

abstract class BGQuery(val serverPath: String, val type: BGQueryType) {
    var completionBlocks: ArrayList<(BGReturnData) -> Unit> = ArrayList()
    abstract var returnData: BGReturnData
    abstract var queryString: String

    fun addCompletion(completion: (BGReturnData) -> Unit) {
        completionBlocks.add(completion)
    }
    fun runCompletions() {
        for (completion in completionBlocks) {
            completion(returnData)
        }
    }

    fun encodeUrl(): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"

        val queryURL = URL(serverPath + "?query" + URLEncoder.encode(queryString, "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)

        return queryURL
    }
}



class BGNodeQuery(serverPath: String, val nodeUri: String): BGQuery(serverPath, BGQueryType.NODE_QUERY) {

    override var queryString = "BASE   <http://www.semantic-systems-biology.org/> \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  \n" +
            "PREFIX uniprot:  <http://identifiers.org/uniprot/>  \n" +
            "PREFIX term_id: <"+ nodeUri +">  \n" +
            "PREFIX graph: <cco>  \n" +
            "SELECT term_id: ?label ?description\n" +
            "WHERE {  \n" +
            " GRAPH graph: {  \n" +
            "  term_id: skos:prefLabel ?label .\n" +
            " term_id: skos:definition ?description .\n" +
            " } } \n"

    override var returnData: BGReturnData = BGReturnNodeData(type)
}



