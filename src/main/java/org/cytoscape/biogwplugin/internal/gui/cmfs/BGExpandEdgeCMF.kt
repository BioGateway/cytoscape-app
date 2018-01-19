package org.cytoscape.biogwplugin.internal.gui.cmfs

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.gui.BGRelationSearchResultsController
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.parser.getExpandable
import org.cytoscape.biogwplugin.internal.query.BGFindDiscretePPIsBetweenNodesQuery
import org.cytoscape.biogwplugin.internal.query.BGLoadUnloadedNodes
import org.cytoscape.biogwplugin.internal.query.BGReturnRelationsData
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyEdge
import org.cytoscape.task.EdgeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import javax.swing.JMenuItem



class BGExpandEdgeDoubleClickEVTF(val serviceManager: BGServiceManager): EdgeViewTaskFactory {
    override fun createTaskIterator(edgeView: View<CyEdge>?, netView: CyNetworkView?): TaskIterator {
        if (edgeView == null) throw Exception("Edge view is null!")
        if (netView == null) throw Exception("Network view is null!")

        val edgeSuid = edgeView?.model?.suid  ?: throw Exception("Edge SUID is null!")
        val edgeTable = netView?.model?.defaultEdgeTable
        val nodeTable = netView?.model?.defaultNodeTable
        val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")
        val fromNodeUri = nodeTable?.getRow(edgeView?.model?.source?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("From node URI not found in CyNetwork!")
        val toNodeUri = nodeTable.getRow(edgeView?.model?.target?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("To node URI not found in CyNetwork!")

        val query = BGFindDiscretePPIsBetweenNodesQuery(serviceManager, fromNodeUri, toNodeUri)
        query.addCompletion {
            val returnData = it as? BGReturnRelationsData
            if (returnData != null) {
                val network = netView.model
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
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
                    serviceManager.server.networkBuilder.expandEdgeWithRelations(netView, edgeView, newNodes, relations)
                }
            }
        }
        return TaskIterator(query)
    }

    override fun isReady(edgeView: View<CyEdge>?, networkView: CyNetworkView?): Boolean {
        // Check if edge can be expanded.
        if (edgeView == null || networkView == null) return false

        val expandable = edgeView.model.getExpandable(networkView.model)


        return expandable
    }

}

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
                val query = BGFindDiscretePPIsBetweenNodesQuery(serviceManager, fromNodeUri, toNodeUri)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        val network = netView.model
                        if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                        BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
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
                            serviceManager.server.networkBuilder.expandEdgeWithRelations(netView, edgeView, newNodes, relations)

                            //BGRelationSearchResultsController(serviceManager, returnData, returnData.columnNames, network)
                        }
                    }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            }
            return CyMenuItem(item, gravity)
        }
        return CyMenuItem(null, gravity)
    }
}