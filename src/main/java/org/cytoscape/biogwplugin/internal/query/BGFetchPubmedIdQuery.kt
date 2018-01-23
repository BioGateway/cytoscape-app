package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType


enum class BGMetadataType(val uri: String) {
    PUBMED_ID("http://semanticscience.org/resource/SIO_000253"),
    EVIDENCE_CODE("http://www.w3.org/2004/02/skos/core#relatedMatch"),
    CONFIDENCE_VALUE(""),
    FNL_VALUE(""),
    SOURCE("")
}

class BGFetchMetadataQuery(serviceManager: BGServiceManager, val fromNodeUri: String, val relationUri: String, val toNodeUri: String, val graph: String, val metadataType: BGMetadataType): BGQuery(serviceManager, BGReturnType.METADATA_FIELD) {
    override var queryString: String = ""
        get() = generateQueryString(fromNodeUri, relationUri, toNodeUri) //To change initializer of created properties use File | Settings | File Templates.

    init {
        parsingBlock = {
            parser.parsePubmedIdsToTextArray(it, type) {
                taskMonitor?.setTitle("Loading results...")
                returnData = it
                runCompletions()
            }
        }
    }

    fun generateQueryString(fromNodeUri: String, relationUri: String, toNodeUri: String): String {
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

class BGFetchPubmedIdQuery(serviceManager: BGServiceManager, val fromNodeUri: String, val relationUri: String, val toNodeUri: String): BGQuery(serviceManager, BGReturnType.PUBMED_ID) {
    override var queryString: String = ""
        get() = generateQueryString(fromNodeUri, relationUri, toNodeUri) //To change initializer of created properties use File | Settings | File Templates.

    init {
        parsingBlock = {
            parser.parsePubmedIdsToTextArray(it, type) {
                taskMonitor?.setTitle("Loading results...")
                returnData = it
                runCompletions()
            }
        }
    }

    fun generateQueryString(fromNodeUri: String, relationUri: String, toNodeUri: String): String {
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