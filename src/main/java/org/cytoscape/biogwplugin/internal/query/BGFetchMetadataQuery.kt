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

class BGFetchMetadataQuery(serviceManager: BGServiceManager, val fromNodeUri: String, val relationUri: String, val toNodeUri: String, val graph: String, val metadataType: BGMetadataType): BGCallableQuery(serviceManager, BGReturnType.METADATA_FIELD) {


    override fun createQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>  \n" +
                "SELECT DISTINCT ?metadata\n" +
                "WHERE {\n" +
                "GRAPH ?graph {  \n" +
                "?triple rdf:subject <"+fromNodeUri+"> .  \n" +
                "?triple rdf:predicate <"+relationUri+"> .  \n" +
                "?triple rdf:object <"+toNodeUri+"> . \n" +
                "?triple <"+metadataType.uri+"> ?metadata .\n" +
                "}}"}

    init {
        parseFunction = parser::parseMetadata
    }
}