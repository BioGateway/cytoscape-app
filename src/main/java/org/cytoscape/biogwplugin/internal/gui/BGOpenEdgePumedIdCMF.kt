package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.query.BGFetchPubmedIdQuery
import org.cytoscape.biogwplugin.internal.query.BGReturnPubmedIds
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.Desktop
import java.net.URI
import javax.swing.JMenuItem

class BGOpenEdgePumedIdCMF(val gravity: Float, val serviceManager: BGServiceManager): CyEdgeViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {
        val edgeSuid = edgeView?.model?.suid
        val edgeTable = netView?.model?.defaultEdgeTable
        val nodeTable = netView?.model?.defaultNodeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get("identifier uri", String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")
        val fromNodeUri = nodeTable?.getRow(edgeView?.model?.source?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("From node URI not found in CyNetwork!")
        val toNodeUri = nodeTable?.getRow(edgeView?.model?.target?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("To node URI not found in CyNetwork!")

        val item = JMenuItem("Open PubMed Source.")

        item.addActionListener {
            var pubmedId = edgeTable.getRow(edgeSuid)?.get("pubmed uri", String::class.java)

            if (pubmedId != null) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(pubmedId));
                }
            } else {
                val query = BGFetchPubmedIdQuery(serviceManager, fromNodeUri, edgeUri, toNodeUri)
                query.addCompletion {
                    val data = it as? BGReturnPubmedIds ?: throw Exception("Invalid return data!")
                    if (data.pubmedIDlist.size == 0) {
                        throw Exception("No results found.")
                    }
                    pubmedId = data.pubmedIDlist[0]
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(pubmedId));
                    }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            }
        }
        return CyMenuItem(item, gravity)
    }
}