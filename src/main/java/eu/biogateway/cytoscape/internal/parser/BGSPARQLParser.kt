package eu.biogateway.cytoscape.internal.parser

import eu.biogateway.cytoscape.internal.model.BGQueryConstraint
import eu.biogateway.cytoscape.internal.model.BGRelationType
import eu.biogateway.cytoscape.internal.util.Utility
import java.util.ArrayList

/**
 * Created by sholmas on 23/05/2017.
 */


class BGQueryOptions() {
    var selfLoopsEnabled = false
}

object BGSPARQLParser {

    enum class BGVariableType {URI, Variable, INVALID}
    class BGGraphParameter(val value: String, val type: BGVariableType)
    class BGGraphConstraint(val id: String, val value: String)
    class BGQueryGraph(val from: BGGraphParameter, val relation: BGGraphParameter, val to: BGGraphParameter, val graph: BGGraphParameter)

    private fun getParameterType(parameter: String): BGVariableType {
        if (parameter.startsWith("?")) return BGVariableType.Variable
        if (parameter.startsWith("<") && parameter.endsWith(">")) return BGVariableType.URI
        return BGVariableType.INVALID
    }

    private fun getParameterValue(variable: String, type: BGVariableType): String? {
        when (type) {
            BGVariableType.URI -> return variable.removePrefix("<").removeSuffix(">")
            BGVariableType.Variable -> return variable.removePrefix("?")
            BGVariableType.INVALID -> return null
        }
    }

    private fun parseParameter(input: String): BGGraphParameter? {
        val type = getParameterType(input)
        val value = getParameterValue(input, type)

        if (type != BGVariableType.INVALID && value != null && value.isNotEmpty()) {
            return BGGraphParameter(value, type)
        }
        return null
    }

    private fun validateRelation(edge: BGGraphParameter, validRelationTypeMap: Map<String, BGRelationType>, graph: String): Boolean {
        // TODO: Add support for relation types as variables.
        // TODO: Also add support for relation types not in the XML file.
        if (edge.type == BGVariableType.Variable) return false

        val identifier = Utility.createRelationTypeIdentifier(edge.value, graph)

        if (validRelationTypeMap.containsKey(identifier)) return true
        if (validRelationTypeMap.containsKey(edge.value)) return true

//        val relationTypeURIs = validRelationTypeMap.values.map { it.uri }
//        if (relationTypeURIs.contains(edge.value)) return true

        println("WARNING: Attempted to parse an edge with an URI not found among the supported edges.")
        return false
    }


    fun parseSPARQLCode(sparqlcode: String, validRelationTypeMap: Map<String, BGRelationType>): Triple<ArrayList<BGQueryGraph>, List<BGGraphConstraint>?, BGQueryOptions> {

        var options = BGQueryOptions()

        options.selfLoopsEnabled = sparqlcode.contains("#enableSelfLoops")

        val query = sparqlcode.split("#QueryConstraints:")

        val mainQuery = query.first()

        val graphs = mainQuery.split("GRAPH")
        var queryGraphs = ArrayList<BGQueryGraph>()

        for (graphString in graphs) {
            if (graphString.startsWith("BASE")) continue // First one, let's just skip it.

            val subGraphs = graphString.split("{")
            if (subGraphs.size < 2) continue // Must be something on both sides of the {, otherwise skip.
            val graphName = subGraphs.get(0).replace(" ", "")
            // If the graph name is not on the form ?graph or <graph>, skip.
            val graphParameter = parseParameter(graphName) ?: continue // If it's invalid, it will be null.
            var graphContents = subGraphs.get(1)

            if (!graphContents.contains("}")) continue // Must be ended with a }.
            graphContents = graphContents.replace("}", "")

            val graphLines = graphContents.split("\n")

            for (line in graphLines) {
                var triple = line.split(" ").filter { it.isNotEmpty() }.filter { it != "." }
                if (triple.size != 3) continue // Should be exactly 3 parts of a triple

                val fromParameter = parseParameter(triple[0]) ?: continue
                val relationParameter = parseParameter(triple[1]) ?: continue
                val toParameter = parseParameter(triple[2]) ?: continue

                if (!validateRelation(relationParameter, validRelationTypeMap, graphName)) continue

                val queryGraph = BGQueryGraph(fromParameter, relationParameter, toParameter, graphParameter)
                queryGraphs.add(queryGraph)
            }
        }

        var constraints: List<BGGraphConstraint>? = null

        if (query.size == 2) {
            val constraintsSparql = query[1]
            constraints = constraintsSparql
                    .split("\n")
                    .filter { it.startsWith("#Constraint: ") }
                    .map { it.replace("#Constraint: ", "") }
                    .map { it.split("=") }
                    .filter { it.size == 2 }
                    .map { BGGraphConstraint(it[0].trim(), it[1].trim()) }
        }
        return Triple(queryGraphs, constraints, options)
    }
}