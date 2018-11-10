package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGDatasetSource
import eu.biogateway.cytoscape.internal.model.BGQueryConstraint
import eu.biogateway.cytoscape.internal.model.BGRelation
import eu.biogateway.cytoscape.internal.model.BGRelationType
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import eu.biogateway.cytoscape.internal.util.Utility


class BGFindRelationForNodeQuery(val relationType: BGRelationType, val nodeUri: String, val direction: BGRelationDirection): BGQuery(BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {

       return when (direction) {
            BGRelationDirection.TO -> generateToQueryString()
            BGRelationDirection.FROM -> generateFromQueryString()
        }
    }

    val graphName: String get() {
        relationType.defaultGraphURI?.let {
            if (it.isNotEmpty()) return "<"+it+">"
        }
        return "?graph"
    }

    var returnDataFilter: ((BGRelation) -> Boolean)? = null


    init {
        parsingBlock = {
            val returnRelationsData = parser.parseRelations(it, type, taskMonitor)
                val filter = returnDataFilter
                if (filter != null) {
                    returnRelationsData.relationsData = ArrayList(returnRelationsData.relationsData.filter(filter))
                    returnRelationsData.unloadedNodes?.let {
                        returnRelationsData.unloadedNodes = Utility.removeNodesNotInRelationSet(it, returnRelationsData.relationsData).toList()
                    }
                }
            returnData = returnRelationsData
            runCompletions()
        }
    }

    fun generateFromQueryString(): String {

        val triple = Triple("<$nodeUri>", relationType, "?toNode")
        val constraintFilter = BGQueryConstraint.generateConstraintQueries(arrayListOf(triple))

        val sourceFilter = BGDatasetSource.generateSourceConstraint(relationType, "<"+nodeUri+">", "?toNode") ?: Pair("", "")
        return "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT <$nodeUri> $graphName <${relationType.uri}> ?toNode\n" +
                "WHERE {\n" +
                sourceFilter.first + "\n" +
                "GRAPH "+graphName+" {\n" +
                "<$nodeUri> ${relationType.sparqlIRI} ?toNode .\n" +
                sourceFilter.second + "\n" +
                "}" +
                constraintFilter + "}"
    }

    fun generateToQueryString(): String {
        val triple = Triple("?fromNode", relationType, "<$nodeUri>")
        val constraintFilter = BGQueryConstraint.generateConstraintQueries(arrayListOf(triple))

        val sourceFilter = BGDatasetSource.generateSourceConstraint(relationType, "?fromNode",  "<"+nodeUri+">") ?: Pair("", "")
        return "BASE <http://rdf.biogateway.eu/graph/>\n" +
                "SELECT DISTINCT ?fromNode $graphName <${relationType.uri}> <$nodeUri> \n" +
                "WHERE {\n" +
                sourceFilter.first + "\n" +
                "GRAPH "+graphName+" {\n" +
                "?fromNode ${relationType.sparqlIRI} <$nodeUri> .\n" +
                sourceFilter.second + "\n" +
                "}" +
                constraintFilter + "}"
    }
}

