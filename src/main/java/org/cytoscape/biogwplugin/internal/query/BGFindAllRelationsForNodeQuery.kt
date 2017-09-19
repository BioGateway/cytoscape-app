package org.cytoscape.biogwplugin.internal.query

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader
import java.io.StringReader

class BGFindAllRelationsForNodeQuery(serviceManager: BGServiceManager, val nodeUri: String, val direction: BGRelationDirection): BGQuery(serviceManager, BGReturnType.RELATION_TRIPLE_NAMED, serviceManager.server.parser) {

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
            parser.parseRelations(reader, type) {
                returnData = it as? BGReturnData ?: throw Exception("Invalid return data!")
                runCompletions()
            }
        }
    }

    override var queryString: String
        get() = when (direction) {
            BGRelationDirection.TO -> generateToQueryString()
            BGRelationDirection.FROM -> generateFromQueryString()
        } //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    private fun generateFromQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX fromNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT fromNode: ?fromNodeName ?relation ?toNode ?toNodeName\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "fromNode: ?relation ?toNode .\n" +
                "?toNode skos:prefLabel|skos:altLabel ?toNodeName .\n" +
                "fromNode: skos:prefLabel|skos:altLabel ?fromNodeName .\n" +
                "}}"
    }

    private fun generateToQueryString(): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX toNode: <" + nodeUri + ">\n" +
                "SELECT DISTINCT ?fromNode ?fromNodeName ?relation toNode: ?toNodeName\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "?fromNode ?relation toNode: .\n" +
                "toNode: skos:prefLabel|skos:altLabel ?toNodeName .\n" +
                "?fromNode skos:prefLabel|skos:altLabel ?fromNodeName .\n" +
                "}}"
    }
}