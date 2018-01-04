package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.io.BufferedReader
import java.io.StringReader


class BGFindGraphRelationForNodeQuery(serviceManager: BGServiceManager, val nodeType: BGNodeType, val nodeUri: String): BGRelationQuery(serviceManager, BGReturnType.RELATION_TRIPLE, serviceManager.server.parser) {
    override var queryString: String = ""
        get() = when (nodeType) {
            BGNodeType.Gene -> generateFindProteinsRegluatingGeneQueryString()
            BGNodeType.Protein -> generateFindGenesRegulatedByProteinQueryString()
            else -> {
                throw Exception("TG-TF search is only available for genes and proteins!")
            }
        }

//    override fun run() {
//        taskMonitor?.setTitle("Searching for relations...")
//        val uri = encodeUrl()?.toURI()
//        if (uri != null) {
//            val httpGet = HttpGet(uri)
//            val response = client.execute(httpGet)
//            val statusCode = response.statusLine.statusCode
//            val data = EntityUtils.toString(response.entity)
//            if (statusCode < 200 || statusCode > 399) throw Exception("Server error "+statusCode+": \n"+data)
//            val reader = BufferedReader(StringReader(data))
//            client.close()
//            taskMonitor?.setTitle("Loading results...")
//            parser.parseRelations(reader, type, taskMonitor) {
//                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
//                runCompletions()
//            }
//        }
//    }


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