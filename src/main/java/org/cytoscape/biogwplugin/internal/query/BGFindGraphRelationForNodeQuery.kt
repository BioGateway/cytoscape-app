package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.StringReader

class BGFindGraphRelationForNodeQuery(serviceManager: BGServiceManager, val nodeType: BGNodeType, val nodeUri: String): BGQuery(serviceManager, BGReturnType.RELATION_TRIPLE_PUBMED, serviceManager.server.parser) {
    override var queryString: String
        get() = when (nodeType) {
            BGNodeType.Gene -> generateFindProteinsRegluatingGeneQueryString()
            BGNodeType.Protein -> generateFindGenesRegulatedByProteinQueryString()
            else -> {
                throw Exception("TG-TF search is only available for genes and proteins!")
            }
        }
        set(value) {}

    override fun run() {
        taskMonitor?.setTitle("Searching for relations...")
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            val data = EntityUtils.toString(response.entity)
            if (statusCode < 200 || statusCode > 399) throw Exception("Server error "+statusCode+": \n"+data)
            val reader = BufferedReader(StringReader(data))
            client.close()
            taskMonitor?.setTitle("Loading results...")
            parser.parseRelations(reader, type) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
    }


fun generateFindProteinsRegluatingGeneQueryString(): String {
    return "BASE <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "PREFIX inheres_in: <http://purl.obolibrary.org/obo/RO_0000052>  \n" +
            "PREFIX molecularly_controls: <http://purl.obolibrary.org/obo/RO_0002448>\n" +
            "PREFIX regulates: <http://purl.obolibrary.org/obo/RO_0002211>\n" +
            "PREFIX source: <http://semanticscience.org/resource/SIO_000253> \n" +
            "PREFIX geneUri: <" + nodeUri + ">\n" +
            "PREFIX graph1: <refseq>  \n" +
            "PREFIX graph2: <refprot> \n" +
            "PREFIX graph3: <tf-tg> \n" +
            "\n" +
            "PREFIX taxon: <http://purl.obolibrary.org/obo/NCBITaxon_9606>  \n" +
            "SELECT DISTINCT ?proteinUri ?protein molecularly_controls: geneUri: ?gene ?pubmedUri\n" +
            "WHERE {  \n" +
            "GRAPH graph3: {  \n" +
            "?triple rdf:subject ?proteinUri .  \n" +
            "?triple rdf:predicate molecularly_controls: .  \n" +
            "?triple rdf:object geneUri: .  \n" +
            "?triple source: ?pubmedUri . \n" +
            "  }  \n" +
            "GRAPH graph2: {  \n" +
            "?proteinUri skos:prefLabel ?protein .  \n" +
            "?proteinUri inheres_in: taxon: . \n" +
            "  }  \n" +
            "GRAPH graph1: {  \n" +
            "geneUri: skos:prefLabel ?gene .  \n" +
            "geneUri: inheres_in: taxon: .  \n" +
            "  }  \n" +
            "}"
}

    fun generateFindGenesRegulatedByProteinQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX inheres_in: <http://purl.obolibrary.org/obo/RO_0000052>  \n" +
                "PREFIX molecularly_controls: <http://purl.obolibrary.org/obo/RO_0002448>\n" +
                "PREFIX regulates: <http://purl.obolibrary.org/obo/RO_0002211>\n" +
                "PREFIX source: <http://semanticscience.org/resource/SIO_000253> \n" +
                "PREFIX proteinUri: <" + nodeUri + ">\n" +
                "PREFIX graph1: <refseq>  \n" +
                "PREFIX graph2: <refprot> \n" +
                "PREFIX graph3: <tf-tg> \n" +
                "\n" +
                "PREFIX taxon: <http://purl.obolibrary.org/obo/NCBITaxon_9606>  \n" +
                "SELECT DISTINCT proteinUri: ?protein molecularly_controls: ?geneUri ?gene ?pubmedUri\n" +
                "WHERE {  \n" +
                "GRAPH graph3: {  \n" +
                "?triple rdf:subject proteinUri: .  \n" +
                "?triple rdf:predicate molecularly_controls: .  \n" +
                "?triple rdf:object ?geneUri .  \n" +
                "?triple source: ?pubmedUri . \n" +
                "  }  \n" +
                "GRAPH graph2: {  \n" +
                "proteinUri: skos:prefLabel ?protein .  \n" +
                "proteinUri: inheres_in: taxon: . \n" +
                "  }  \n" +
                "GRAPH graph1: {  \n" +
                "?geneUri skos:prefLabel ?gene .  \n" +
                "?geneUri inheres_in: taxon: .  \n" +
                "  }  \n" +
                "}"
    }
}