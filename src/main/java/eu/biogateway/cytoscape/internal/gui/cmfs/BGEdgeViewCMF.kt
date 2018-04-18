package eu.biogateway.cytoscape.internal.gui.cmfs

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.query.*
import eu.biogateway.cytoscape.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import javax.swing.JMenu
import javax.swing.JMenuItem


class BGEdgeViewCMF(val gravity: Float, val serviceManager: BGServiceManager): CyEdgeViewContextMenuFactory {
    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {

        val edgeSuid = edgeView?.model?.suid
        val edgeTable = netView?.model?.defaultEdgeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")

        return CyMenuItem(createChangeEdgeTypeMenu(netView, edgeView), gravity)
    }

    @Deprecated("This should not be allowed anymore.")
    fun createChangeEdgeTypeMenu(netView: CyNetworkView?, edgeView: View<CyEdge>?): JMenuItem {
        val parentMenu = JMenu("Change to...")
        val edgeSuid = edgeView?.model?.suid
        val edgeTable = netView?.model?.defaultEdgeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")

        for (key in serviceManager.cache.relationTypeMap.keys) {
            val relationType = serviceManager.cache.relationTypeMap.get(key)
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

        return parentMenu
    }

    @Deprecated("Should not be used.")
    fun openEdgeSourceViewMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): JMenuItem {
        val edgeSuid = edgeView?.model?.suid
        val edgeTable = netView?.model?.defaultEdgeTable
        val nodeTable = netView?.model?.defaultNodeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")
        val fromNodeUri = nodeTable?.getRow(edgeView?.model?.source?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("From node URI not found in CyNetwork!")
        val toNodeUri = nodeTable.getRow(edgeView?.model?.target?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("To node URI not found in CyNetwork!")
        val sourceGraph = edgeTable.getRow(edgeSuid)?.get(Constants.BG_FIELD_SOURCE_GRAPH, String::class.java)
        val item = JMenuItem("View source data")

        item.addActionListener {
            /*
            val query = BGFetchPubmedIdQuery(serviceManager, fromNodeUri, edgeUri, toNodeUri)
            query.addCompletion {
                val metadata = BGRelationMetadata(edgeUri)
                metadata.sourceGraph = sourceGraph
                val data = it as? BGReturnPubmedIds ?: throw Exception("Invalid return data!")
                metadata.pubmedUris.addAll(data.pubmedIDlist)
                EventQueue.invokeLater {
                    BGRelationSourceController(metadata)
                }
            }
            */
            val query = BGFetchMetadataQuery(fromNodeUri, edgeUri, toNodeUri, sourceGraph ?: "?graph", BGMetadataTypeEnum.PUBMED_ID.uri)
            serviceManager.taskManager?.execute(TaskIterator(query))
            Thread(Runnable {
                val metadata = query.returnFuture.get() as BGReturnMetadata
                // TODO: Finish this if the method gets un-deprecated

            }).start()
        }
        return item
    }

}