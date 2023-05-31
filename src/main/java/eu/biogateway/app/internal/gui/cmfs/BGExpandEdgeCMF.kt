package eu.biogateway.app.internal.gui.cmfs

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.parser.BGReturnType
import eu.biogateway.app.internal.parser.getSourceGraph
import eu.biogateway.app.internal.parser.getUri
import eu.biogateway.app.internal.query.*
import eu.biogateway.app.internal.util.Utility
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.Desktop
import java.net.URI
import javax.swing.JMenu
import javax.swing.JMenuItem


class BGExpandEdgeCMF(val gravity: Float): CyEdgeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {

        val RELATED_MATCH_URI = "<http://www.w3.org/2004/02/skos/core#relatedMatch>"
        val HAS_SOURCE_URI = "<http://semanticscience.org/resource/SIO_000253>"
        val REFERENCE_URI = "<http://www.w3.org/2004/02/skos/core#reference>"
        val HAS_EVIDENCE_URI = "<http://semanticscience.org/resource/SIO_000772>"
        val PART_OF_PATHWAY_URI = "<http://purl.obolibrary.org/obo/BFO_0000050>"

        if (netView != null && edgeView != null) {
            val parentMenu = JMenu("BioGateway")
            val network = netView.model

            val testItem = JMenuItem("Test")
            testItem.addActionListener {
                println("TESTING")
            }
            parentMenu.add(testItem)
            parentMenu.addSeparator()

            createOpenResourceURIMenuList(netView, edgeView)?.let { menu ->
                parentMenu.add(menu)
                parentMenu.addSeparator()
            }

            createOpenResourceMenu(HAS_EVIDENCE_URI, "Show evidence", network, edgeView)?.let { menu ->
                parentMenu.add(menu)
                parentMenu.addSeparator()
            }

            createOpenResourceMenu(PART_OF_PATHWAY_URI, "Show pathway", network, edgeView)?.let { menu ->
                parentMenu.add(menu)
                parentMenu.addSeparator()
            }

//            if (isExpandable(edgeView, netView)) {
//                createOpenResourceURIMenuList(netView, edgeView)?.let { menu ->
//                    parentMenu.add(menu)
//                    parentMenu.addSeparator()
//                }
//
//                val expandItem = JMenuItem("Expand")
//                expandItem.addActionListener {
//                    val task = createExpandMenuTask(netView, edgeView)
//                    BGServiceManager.taskManager?.execute(task)
//                }
//                parentMenu.add(expandItem)
//                parentMenu.addSeparator()
//            }

            return CyMenuItem(parentMenu, gravity)
        }
        return CyMenuItem(null, gravity)
    }

    private fun isExpandable(edgeView: View<CyEdge>?, networkView: CyNetworkView?): Boolean {
        // Check if edge can be expanded.
        if (edgeView == null || networkView == null) return false
        val edge = edgeView.model
        val network = networkView.model

        val relationType = BGServiceManager.config.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network))

        if (relationType == null) return false
        return relationType.expandable
    }

    fun createOpenResourceURIMenuList(networkView: CyNetworkView, edgeView: View<CyEdge>): JMenuItem? {
        val network = networkView.model
        val edge = edgeView.model ?: throw  Exception("CyEdge is null!")

        val fromNodeUri = edge.source.getUri(network)
        val toNodeUri = edge.target.getUri(network)
        val relationType = BGServiceManager.config.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network)) ?: throw Exception("RelationType not found in config!")
        val query = BGExpandRelationToNodesQuery(fromNodeUri, toNodeUri, relationType, BGReturnType.RELATION_MULTIPART)
        query.run()
        val data = query.returnData as? BGReturnRelationsData ?: return null
        val instances = data.relationsData.map { it.fromNode.uri }.toSet()

        val validURLs = instances.map { Utility.sanitizeParameter(it)}

        if (validURLs.size == 0) return null

        if (validURLs.size == 1) {
            val item = JMenuItem("Open source URI")
            item.addActionListener {
                if ( Desktop.isDesktopSupported() ) {
                    Desktop.getDesktop().browse(URI(validURLs[0]))
                }
            }
            return item
        }

        val menu = JMenu("Open source URIs")
        for ((index, uri) in validURLs.withIndex()) {
            val label = "["+(index+1)+"]: " + uri
            val item = JMenuItem(label)
            item.addActionListener {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(uri))
                }
            }
            menu.add(item)
        }
        return menu
    }

fun createOpenResourceMenu(metadataUri: String, menuItemLabel: String, network: CyNetwork, edgeView: View<CyEdge>): JMenuItem? {
    val edge = edgeView.model ?: throw  Exception("CyEdge is null!")

    val fromNodeUri = edge.source.getUri(network)
    val toNodeUri = edge.target.getUri(network)
    val edgeUri = edge.getUri(network)
    val graphUri = edge.getSourceGraph(network)

    val query = BGFetchEdgeAttributeValuesQuery(fromNodeUri, toNodeUri, edgeUri, metadataUri, graphUri)
    query.run()
    val data = query.returnData as? BGReturnMetadata ?: return null

    val validURLs = data.values.filter { it.toLowerCase().contains("http://") }.map {Utility.sanitizeParameter(it)}

    if (validURLs.isEmpty()) return null

    if (validURLs.size == 1) {
        val item = JMenuItem(menuItemLabel)
        item.addActionListener {
            if ( Desktop.isDesktopSupported() ) {
                Desktop.getDesktop().browse(URI(validURLs[0]))
            }
        }
        return item
    }

    val menu = JMenu(menuItemLabel+"s")
    for ((index, uri) in validURLs.withIndex()) {
        val label = "["+(index+1)+"]: " + uri
        val item = JMenuItem(label)
        item.addActionListener {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(uri))
            }
        }
        menu.add(item)
    }
    return menu
}

    fun createExpandMenuTask(netView: CyNetworkView, edgeView: View<CyEdge>): TaskIterator {
        val network = netView.model
        val edge = edgeView.model ?: throw  Exception("CyEdge is null!")

        val fromNodeUri = edge.source.getUri(network)
        val toNodeUri = edge.target.getUri(network)
        val edgeUri = edge.getUri(network)
        val relationType = BGServiceManager.config.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network)) ?: throw Exception("RelationType not found in config!")

        val query = BGExpandRelationToNodesQuery(fromNodeUri, toNodeUri, relationType, BGReturnType.RELATION_MULTIPART)
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
                    BGServiceManager.dataModelController.networkBuilder.expandEdgeWithRelations(netView, edgeView, newNodes, relations)
                }
            }
        }
        return TaskIterator(query)
    }

}