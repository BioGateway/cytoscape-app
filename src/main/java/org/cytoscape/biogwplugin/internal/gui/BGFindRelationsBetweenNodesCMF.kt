package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import org.cytoscape.biogwplugin.internal.query.BGQuery
import org.cytoscape.biogwplugin.internal.query.BGRelationsQuery
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyTableUtil
import org.cytoscape.view.model.CyNetworkView
import javax.swing.JMenuItem

class BGFindRelationsBetweenNodesCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNetworkViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?): CyMenuItem {

        val item = JMenuItem("Find relations between selected nodes")

        item.addActionListener {
            val network = netView?.model ?: throw Exception("Network model not found!")
            val selectedNodes = CyTableUtil.getNodesInState(network, "selected", true)

            if (selectedNodes.size < 2) {
                throw Exception("Too few nodes selected. Try selecting two nodes.")
            }

            if (selectedNodes.size > 2) {
                throw Exception("Too many nodes selected. Try selecting two nodes.")
            }

            val firstNode = selectedNodes.get(0)
            val secondNode = selectedNodes.get(1)

            val firstUri = netView.model.defaultNodeTable.getRow(firstNode.suid).get("identifier uri", String::class.java)
            val secondUri = netView.model.defaultNodeTable.getRow(secondNode.suid).get("identifier uri", String::class.java)

            val queryString = generateQueryString(firstUri, secondUri)
            val query = BGRelationsQuery(serviceManager, queryString, serviceManager.server.parser, BGReturnType.RELATION_TRIPLE_NAMED)


        }

        return CyMenuItem(item, gravity)
    }


    private fun generateQueryString(fromNode: String, toNode: String): String {
        return "BASE <http://www.semantic-systems-biology.org/>\n" +
                "PREFIX fromNode: <" + fromNode + ">\n" +
                "PREFIX toNode: <" + toNode + ">\n" +
                "SELECT DISTINCT fromNode: ?fromNodeName ?relation toNode: ?toNodeName\n" +
                "WHERE {\n" +
                "GRAPH ?graph {\n" +
                "fromNode: ?relation ?toNode .\n" +
                "toNode: skos:prefLabel|skos:altLabel ?toNodeName .\n" +
                "fromNode: skos:prefLabel|skos:altLabel ?fromNodeName .\n" +
                "}}"
    }

}