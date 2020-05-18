package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.parser.BGReturnType

/// Performs a fetch query for the values with the given relation URI for the given node, in the given graph.
class BGFetchAttributeValuesQuery(val nodeUri: String, val relationUri: String, var graphName: String, val direction: BGRelationDirection) : BGQuery(BGReturnType.METADATA_FIELD) {



    override fun generateQueryString(): String {
        if (!graphName.startsWith("?")) graphName = "<"+graphName+">"

        return when (direction) {
            BGRelationDirection.TO -> "BASE <http://rdf.biogateway.eu/graph/> \n" +
                    "SELECT DISTINCT ?value\n" +
                    "WHERE {  \n" +
                    " GRAPH "+graphName+" {\n" +
                    "\t ?instance rdf:type <"+nodeUri+"> .\n" +
                    "\t ?value <"+relationUri+"> ?instance .\n" +
                    "}}"
            BGRelationDirection.FROM -> "BASE <http://rdf.biogateway.eu/graph/> \n" +
                    "SELECT DISTINCT ?value\n" +
                    "WHERE {  \n" +
                    " GRAPH "+graphName+" {\n" +
                    "\t ?instance rdf:type <"+nodeUri+"> .\n" +
                    "\t ?instance <"+relationUri+"> ?value .\n" +
                    "}}"
        }
    }
}