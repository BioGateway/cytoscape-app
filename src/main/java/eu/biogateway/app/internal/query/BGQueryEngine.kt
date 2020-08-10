package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.gui.multiquery.BGMultiQueryPanel
import eu.biogateway.app.internal.model.BGRelationType
import eu.biogateway.app.internal.parser.BGReturnType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.BufferedReader
import java.io.StringReader
import java.net.URL
import java.net.URLEncoder

data class BGQueryNode(var uri: String?, var variable: String?) {

    constructor(string: String): this(null, null) {
        if (string.startsWith("?")) {
            this.uri = null
            this.variable = string.replace("?", "")
        } else {
            this.uri = string
            this.variable = null
        }
    }

    val isVariable: Boolean get() {
        return (this.variable != null)
    }
}

data class QueryTriple(var from: BGQueryNode, var relationType: BGRelationType, var to: BGQueryNode) {
    fun generateTSV(): String {
        return "${from.uri}\thttp://rdf.biogateway.eu/graph/${relationType.defaultGraphURI}\t${relationType.uri}\t${to.uri}"
    }
}

class SPARQLQueryRequest(val sparql: String) {

    var client = HttpClients.createDefault()!!


    public suspend fun execute(): String? {
        val uri = encodeUrl(sparql)?.toURI() ?: return null
        val httpRequest = HttpGet(uri)
        val response = client.execute(httpRequest)
        val data = EntityUtils.toString(response.entity)
        return data
    }

    fun encodeUrl(query: String): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"
        val queryURL = URL(BGServiceManager.serverPath + "?query=" + URLEncoder.encode(query, "UTF-8") + "&format=" + RETURN_TYPE_TSV + "&" + BIOPAX_DEFAULT_OPTIONS)
        return queryURL
    }
}

class BGQueryEngine {
    // Input data

    suspend fun runMultiQuery(sparqlQuery: String, components: List<QueryTriple>): BGReturnRelationsData? {
        val query = SPARQLQueryRequest(sparqlQuery)
            val data = query.execute() ?: return null
            val result = parse(data)
            val resultWithContext = insertContext(result, components)
            val reader = BufferedReader(StringReader(resultWithContext))
            val returnData = BGServiceManager.dataModelController.parser
                    .parseRelations(reader, BGReturnType.RELATION_MULTIPART, null)
            return returnData
    }

    fun insertContext(resultRows: List<Map<String, String>>, components: List<QueryTriple>): String {

        var resultString = "header\n"
        for (row in resultRows) {
            var line = ""
            // Replace QueryNodes with values:
            for (triple in components) {
                if (triple.from.variable != null) {
                    val variable = triple.from.variable
                    triple.from.uri = row[variable]
                }
                if (triple.to.variable != null) {
                    val variable = triple.to.variable
                    triple.to.uri = row[variable]
                }
                line += triple.generateTSV() + "\t"
            }
            resultString += line.dropLast(1) + "\n"
        }
        return resultString
    }

    fun parse(data: String): List<Map<String, String>> {
        val resultRows = mutableListOf<Map<String, String>>()
        val reader = BufferedReader(StringReader(data))

        fun parseLine(line: String): Array<String> {
            return line.split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()
        }
        // First line holds the variable names.
        val headers = parseLine(reader.readLine())

        reader.forEachLine { line ->
            val columns = parseLine(line)
            val row = mutableMapOf<String, String>()
            for (i in columns.indices) {
                row[headers[i]] = columns[i]
            }
            resultRows.add(row)
        }
        return resultRows
    }


    fun generateSimplifiedSPARQL(): String {

        /*
        val queryComponents = generateReturnValuesAndGraphQueries()
        var queryWildcards = variableManager.usedVariables.values.toHashSet()
                .sortedBy { it.name }
                .map { "?"+it.value }
                .fold("") { acc, s -> acc+" "+s }
        if (queryWildcards.isEmpty()) {
            queryWildcards = "<placeholder>"
        }
        val query = "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT "+queryWildcards+"\n" +
                "WHERE {\n" +
                queryComponents.second +
                "}"
        return query

         */
        return ""
    }

    private fun generateReturnValuesAndGraphQueries() {
        var returnValues = ""
        var graphQueries = ""

        var triples = HashSet<Triple<String, BGRelationType, String>>()

        var nodeNames = HashSet<String>()

        var numberOfGraphQueries = 0

        /*
        for (line in queryLines) {
            val fromUri = line.fromUri ?: throw Exception("Invalid From URI!")
            //var relationType = line.relationType?.let { relationTypes.get(it) } ?: throw Exception("Invalid Relation Type!")
            val relationType = line.relationType ?: throw Exception("Invalid Relation Type!")
            val toUri = line.toUri ?: throw Exception("Invalid To URI!")
            val fromRDFUri = getRDFURI(fromUri)
            val toRDFUri = getRDFURI(toUri)

            val fromName = "?name_"+getSafeString(fromUri)
            val toName = "?name_"+getSafeString(toUri)

            val graphName = relationType.defaultGraphURI ?: generateGraphName(numberOfGraphQueries, relationType)

            returnValues += fromRDFUri+" as ?"+getSafeString(fromUri)+numberOfGraphQueries+" <"+graphName+"> <"+relationType.uri+"> "+toRDFUri+" as ?"+getSafeString(toUri)+numberOfGraphQueries+" "
            graphQueries += generateSparqlGraph(numberOfGraphQueries, fromRDFUri, relationType, toRDFUri)
            triples.add(Triple(fromRDFUri, relationType, toRDFUri))
            nodeNames.add(fromRDFUri)
            nodeNames.add(toRDFUri)
            numberOfGraphQueries += 1
        }

        val uniqueSetsFilter = if (uniqueSetsCheckBox.isSelected) { generateUniqueSetsFilter() } else { "\n #enableSelfLoops \n" }

        try {
            val constraintValues = constraintPanel.getConstraintValues()
            BGServiceManager.config.taxonConstraint?.let { constraint ->
                BGQueryConstraint.generateTaxonConstraintValue()?.let { value ->
                    constraintValues[constraint] = value
                }
            }
            val constraints = uniqueSetsFilter + BGQueryConstraint.generateConstraintQueries(constraintValues, triples)

            return Triple(returnValues, graphQueries, constraints)
        } catch (exception: InvalidInputValueException) {
            JOptionPane.showMessageDialog(this, exception.message, "Invalid query constraints", JOptionPane.ERROR_MESSAGE)
            return Triple(returnValues, graphQueries, uniqueSetsFilter)
        }
        */
    }
}