package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGNodeTypeNew
import eu.biogateway.cytoscape.internal.parser.BGReturnType

class BGBulkImportNodesFromURIs(val nodeType: BGNodeTypeNew, val nodeUris: Collection<String>): BGQuery(BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {

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

        val queryString = "BASE <http://www.semantic-systems-biology.org/>\n" +
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