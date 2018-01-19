package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.util.Utility
import java.util.ArrayList

/**
 * Created by sholmas on 23/05/2017.
 */


object BGSPARQLParser {

    enum class BGVariableType {URI, Variable, INVALID}
    class BGGraphParameter(val value: String, val type: BGVariableType)
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


    fun parseSPARQLCode(sparqlcode: String, validRelationTypeMap: Map<String, BGRelationType>): ArrayList<BGQueryGraph> {

        val graphs = sparqlcode.split("GRAPH")
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
        return queryGraphs
    }
}