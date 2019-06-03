package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import java.lang.Exception
import java.util.concurrent.TimeUnit

class BGBulkImportNodesFromURIs(val nodeType: BGNodeType, val nodeUris: Collection<String>): BGQuery(BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {


    override fun run() {
        searchDictionaryForNodes()
        futureReturnData.complete(returnData)
        runCompletions()
    }

    override fun generateQueryString(): String {
        // This class overrides run() so it will not need this method.
        return ""
    }

    private fun searchDictionaryForNodes() {
        // This will fetch the nodes, but with taxa as URIs
        val query = BGMultiNodeFetchMongoQuery(nodeUris, "fetch", nodeType.id, arrayListOf("taxon"), BGReturnType.NODE_LIST_DESCRIPTION_TAXON)
        query.run()
        val nodeResults = query.futureReturnData.get(10, TimeUnit.SECONDS) as BGReturnNodeData
        if (nodeResults.nodeData.size == 0) throw Exception("No results found.")
        val taxonUris = nodeResults.nodeData.map { it.value.taxon }.filterNotNull().toHashSet()
        val taxaMap = searchDictionaryForTaxa(taxonUris)

        // For the results, we want to use the taxa names instead of URIs where possible.
        for (node in nodeResults.nodeData.values) {
            node.taxon?.let {
                node.taxon = taxaMap[it] ?: node.taxon
            }
        }

        // We return the nodes that we got from the node fetch query above.
        returnData = nodeResults
    }

    private fun searchDictionaryForTaxa(taxonUris: Collection<String>): HashMap<String, String> {
        val query = BGMultiNodeFetchMongoQuery(taxonUris, "fetch", "taxon")
        val taxaNames = HashMap<String, String>()
        query.run()
        val taxons = query.futureReturnData.get() as BGReturnNodeData
        for ((uri, node) in taxons.nodeData) {
            taxaNames[uri] = node.name
        }
        return taxaNames
    }

}


class BGBulkImportNodesFromURIsOld(val nodeType: BGNodeType, val nodeUris: Collection<String>): BGQuery(BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {

    init {
        taskMonitorTitle = "Searching for nodes..."
        parseType = BGParsingType.TO_ARRAY
    }

    override fun generateQueryString(): String {

        val nodeTypeGraph = if (nodeType.metadataGraph != null) "<" +nodeType.metadataGraph+">" else "?anyGraph"

        var uriList = ""
        for (nodeName in nodeUris) {
            uriList += "<"+nodeName+">,"
        }
        uriList = uriList.removeSuffix(",")


        val taxaGraph = when (nodeType.id) {
            "gene", "protein" -> "?uri inheres_in: ?taxon . \n } GRAPH taxaGraph: {\n" +
                    "?taxon rdfs:subClassOf sio:SIO_010000 . \n" +
                    "?taxon skos:prefLabel ?taxaName }\n"
            else -> {
                "\n }"
            }
        }
        val taxaName = when (nodeType.id) {
            "gene", "protein" -> "?taxaName"
            else -> {
                "'N/A'"
            }
        }

        val queryString = "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "PREFIX sio:  <http://semanticscience.org/resource/>  \n" +
                "PREFIX taxaGraph: <cco>\n" +
                "PREFIX inheres_in: <http://purl.obolibrary.org/obo/RO_0000052>\n" +
                "SELECT DISTINCT ?uri ?name ?definition "+taxaName+"\n" +
                "WHERE {  \n" +
                "FILTER (?uri IN ("+uriList+"))" +
                "GRAPH "+nodeTypeGraph+" { \n" +
                "?uri skos:prefLabel ?name . \n" +
                "?uri skos:definition ?definition .\n" +
                taxaGraph +
                "}\n"
        return queryString
    }
}