package eu.biogateway.app.internal.gui.cmfs

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.gui.BGQueryBuilderController
import eu.biogateway.app.internal.gui.BGRelationSearchResultsController
import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.model.BGNodeType
import eu.biogateway.app.internal.query.*
import eu.biogateway.app.internal.util.Constants
import eu.biogateway.app.internal.util.Utility
import org.apache.commons.lang3.SystemUtils
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.net.URI
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * Created by sholmas on 06/07/2017.
 */

class BGNodeMenuActionsCMF(val gravity: Float): CyNodeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val network = netView?.model ?: return CyMenuItem(null, gravity)
        val nodeUri = network.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")
        val node = BGServiceManager.dataModelController.searchForExistingNode(nodeUri) ?: throw Exception("Node not found!")

        val nodeMenu = createNodeMenu(node, nodeUri, network)
        return CyMenuItem(nodeMenu as JMenuItem?, gravity)
    }

    fun createNodeMenu(node: BGNode, nodeUri: String, network: CyNetwork): JMenu {
        var parentMenu = JMenu("BioGateway")

        parentMenu.add(createRelationSearchMenu("Fetch relations FROM node", network, nodeUri, BGRelationDirection.FROM))
        parentMenu.addSeparator() // Weird bug that doesn't show even numbered menu items, so we're adding a separator (that won't be shown) as a workaround.
        parentMenu.add(createRelationSearchMenu("Fetch relations TO node", network, nodeUri, BGRelationDirection.TO))

        when (node.type.typeClass) {
            BGNodeType.BGNodeTypeClass.ENTITY -> {
                createFetchAssociatedGeneOrProteinMenuItem(network, node.type, nodeUri)?.let {
                    parentMenu.addSeparator()
                    parentMenu.add(it)
                }
            }
            BGNodeType.BGNodeTypeClass.STATEMENT,
            BGNodeType.BGNodeTypeClass.UNDIRECTED_STATEMENT-> {
                createPubmedURIMenuList(network, nodeUri)?.let {
                    parentMenu.addSeparator()
                    parentMenu.add(it)
                }
                createRelatedResourceURIMenuList(network, nodeUri)?.let {
                    parentMenu.addSeparator()
                    parentMenu.add(it)
                }
            }
        }

        createCopyURIMenu(nodeUri)?.let {
            parentMenu.addSeparator()
            parentMenu.add(it)
        }

        createOpenQueryBuilderWithSelectedURIsMenu(nodeUri)?.let {
            parentMenu.addSeparator()
            parentMenu.add(it)
        }

        createOpenURIMenu(nodeUri)?.let {
            parentMenu.addSeparator()
            parentMenu.add(it)
        }

        return parentMenu
    }

    fun createPubmedURIMenuList(network: CyNetwork, nodeUri: String): JMenuItem? {
        val menu = JMenu("Open PubMed Annotations")

        val RELATED_MATCH_URI = "http://www.w3.org/2004/02/skos/core#relatedMatch"
        val HAS_SOURCE_URI = "http://semanticscience.org/resource/SIO_000253"
        val REFERENCE_URI = "http://www.w3.org/2004/02/skos/core#reference"
        val HAS_EVIDENCE_URI = "http://semanticscience.org/resource/SIO_000772"


        val nodeType = BGNode(nodeUri).type

        val relationUri = HAS_EVIDENCE_URI



        val query = BGFetchAttributeValuesQuery(nodeUri, relationUri, "?graph", BGRelationDirection.FROM)
        query.run()
        val data = query.returnData as? BGReturnMetadata ?: return null

        //TODO: This is assuming that PubMed nodes are not using URIs, but just PubMedID's.
        val validURLs = data.values.filter { it.toLowerCase().contains("pubmed") }.map { it.replace("PubMed:", "http://identifiers.org/pubmed/") }
        //val validURLs = validResults.map { "http://identifiers.org/pubmed/"+it }

        if (validURLs.size == 0) return null

        if (validURLs.size == 1) {
            val item = JMenuItem("Open PubMed Annotation")
            item.addActionListener {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(validURLs[0]))
                }
            }
            return item
        }

        val uris = validURLs.map { Utility.sanitizeParameter(it) }

        for ((index, uri) in uris.withIndex()) {
            val label = "["+(index+1)+"]: "+uri
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

    fun createRelatedResourceURIMenuList(network: CyNetwork, nodeUri: String): JMenuItem? {

        val RELATED_MATCH_URI = "http://schema.org/evidenceOrigin"

        val query = BGFetchAttributeValuesQuery(nodeUri, RELATED_MATCH_URI, "?graph", BGRelationDirection.FROM)
        query.run()
        val data = query.returnData as? BGReturnMetadata ?: return null

        //TODO: This is assuming that PubMed nodes are not using URIs, but just PubMedID's.
        val validURLs = data.values.filter { it.toLowerCase().contains("http://") }.map {Utility.sanitizeParameter(it)}

        if (validURLs.size == 0) return null

        if (validURLs.size == 1) {
            val item = JMenuItem("Open Evidence URL")
            item.addActionListener {
                if ( Desktop.isDesktopSupported() ) {
                    Desktop.getDesktop().browse(URI(validURLs[0]))
                }
            }
            return item
        }

        val menu = JMenu("Open Evidence URLs")
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

    protected fun createOpenQueryBuilderWithSelectedURIsMenu(nodeUri: String): JMenuItem {
        val item = JMenuItem("Use node URI in query builder")
        item.addActionListener {
            val queryBuilder = BGQueryBuilderController()
            queryBuilder.addMultiQueryLinesForURIs(arrayListOf(nodeUri))

        }
        return item
    }

    protected fun createFetchAssociatedGeneOrProteinMenuItem(network: CyNetwork, nodeType: BGNodeType, nodeUri: String): JMenuItem? {
        var menuItemText = when (nodeType.id) {
            "gene" -> "Get associated proteins"
            "protein" -> "Get associated genes"
            else -> {
                return null
            }
        }
        val direction = when (nodeType.id) {
            "protein" -> BGRelationDirection.TO
            "gene" -> BGRelationDirection.FROM
            else -> {
                return null
            }
        }
        val graphUri = BGServiceManager.config.datasetGraphs.get("refseq") ?: return null
        val encodesIdentifier = Utility.createRelationTypeIdentifier("http://semanticscience.org/resource/SIO_010078", graphUri)

        val relationType = BGServiceManager.dataModelController.config.relationTypeMap.get(encodesIdentifier) ?: return null
        //throw Exception("Relation type with identifier: "+encodesIdentifier+" not found in config.")
        val menuItem = JMenuItem(menuItemText)

        menuItem.addActionListener {
            val query = BGFindRelationForNodeQuery(relationType, nodeUri, direction)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData
                if (returnData != null) {
                    if (returnData.relationsData.size == 0) {
                        throw Exception("No relationsFound found.")
                    }
                    var relationsData = returnData.relationsData

                    // Remove those pesky "protein" and "molecule" entries.
                    // WARNING: Yes, I know it's bad to do this deep in the code.
                    // TODO: Update the query to ignore "higher order" metadata.
                    val iterator = relationsData.listIterator()
                    while (iterator.hasNext()) {
                        val relation = iterator.next()
                        if (relation.toNode.uri == "http://semanticscience.org/resource/SIO_011125" ||
                                relation.toNode.uri == "http://semanticscience.org/resource/SIO_010043") {
                            iterator.remove()
                        }
                    }
                    BGLoadNodeDataFromBiogwDict.createAndRun(returnData.unloadedNodes, 200) {
                        BGServiceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, relationsData)
                        Utility.reloadCurrentVisualStyleCurrentNetworkView()
                    }
                }
            }
            BGServiceManager.taskManager?.execute(TaskIterator(query))
        }
        return menuItem
    }


    protected fun createRelationSearchMenu(description: String, network: CyNetwork, nodeUri: String, direction: BGRelationDirection, lookForGroups: Boolean = false): JMenu {

        val parentMenu = JMenu(description)

        val searchAllItem = JMenuItem("Search for all relation types")
        searchAllItem.addActionListener {
            println("Searching for all relations.")
            val query = BGFindAllRelationsForNodeQuery(nodeUri, direction)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                val columnNames = arrayOf("from node","relation type", "to node")

                BGLoadUnloadedNodes.createAndRun(returnData.unloadedNodes) {
                    BGRelationSearchResultsController(returnData, columnNames, network)
                }
            }
            if (lookForGroups) {
                val group = Utility.selectGroupPopup(network) ?: return@addActionListener
                val groupNodeURIs = Utility.getNodeURIsForGroup(group)
                query.returnDataFilter = { relation ->
                    (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
                }
            }

            BGServiceManager.taskManager?.execute(TaskIterator(query))
        }
        parentMenu.add(searchAllItem)

        // Will only create the menu if the config is loaded.
        //for (relationType in serviceManager.config.relationTypeMap.values.sortedBy { it.number }) {
        for (relationType in BGServiceManager.config.filteredRelationTypeMap.values.sortedBy { it.number }) {

            // Skip the relations that cannot return data:
            val nodeType = BGNode(nodeUri, "").type
            if (nodeType != BGNodeType.UNDEFINED) {
                if (direction == BGRelationDirection.FROM && relationType.fromType != null && nodeType != relationType.fromType) continue
                if (direction == BGRelationDirection.TO && relationType.toType != null && nodeType != relationType.toType) continue
            }

            val item = JMenuItem(relationType.description)

            item.addActionListener {
                val query = BGFindRelationForNodeQuery(relationType, nodeUri, direction)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        if (returnData.relationsData.size == 0) throw Exception("No relations found.")

                        val columnNames = arrayOf("from node","relation type", "to node")

                        BGLoadNodeDataFromBiogwDict.createAndRun(returnData.unloadedNodes, 200) {
                            println("Loaded "+it.toString()+ " nodes.")
                            BGRelationSearchResultsController(returnData, columnNames, network)
                        }
                    }
                }
                if (lookForGroups) {
                    val group = Utility.selectGroupPopup(network) ?: return@addActionListener
                    val groupNodeURIs = Utility.getNodeURIsForGroup(group)
                    query.returnDataFilter = { relation ->
                        (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
                    }
                }

                BGServiceManager.taskManager?.execute(TaskIterator(query))
            }

            parentMenu.add(item)
        }
        return parentMenu
    }

    protected fun createCopyURIMenu(nodeUri: String): JMenuItem? {
        if (nodeUri.isEmpty()) return null

        val menuItem = JMenuItem("Copy node URI to clipboard")
        menuItem.addActionListener {
            val selection = StringSelection(nodeUri)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
        }
        return menuItem
    }

    protected fun createOpenURIMenu(nodeUri: String): JMenuItem? {

        if (nodeUri.startsWith("http")) {
            val menuItem = JMenuItem("Open resource URI")
            menuItem.addActionListener {
                // Probably a pubmed uri?

                // TODO: THIS IS JUST A TEST. DO NOT USE!
                val uri = nodeUri.replace("semantic-systems-biology.org/", "semantic-systems-biology.org:8080/")


                if (SystemUtils.IS_OS_LINUX) {
                        // Ubuntu
                        val runtime = Runtime.getRuntime()
                        runtime.exec("/usr/bin/firefox -new-window $uri")

                } else {

                try {
                    if (Desktop.isDesktopSupported()) {

                        Desktop.getDesktop().browse(URI(uri))

                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }}
            return menuItem
        }
        return null
    }
}