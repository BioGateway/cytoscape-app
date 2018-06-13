package eu.biogateway.cytoscape.internal.gui.cmfs

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * A ContextMenuFactory which creates context menus for changing the type of a CyEdge.
 *
 * @param gravity The position of the menu where it is added.
 * @param serviceManager The BGServiceManager object holding references to services needed.
 * @constructor Creates a new CyEdgeViewContextMenuFactory which can create CyMenuItems for right-clicked edges.
 *
 */
class BGChangeEdgeTypeCMF(val gravity: Float): CyEdgeViewContextMenuFactory {

    /**
     * Creates a CyMenuItem with actions which allows the user to change the relation type and URI of an edge.
     *
     * @param netView The CyNetworkView that the edge belongs to.
     * @param edgeView The view for the edge that was selected.
     *
     * @return The CyMenuItem with the actions.
     *
     */
    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {
        val parentMenu = JMenu("Change to...")
        val edgeSuid = edgeView?.model?.suid
        val edgeTable = netView?.model?.defaultEdgeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")

        for (key in BGServiceManager.config.relationTypeMap.keys) {
            val relationType = BGServiceManager.config.relationTypeMap.get(key)
            if (relationType != null) {
                val item = JMenuItem(relationType.name)
                item.addActionListener {
                    if (key == edgeUri) {
                        return@addActionListener
                    }
                    // Change the relation in the edge table:
                    val edgeId = edgeTable.getRow(edgeSuid)?.get(Constants.BG_FIELD_EDGE_ID, String::class.java)
                    if (edgeId != null) {
                        edgeTable.getRow(edgeSuid)?.set(Constants.BG_FIELD_EDGE_ID,  edgeId.replace(edgeUri, relationType.uri))
                    }

                    edgeTable.getRow(edgeSuid)?.set(Constants.BG_FIELD_NAME, relationType.name)
                    edgeTable.getRow(edgeSuid)?.set("shared name", relationType.name)
                    edgeTable.getRow(edgeSuid)?.set(Constants.BG_FIELD_IDENTIFIER_URI, relationType.uri)

                }
                parentMenu.add(item)
            }
        }

        return CyMenuItem(parentMenu, gravity)
    }
}