package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGParser
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.util.Utility
import java.io.BufferedReader
import java.io.StringReader

class BGRelationQueryImplementation(serviceManager: BGServiceManager, override var queryString: String, parser: BGParser, var returnType: BGReturnType): BGRelationQuery(serviceManager, returnType, parser)

abstract class BGRelationQuery(serviceManager: BGServiceManager, type: BGReturnType, parser: BGParser): BGQuery(serviceManager, type, parser) {
    var returnDataFilter: ((BGRelation) -> Boolean)? = null

    init {
        taskMonitorTitle = "Searching for relations..."
        parsingBlock = {
            parser.parseRelations(it, type, taskMonitor) {
                val returnRelationsData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                returnDataFilter?.let {
                    returnRelationsData.relationsData = ArrayList(returnRelationsData.relationsData.filter(it))
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
                val returnRelationsData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                returnDataFilter?.let {
                    returnRelationsData.relationsData = ArrayList(returnRelationsData.relationsData.filter(it))
                    returnRelationsData.unloadedNodes?.let {
                        returnRelationsData.unloadedNodes = Utility.removeNodesNotInRelationSet(it, returnRelationsData.relationsData).toList()
                    }
                }
                returnData = returnRelationsData
                runCompletions()
            }
        }
    }*/
}

