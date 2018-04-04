package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.parser.BGReturnType


class BGBulkImportNodesQuery(serviceManager: BGServiceManager, val nodeList: Collection<String>, val nodeType: BGNodeType): BGQuery(serviceManager, BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {

    init {
        parseType = BGParsingType.TO_ARRAY
    }

    override fun generateQueryString(): String {
        val nodeTypeGraph = when (nodeType) {
            BGNodeType.Gene -> "<refseq>"
            BGNodeType.Protein -> "<refprot>"
            BGNodeType.GO -> "<go-basic>"
            BGNodeType.Taxon -> "<cco>"
            else -> {
                "?anyGraph"
            }
        }
        fun  getFilter(): String {
            var namesString = ""
            for (nodeName in nodeList) {
                namesString += "?name = '"+nodeName+"' || "
            }
            namesString = namesString.removeSuffix(" || ") // Remove the last OR operator.
            return "FILTER ("+namesString+") .\n"
        }

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
                getFilter() +
                "GRAPH "+nodeTypeGraph+" { \n" +
                "?uri skos:prefLabel ?name . \n" +
                "?uri skos:definition ?definition .\n" +
                taxaGraph +
                "}\n"
        return queryString
    }
}