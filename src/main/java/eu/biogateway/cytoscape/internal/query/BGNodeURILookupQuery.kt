package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.parser.BGReturnType

class BGNodeURILookupQuery(serviceManager: BGServiceManager, val searchString: String, val useRegex: Boolean, val nodeType: BGNodeType): BGQuery(serviceManager, BGReturnType.NODE_LIST_DESCRIPTION_TAXON) {

    init {
        taskMonitorTitle = "Searching for nodes..."
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
        val filter = when (useRegex) {
            true -> "FILTER regex ( ?name, '"+searchString+"','i' ) .\n"
            false -> "FILTER ( ?name = '"+searchString+"') .\n"
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

        if (nodeType == BGNodeType.Taxon) {
            val queryString = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
                    "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                    "PREFIX sio:  <http://semanticscience.org/resource/>  \n" +
                    "PREFIX graph: <cco>  \n" +
                    "SELECT DISTINCT ?uri ?name ?definition "+taxaName+"\n"+
                    "WHERE {  \n" +
                    " FILTER regex ( ?name, '"+searchString+"', 'i') .\n" +
                    " GRAPH graph: {  \n" +
                    " ?uri rdfs:subClassOf sio:SIO_010000 .  \n" +
                    " ?uri skos:prefLabel ?name .  \n" +
                    " ?uri skos:definition ?definition . \n" +
                    " }  \n" +
                    "}  \n" +
                    "ORDER BY ?name"
            return queryString
        }

        val queryString = "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX taxaGraph: <cco>\n" +
                "PREFIX inheres_in: <http://purl.obolibrary.org/obo/RO_0000052>\n" +
                "PREFIX sio:  <http://semanticscience.org/resource/>  \n" +
                "SELECT DISTINCT ?uri ?name ?definition "+taxaName+"\n" +
                "WHERE {  \n" +
                filter +
                "GRAPH "+nodeTypeGraph+" { \n" +
                "?uri skos:prefLabel|skos:altLabel ?name . \n" +
                "?uri skos:definition ?definition .\n" +
                taxaGraph +
                "}\n"
        return queryString
    }
}
