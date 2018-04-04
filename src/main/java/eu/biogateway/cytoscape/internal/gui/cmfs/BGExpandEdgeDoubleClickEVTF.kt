package eu.biogateway.cytoscape.internal.gui.cmfs

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import eu.biogateway.cytoscape.internal.parser.getSourceGraph
import eu.biogateway.cytoscape.internal.parser.getUri
import eu.biogateway.cytoscape.internal.query.BGExpandRelationToNodesQuery
import eu.biogateway.cytoscape.internal.query.BGLoadUnloadedNodes
import eu.biogateway.cytoscape.internal.query.BGReturnRelationsData
import org.cytoscape.model.CyEdge
import org.cytoscape.task.EdgeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator

class BGExpandEdgeDoubleClickEVTF(val serviceManager: BGServiceManager): EdgeViewTaskFactory {
    override fun createTaskIterator(edgeView: View<CyEdge>?, netView: CyNetworkView?): TaskIterator {
        if (edgeView == null) throw Exception("Edge view is null!")
        if (netView == null) throw Exception("Network view is null!")

        val network = netView.model
        val edge = edgeView?.model ?: throw  Exception("CyEdge is null!")

        //val edgeSuid = edgeView?.model?.suid  ?: throw Exception("Edge SUID is null!")
        //val edgeTable = netView?.model?.defaultEdgeTable
        //val nodeTable = netView?.model?.defaultNodeTable
        //val edgeUri = edgeTable?.getRow(edgeSuid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Edge URI not found in CyNetwork")
        //val fromNodeUri = nodeTable?.getRow(edgeView?.model?.source?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("From node URI not found in CyNetwork!")
        //val toNodeUri = nodeTable.getRow(edgeView?.model?.target?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("To node URI not found in CyNetwork!")
        val fromNodeUri = edge.source.getUri(network)
        val toNodeUri = edge.target.getUri(network)
        val edgeUri = edge.getUri(network)
        val relationType = serviceManager.cache.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network)) ?: throw Exception("RelationType not found in cache!")

        //val query = BGFindBinaryPPIsBetweenNodesQuery(serviceManager, fromNodeUri, toNodeUri)

        val PPI_URI = "http://purl.obolibrary.org/obo/RO_0002436"

        // The PPI edges returns a multipart type results.

        val returnType = when (edgeUri == PPI_URI) {
            false -> BGReturnType.RELATION_MULTIPART
            true -> BGReturnType.RELATION_TRIPLE_GRAPHURI
        }

        val query = BGExpandRelationToNodesQuery(serviceManager, fromNodeUri, toNodeUri, relationType, returnType)
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
                    serviceManager.dataModelController.networkBuilder.expandEdgeWithRelations(netView, edgeView, newNodes, relations)
                }
            }
        }
        return TaskIterator(query)
    }

    override fun isReady(edgeView: View<CyEdge>?, networkView: CyNetworkView?): Boolean {
        // Check if edge can be expanded.
        if (edgeView == null || networkView == null) return false
        val edge = edgeView.model
        val network = networkView.model

        val relationType = serviceManager.cache.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network))

        if (relationType == null) return false
        return relationType.expandable

        //val expandable = edgeView.model.getExpandable(networkView.model)
    }

}