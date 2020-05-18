package eu.biogateway.app.internal.model

import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.parser.BGReturnType
import eu.biogateway.app.internal.query.*
import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyNetwork
import java.util.concurrent.TimeUnit

class BGConversionQuery(val sparqlQuery: String): BGQuery(BGReturnType.METADATA_FIELD) {
    override fun generateQueryString(): String {
        return "BASE <http://rdf.biogateway.eu/graph/>  \n" +
                "SELECT DISTINCT ?queryReturnData\n" +
                "WHERE {\n" +
                sparqlQuery +
                "}"
    }
}

class BGIdentifierConversion(val nodeType: BGNodeType, type: BGConversionType, sourceNetwork: CyNetwork, sourceColumn: CyColumn, destinationFieldName: String): BGConversion(type, sourceNetwork, sourceColumn, destinationFieldName) {

}
open class BGConversion(val type: BGConversionType, val sourceNetwork: CyNetwork, val sourceColumn: CyColumn, val destinationFieldName: String) {

    fun runForDataString(serviceManager: BGServiceManager, input: String): String? {

        var data = input
        // Check if there is a SPARQL query, in that case, run it first.
        val sparql = type.sparqlTemplate

        if (sparql != null && sparql.isNotEmpty()) {
            val sparqlQuery = sparql.replace("@input", input).replace("@output", "?queryReturnData")
            val query = BGConversionQuery(sparqlQuery)
            query.run()
            val returnData = query.futureReturnData.get(20, TimeUnit.SECONDS) as? BGReturnMetadata ?: throw Exception("Invalid return data.")
            if (returnData.values.isNotEmpty()) {
                // Will only get the first value for the SPARQL result. The query specified in the XML must assure that only one result is returned.
                // TODO: Might add support for "stringarray" here too, in that case it will make a ";"-separated list.
                data = returnData.values.first()
            } else {
                return null
            }
        }
        // Continue with the conversion:

        // Find the template for this conversion:

        val result = when (type.lookupMethod) {
            BGConversionType.LookupMethod.REPLACE -> {
                type.template?.replace("@value", data)
            }
            BGConversionType.LookupMethod.COPY -> data
            BGConversionType.LookupMethod.EXTRACT -> {
                if (type.template != null) {
                    // Will get the parts of the template before and after the "@value" tag, and remove them from the data.
                    val parts = type.template.split("@value")
                    if (parts.size == 2) {
                        var string = data.replace(parts[0], "")
                        string = string.replace(parts[1], "")
                        if (string != data) { // If the result equals the input, the pattern was not found.
                            string
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            BGConversionType.LookupMethod.DICT_EXACT_LOOKUP -> {
                val nodeConversion = type as? BGNodeConversionType ?: return null //  Only valid for node conversions.
                val suggestions = serviceManager.endpoint.searchForLabel(data, nodeConversion.nodeType.id.toLowerCase(), 1)
                if (suggestions.isNotEmpty()) {
                    suggestions.first()._id
                } else {
                    null
                }
            }
        }
        return result
    }

}