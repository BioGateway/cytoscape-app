package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.query.BGReturnMetadata
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class BGNodeMetadataType(val id: String, val label: String, val dataType: BGTableDataType, val nodeType: BGNodeTypeNew, val template: String? = null, val sparql: String? = null, val restGet: String? = null, val jsonField: String? = null) {

    // First, the SPARQL query is being run, if present.


    fun runQueries(input: String): BGNodeMetadata? {
        val sparqlResult = runSparql(input)
        val restResult = runRESTQuery(sparqlResult)

        if (restResult == null) return null
        // TODO: Add the results to a column.
        print(restResult)
        val metadata = BGNodeMetadata(dataType, restResult, id)
        return metadata
    }


    fun runSparql(input: String): String {
        if (sparql == null || sparql.isEmpty()) return input
        val sparqlQuery = sparql.replace("@input", input).replace("@output", "?queryReturnData")
        val query = BGConversionQuery(sparqlQuery)
        query.run()
        val returnData = query.futureReturnData.get(20, TimeUnit.SECONDS) as? BGReturnMetadata ?: throw Exception("Invalid return data.")
        if (returnData.values.isEmpty()) {
            return ""
        }
        val data = returnData.values.first()

        return data
    }

    // Second, the results from the SPARQL is being used for the REST query. If no result, the input data is used.

    fun runRESTQuery(input: String): Any? {
        if (restGet == null || restGet.isEmpty() || jsonField == null || jsonField.isEmpty()) return runTemplate(input)

        val encodedInput = URLEncoder.encode(input, "UTF-8")
        val path = restGet.replace("@input", encodedInput)
        val json = BGServiceManager.endpoint.getSuggestionsForQueryPath(path)
        val field = json?.get(jsonField) ?: throw Exception("Metadata not found!")

        val result: Any? = when (dataType) {
            BGTableDataType.STRING -> {
                runTemplate(field.asString)
            }
            BGTableDataType.DOUBLE -> field.asDouble
            BGTableDataType.INT -> field.asInt
            BGTableDataType.BOOLEAN -> field.asBoolean
            BGTableDataType.STRINGARRAY -> {
                val values = field.asJsonArray.map { it.asString }
                runTemplateOnList(values)
            }
            BGTableDataType.INTARRAY -> field.asJsonArray.map { it.asInt }
            BGTableDataType.DOUBLEARRAY -> field.asJsonArray.map { it.asDouble }
            BGTableDataType.UNSUPPORTED -> throw Exception("Unsupported metadata!")
        }
        return result
    }

    fun runTemplateOnList(input: List<String>): List<String> {
        return input.map { runTemplate(it) }
    }

    fun runTemplate(input: String): String {
        if (template == null || template.isEmpty()) {
            return input
        }
        // Will get the parts of the template before and after the "@value" tag, and remove them from the data.
        val parts = template.split("@value")
        if (parts.size == 2) {
            var string = input.replace(parts[0], "")
            string = string.replace(parts[1], "")
            if (string != input) { // If the result equals the input, the pattern was not found.
                return string
            }
        }

        return input
    }

}


/*
class BGRelationMetadataType(val id: String, val name: String, val dataType: BGTableDataType, val relationUri: String, val supportedRelations: Collection<BGRelationType>, val sparql: String? = null, val conversions: Map<String, String>? = null) {
    var scalingFactor: Double = 1.0
    var enabledByDefault = false
}
 */