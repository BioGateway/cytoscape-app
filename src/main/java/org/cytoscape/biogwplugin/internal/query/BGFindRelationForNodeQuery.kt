package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.util.Utility


class BGFindRelationForNodeQuery(serviceManager: BGServiceManager, val relationType: BGRelationType, val nodeUri: String, val direction: BGRelationDirection): BGQuery(serviceManager, BGReturnType.RELATION_TRIPLE_GRAPHURI) {

    override fun generateQueryString(): String {
       return when (direction) {
            BGRelationDirection.TO -> generateToQueryString()
            BGRelationDirection.FROM -> generateFromQueryString()
        }
    }

    val graphName: String get() {
        relationType.defaultGraphName?.let {
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
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX relation1: <" + relationType.uri + ">\n" +
                "PREFIX fromNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT fromNode: "+graphName+" relation1: ?toNode\n" +
                "WHERE {\n" +
                "GRAPH "+graphName+" {\n" +
                "fromNode: "+relationType.sparqlIRI+" ?toNode .\n" +
                "}}"

    }

    fun generateToQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX relation1: <" + relationType.uri + ">\n" +
                "PREFIX toNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT ?fromNode "+graphName+" relation1: toNode:\n" +
                "WHERE {\n" +
                "GRAPH "+graphName+" {\n" +
                "?fromNode "+relationType.sparqlIRI+" toNode: .\n" +
                "}}"
    }
}

