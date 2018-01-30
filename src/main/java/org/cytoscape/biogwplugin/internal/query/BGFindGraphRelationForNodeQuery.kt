package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.StringReader


@Deprecated("Should use BGFindRelationForNodeQuery instead.")
class BGFindGraphRelationForNodeQuery(serviceManager: BGServiceManager, val nodeType: BGNodeType, val nodeUri: String): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE) {

    override fun generateQueryString(): String {
        return when (nodeType) {
            BGNodeType.Gene -> generateFindProteinsRegluatingGeneQueryString()
            BGNodeType.Protein -> generateFindGenesRegulatedByProteinQueryString()
            else -> {
                throw Exception("TG-TF search is only available for genes and proteins!")
            }
        }
    }

    private fun generateFindProteinsRegluatingGeneQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX molecularly_controls: <http://purl.obolibrary.org/obo/RO_0002448>\n" +
                "PREFIX geneUri: <" + nodeUri + ">\n" +
                "SELECT DISTINCT ?proteinUri <tf-tg> molecularly_controls: geneUri: \n" +
                "WHERE {  \n" +
                "GRAPH <tf-tg> {\n" +
                "?proteinUri molecularly_controls: geneUri: .\n" +
                "}}"
    }

    private fun generateFindGenesRegulatedByProteinQueryString(): String {

        return "BASE <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX molecularly_controls: <http://purl.obolibrary.org/obo/RO_0002448>\n" +
                "PREFIX proteinUri: <" + nodeUri + ">\n" +
                "SELECT DISTINCT proteinUri: <tf-tg> molecularly_controls: ?geneUri \n" +
                "WHERE {  \n" +
                "GRAPH <tf-tg> {\n" +
                "proteinUri: molecularly_controls: ?geneUri .\n" +
                "}}"
    }
}