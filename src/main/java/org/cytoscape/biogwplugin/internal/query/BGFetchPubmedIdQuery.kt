package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

@Deprecated("Use BGFetchMetadataQuery instead!")
class BGFetchPubmedIdQuery(serviceManager: BGServiceManager, val fromNodeUri: String, val relationUri: String, val toNodeUri: String): BGQuery(serviceManager, BGReturnType.PUBMED_ID) {

    init {
        // TODO: Fix block!
        parsingBlock = {
//            returnData = parser.parsePubmedIdsToTextArray(it, type)
//                taskMonitor?.setTitle("Loading results...")
//                runCompletions()
        }
    }

    override fun generateQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX has_source: <http://semanticscience.org/resource/SIO_000253> \n" +
                "PREFIX relatedMatch: <http://www.w3.org/2004/02/skos/core#relatedMatch> \n" +
                "SELECT DISTINCT ?pubmedId\n" +
                "WHERE {\n" +
                "GRAPH ?graph {  \n" +
                "?triple rdf:subject <"+fromNodeUri+"> .  \n" +
                "?triple rdf:predicate <"+relationUri+"> .  \n" +
                "?triple rdf:object <"+toNodeUri+"> . \n" +
                "?triple has_source:|relatedMatch: ?pubmedId .\n" +
                "}}"
    }
}