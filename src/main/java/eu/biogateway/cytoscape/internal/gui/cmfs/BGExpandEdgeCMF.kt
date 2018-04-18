package eu.biogateway.cytoscape.internal.gui.cmfs

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.query.BGFindBinaryPPIsBetweenNodesQuery
import eu.biogateway.cytoscape.internal.query.BGLoadUnloadedNodes
import eu.biogateway.cytoscape.internal.query.BGReturnRelationsData
import eu.biogateway.cytoscape.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import javax.swing.JMenuItem


class BGExpandEdgeCMF(val gravity: Float, val serviceManager: BGServiceManager): CyEdgeViewContextMenuFactory {

    val MOLECULARLY_INTERACTS_WITH_URI = "http://purl.obolibrary.org/obo/RO_0002436"

    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {

        if (edgeView == null) throw Exception("Edge view is null!")
        if (netView == null) throw Exception("Network view is null!")

        val edgeSuid = edgeView?.model?.suid  ?: throw Exception("Edge SUID is null!")
        val edgeTable = netView?.model?.defaultEdgeTable
        val nodeTable = netView?.model?.defaultNodeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")
        val fromNodeUri = nodeTable?.getRow(edgeView?.model?.source?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("From node URI not found in CyNetwork!")
        val toNodeUri = nodeTable.getRow(edgeView?.model?.target?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("To node URI not found in CyNetwork!")


        if (edgeUri == MOLECULARLY_INTERACTS_WITH_URI) {
            val item = JMenuItem("Expand")
            item.addActionListener {
                val query = BGFindBinaryPPIsBetweenNodesQuery(fromNodeUri, toNodeUri)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        val network = netView.model
                        if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                        BGLoadUnloadedNodes.createAndRun(returnData.unloadedNodes) {
                            println("Loaded " + it.toString() + " nodes.")

                            val relations = returnData.relationsData
                            val newNodes = relations
                                    .fold(HashSet<BGNode>()) { set, rel ->
                                        set.add(rel.fromNode)
                                        set.add(rel.toNode)
                                        set
                                    }
                                    .filter { it.uri != fromNodeUri }
                                    .filter { it.uri != toNodeUri }
                            serviceManager.dataModelController.networkBuilder.expandEdgeWithRelations(netView, edgeView, newNodes, relations)

                            //BGRelationSearchResultsController(serviceManager, returnData, returnData.columnNames, network)
                        }
                    }
                }
                serviceManager.taskManager?.execute(TaskIterator(query))
            }
            return CyMenuItem(item, gravity)
        }
        return CyMenuItem(null, gravity)
    }
}