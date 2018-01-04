package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.util.Utility
import java.io.BufferedReader
import java.io.StringReader

class BGFindRelationForNodeQuery(serviceManager: BGServiceManager, val relationType: BGRelationType, val nodeUri: String, val direction: BGRelationDirection): BGQuery(serviceManager, BGReturnType.RELATION_TRIPLE, serviceManager.server.parser) {
    override var queryString: String = ""
        get() = when (direction) {
                BGRelationDirection.TO -> generateToQueryString()
                BGRelationDirection.FROM -> generateFromQueryString()
        } //To change initializer of created properties use File | Settings | File Templates.

    var returnDataFilter: ((BGRelation) -> Boolean)? = null


    init {
        parsingBlock = {
            parser.parseRelations(it, type, taskMonitor) {
                var returnRelationsData = it ?: throw Exception("Invalid return data!")
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
    }

    /*
    override fun run() {
        taskMonitor?.setTitle("Searching for relations...")
        val uri = encodeUrl()?.toURI()
        if (uri != null) {
            val httpGet = HttpGet(uri)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            val data = EntityUtils.toString(response.entity)
            if (statusCode < 200 || statusCode > 399) throw Exception("Server error "+statusCode+": \n"+data)
            val reader = BufferedReader(StringReader(data))
            client.close()
            taskMonitor?.setTitle("Loading results...")
            parser.parseRelations(reader, type, taskMonitor) {
//                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
//                runCompletions()
                var returnRelationsData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
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
    }
    */

    val graphName: String get() {
        relationType.defaultGraphName?.let {
            if (it.isNotEmpty()) return "<"+it+">"
        }
        return "?graph"
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

