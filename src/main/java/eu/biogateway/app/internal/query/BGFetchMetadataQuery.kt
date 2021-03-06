package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.parser.BGReturnType


enum class BGMetadataTypeEnum(val uri: String) {
    PUBMED_ID("<http://semanticscience.org/resource/SIO_000253>"),
    EVIDENCE_CODE("<http://www.w3.org/2004/02/skos/core#relatedMatch>"),
    CONFIDENCE_VALUE("rdfs:label"),
    FNL_VALUE(""),
    SOURCE("<http://semanticscience.org/resource/SIO_000253>")
}

class BGFetchMetadataQuery(val fromNodeUri: String, val relationUri: String, val toNodeUri: String, val graph: String, val metadataRelationUri: String, val sparql: String? = null): BGCallableQuery(BGReturnType.METADATA_FIELD) {


    override fun createQueryString(): String {

        if (sparql != null && sparql.isNotEmpty()) {
            val sparqlQuery = sparql
                    .replace("@fromUri", fromNodeUri)
                    .replace("@toUri", toNodeUri)
                    .replace("@relationUri", relationUri) // The URI of the relation that this metadata belongs to.
                    .replace("@metadataRelationUri", metadataRelationUri) // The URI for fetching the metadata itself.
                    .replace("@graph", graph)
            return "BASE <http://rdf.biogateway.eu/graph/>  \n" +
                    "SELECT DISTINCT ?metadata\n" +
                    sparqlQuery
        }

        return "BASE <http://rdf.biogateway.eu/graph/>  \n" +
                "SELECT DISTINCT ?metadata\n" +
                "WHERE {\n" +
                "GRAPH $graph {  \n" +
                "?triple rdf:subject <"+fromNodeUri+"> .  \n" +
                "?triple rdf:predicate <"+relationUri+"> .  \n" +
                "?triple rdf:object <"+toNodeUri+"> . \n" +
                "?triple "+metadataRelationUri+" ?metadata .\n" +
                "}}"}

    init {
        parseFunction = parser::parseMetadata
    }
}