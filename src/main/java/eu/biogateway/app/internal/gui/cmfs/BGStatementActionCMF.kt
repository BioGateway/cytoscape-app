package eu.biogateway.app.internal.gui.cmfs

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory
import org.cytoscape.application.swing.CyMenuItem
import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.model.BGRelationType
import eu.biogateway.app.internal.model.BGStatementContextMenuAction
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
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionListener
import java.net.URI
import javax.swing.JMenu
import javax.swing.JMenuItem


class BGStatementActionCMF(val gravity: Float): CyEdgeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, edgeView: View<CyEdge>?): CyMenuItem {

        if (netView != null && edgeView != null) {
            val parentMenu = JMenu("BioGateway")
            val network = netView.model

            if (isStatement(edgeView, netView)) {
                val actions = getValidActions(edgeView, network)
                actions?.let {
                    it.first.forEach { action ->
                        createStatementContextMenuAction(action, network, edgeView, it.second)?.let { menu ->
                            parentMenu.add(menu)
                            parentMenu.addSeparator()
                        }
                    }
                }
            }

            return CyMenuItem(parentMenu, gravity)
        }
        return CyMenuItem(null, gravity)
    }

    private fun getValidActions(edgeView: View<CyEdge>, network: CyNetwork): Pair<List<BGStatementContextMenuAction>, BGRelationType>? {
        try {
            val edge = edgeView.model ?: throw Exception("CyEdge is null!")
            val relationType = BGServiceManager.config.getRelationTypeForURIandGraph(
                edge.getUri(network),
                edge.getSourceGraph(network)
            ) ?: throw Exception("RelationType not found in config!")
            val list = BGServiceManager.config.statementContextMenuActions.map { it.value }.filter { action ->
                action.supportedRelations.contains(relationType)
            }
            return Pair(list, relationType)
        } catch (exception: java.lang.Exception) {
            return null
        }
    }

    private fun isStatement(edgeView: View<CyEdge>?, networkView: CyNetworkView?): Boolean {
        // Check if edge can be expanded.
        if (edgeView == null || networkView == null) return false
        val edge = edgeView.model
        val network = networkView.model

        val relationType = BGServiceManager.config.getRelationTypeForURIandGraph(edge.getUri(network), edge.getSourceGraph(network))

        if (relationType == null) return false
        return relationType.expandable
    }



    fun createStatementContextMenuAction(action: BGStatementContextMenuAction, network: CyNetwork, edgeView: View<CyEdge>, relationType: BGRelationType): JMenuItem? {

        val validURLs = getResourceURIs(action.resourceURI, network, edgeView, relationType)

        if (validURLs.isEmpty()) return null

        val actionHandler = { uri: String ->
            when (action.type) {
                BGStatementContextMenuAction.ActionType.OPEN_URI -> {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(uri))
                    }
                }
                BGStatementContextMenuAction.ActionType.COPY_URI -> {
                    val selection = StringSelection(uri)
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(selection, selection)
                }
            }
        }

//        if (validURLs.size == 1) {
//            val item = JMenuItem(action.singleLabel)
//            item.addActionListener {
//                actionHandler(validURLs[0])
//            }
//            return item
//        }


        val menu = JMenu(action.multipleLabel)
        for ((index, uri) in validURLs.withIndex()) {
            val label = "["+(index+1)+"]: " + uri
            val item = JMenuItem(label)
            item.addActionListener {
                actionHandler(uri)
            }
            menu.add(item)
        }
        return menu
    }


    fun getResourceURIs(metadataUri: String, network: CyNetwork, edgeView: View<CyEdge>, relationType: BGRelationType): List<String> {
        val edge = edgeView.model ?: throw  Exception("CyEdge is null!")
        try {
            val fromNodeUri = edge.source.getUri(network)
            val toNodeUri = edge.target.getUri(network)
            val edgeUri = edge.getUri(network)
            val graphUri = edge.getSourceGraph(network)

            val query = BGFetchEdgeAttributeValuesQuery(fromNodeUri, toNodeUri, edgeUri, metadataUri, graphUri, biDirectional = !relationType.directed)
            query.run()
            val data = query.returnData as? BGReturnMetadata ?: return listOf()

            val validURLs = data.values.filter { it.toLowerCase().startsWith("http") }.map {Utility.sanitizeParameter(it)}

            return validURLs

        } catch (error: java.lang.Exception) {
            println(error.localizedMessage)
            return listOf()
        }
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