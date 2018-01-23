package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

class BGBulkFetchNodesFromURIs(serviceManager: BGServiceManager, val nodeType: BGNodeType, val nodeUris: Collection<String>): BGQuery(serviceManager, BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {
    override var queryString: String = ""
        get() = generateQueryString()

    init {
        taskMonitorTitle = "Searching for nodes..."
        parseType = BGParsingType.TO_ARRAY
    }

    fun generateQueryString(): String {
        val nodeTypeGraph = when (nodeType) {
            BGNodeType.Gene -> "<refseq>"
            BGNodeType.Protein -> "<refprot>"
            BGNodeType.GO -> "<go-basic>"
            BGNodeType.Taxon -> "<cco>"
            else -> {
                "?anyGraph"
            }
        }

        var uriList = ""
        for (nodeName in nodeUris) {
            uriList += "<"+nodeName+">,"
        }
        uriList = uriList.removeSuffix(",")


        val taxaGraph = when (nodeType) {
            BGNodeType.Gene, BGNodeType.Protein -> "?uri inheres_in: ?taxon . \n } GRAPH taxaGraph: {\n" +
                    "?taxon rdfs:subClassOf sio:SIO_010000 . \n" +
                    "?taxon skos:prefLabel ?taxaName }\n"
            else -> {
                "\n }"
            }
        }
        val taxaName = when (nodeType) {
            BGNodeType.Protein, BGNodeType.Gene -> "?taxaName"
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