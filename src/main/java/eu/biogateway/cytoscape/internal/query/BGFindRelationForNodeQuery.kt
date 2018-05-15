package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGDatasetSource
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
        val sourceFilter = BGDatasetSource.generateSourceConstraint(relationType, "<"+nodeUri+">", "?toNode") ?: Pair("", "")
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX relation1: <" + relationType.uri + ">\n" +
                "PREFIX fromNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT fromNode: "+graphName+" relation1: ?toNode\n" +
                "WHERE {\n" +
                sourceFilter.first + "\n" +
                "GRAPH "+graphName+" {\n" +
                "fromNode: "+relationType.sparqlIRI+" ?toNode .\n" +
                sourceFilter.second + "\n" +
                "}}"

    }

    fun generateToQueryString(): String {
        val sourceFilter = BGDatasetSource.generateSourceConstraint(relationType, "?fromNode",  "<"+nodeUri+">") ?: Pair("", "")
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX relation1: <" + relationType.uri + ">\n" +
                "PREFIX toNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT ?fromNode "+graphName+" relation1: toNode:\n" +
                "WHERE {\n" +
                sourceFilter.first + "\n" +
                "GRAPH "+graphName+" {\n" +
                "?fromNode "+relationType.sparqlIRI+" toNode: .\n" +
                sourceFilter.second + "\n" +
                "}}"
    }
}

