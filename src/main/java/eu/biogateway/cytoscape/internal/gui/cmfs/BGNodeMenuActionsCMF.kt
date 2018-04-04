package eu.biogateway.cytoscape.internal.gui.cmfs

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.gui.BGQueryBuilderController
import eu.biogateway.cytoscape.internal.gui.BGRelationSearchResultsController
import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.query.*
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Utility
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Created by sholmas on 06/07/2017.
 */

class BGNodeMenuActionsCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNodeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val network = netView?.model ?: return CyMenuItem(null, gravity)
        val nodeUri = network.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")
        val node = serviceManager.dataModelController.searchForExistingNode(nodeUri) ?: throw Exception("Node not found!")

        val nodeMenu = createNodeMenu(node, nodeUri, network)
        return CyMenuItem(nodeMenu as JMenuItem?, gravity)
    }

    fun createNodeMenu(node: BGNode, nodeUri: String, network: CyNetwork): JMenu {
        var parentMenu = JMenu("BioGateway")

        parentMenu.add(createRelationSearchMenu("Fetch relations FROM node", network, nodeUri, BGRelationDirection.FROM))
        parentMenu.addSeparator() // Weird bug that doesn't show even numbered menu items, so we're adding a separator (that won't be shown) as a workaround.
        parentMenu.add(createRelationSearchMenu("Fetch relations TO node", network, nodeUri, BGRelationDirection.TO))

        when (node.type) {
            BGNodeType.Protein -> {
                parentMenu.addSeparator()
                parentMenu.add(createTFTGSearchMenu(network, BGNodeType.Protein, nodeUri))
                parentMenu.addSeparator()
                parentMenu.add(createFetchAssociatedGeneOrProteinMenuItem(network, BGNodeType.Protein, nodeUri))
                parentMenu.addSeparator()
                parentMenu.add(createPPISearchMenu(network, nodeUri))
            }
            BGNodeType.Gene -> {
                parentMenu.addSeparator()
                parentMenu.add(createTFTGSearchMenu(network, BGNodeType.Gene, nodeUri))
                parentMenu.addSeparator()
                parentMenu.add(createFetchAssociatedGeneOrProteinMenuItem(network, BGNodeType.Gene, nodeUri))
            }
            BGNodeType.PPI, BGNodeType.GOA, BGNodeType.TFTG -> {
                createPubmedURIMenuList(network, nodeUri)?.let {
                    parentMenu.addSeparator()
                    parentMenu.add(it)
                }
            }
            else -> {
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


        val nodeType = BGNode(nodeUri).type

        val relationUri = when (nodeType) {
            BGNodeType.TFTG -> REFERENCE_URI
            BGNodeType.GOA -> RELATED_MATCH_URI
            else -> {
                HAS_SOURCE_URI
            }
        }



        val query = BGFetchAttributeValuesQuery(serviceManager, nodeUri, relationUri, "?graph", BGRelationDirection.FROM)
        query.run()
        val data = query.returnData as? BGReturnMetadata ?: return null

        val validResults = data.values.filter { it.toLowerCase().contains("pubmed") }

        val validURLs = validResults.map { it.replace("PubMed:", "http://identifiers.org/pubmed/") }

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

    fun createFetchPubmedMenuItem(network: CyNetwork, nodeUri: String): JMenuItem {
        val item = JMenuItem("Get annotation data")

        val HAS_SOURCE_URI = "http://semanticscience.org/resource/SIO_000253"
        val RELATED_MATCH_URI = "http://www.w3.org/2004/02/skos/core#relatedMatch"
        val hasSourceType = serviceManager.cache.getRelationTypesForURI(HAS_SOURCE_URI)?.first() ?: throw Exception("Relation type not found in cache!")
        val relatedMatchType = serviceManager.cache.getRelationTypesForURI(RELATED_MATCH_URI)?.first() ?: throw Exception("Relation type not found in cache!")

        val relationTypes = arrayListOf(hasSourceType, relatedMatchType)

        item.addActionListener {
            val query = BGFindRelationsOfDifferentTypesForNodeQuery(serviceManager, relationTypes, nodeUri, BGRelationDirection.FROM)
            query.addCompletion {
                val data = it as? BGReturnRelationsData ?: throw Exception("Invalid return data.")
                BGLoadUnloadedNodes.createAndRun(serviceManager, data.unloadedNodes) {
                    serviceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, data.relationsData)
                    Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)
                }
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
        }

        return item
    }

    // Experimental functionality allowing users to search for relations to or from a CyGroup:
    fun createSearchGroupMenu(description: String, network: CyNetwork, nodeType: BGNodeType, nodeUri: String): JMenu {
        val parentMenu = JMenu(description)
        parentMenu.add(createRelationSearchMenu("Fetch relations FROM node", network, nodeUri, BGRelationDirection.FROM, true))
        parentMenu.add(createRelationSearchMenu("Fetch relations TO node", network, nodeUri, BGRelationDirection.TO, true))

        if (nodeUri.contains("ncbigene")) {
            parentMenu.add(createTFTGSearchMenu(network, BGNodeType.Gene, nodeUri, true))
        } else if (nodeUri.contains("uniprot")) {
            parentMenu.add(createTFTGSearchMenu(network, BGNodeType.Protein, nodeUri, true))
            parentMenu.add(createPPISearchMenu(network, nodeUri, true))
        }
        return parentMenu
    }

    protected fun createOpenQueryBuilderWithSelectedURIsMenu(nodeUri: String): JMenuItem {
        val item = JMenuItem("Use node URI in query builder")
        item.addActionListener {
            val queryBuilder = BGQueryBuilderController(serviceManager)
            queryBuilder.addMultiQueryLinesForURIs(arrayListOf(nodeUri))

        }
        return item
    }

    protected fun createFetchAssociatedGeneOrProteinMenuItem(network: CyNetwork, nodeType: BGNodeType, nodeUri: String): JMenuItem {
        var menuItemText = when (nodeType) {
            BGNodeType.Gene -> "Get associated proteins"
            BGNodeType.Protein -> "Get associated genes"
            else -> {
                ""
            }
        }
        val direction = when (nodeType) {
            BGNodeType.Protein -> BGRelationDirection.TO
            BGNodeType.Gene -> BGRelationDirection.FROM
            else -> {
                throw Exception("Must be gene or protein!")
            }
        }
        val encodesIdentifier = Utility.createRelationTypeIdentifier("http://semanticscience.org/resource/SIO_010078", "refseq")
        val relationType = serviceManager.dataModelController.cache.relationTypeMap.get(encodesIdentifier) ?: throw Exception("Relation type with identifier: "+encodesIdentifier+" not found in cache.")
        val menuItem = JMenuItem(menuItemText)

        menuItem.addActionListener {
            val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
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
                    BGLoadNodeDataFromBiogwDict.createAndRun(serviceManager, returnData.unloadedNodes, 200) {
                        serviceManager.dataModelController.networkBuilder.addRelationsToNetwork(network, relationsData)
                        Utility.reloadCurrentVisualStyleCurrentNetworkView(serviceManager)
                    }
                }
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
        }
        return menuItem
    }


    protected fun createRelationSearchMenu(description: String, network: CyNetwork, nodeUri: String, direction: BGRelationDirection, lookForGroups: Boolean = false): JMenu {

        val parentMenu = JMenu(description)

        val searchAllItem = JMenuItem("Search for all relation types")
        searchAllItem.addActionListener {
            println("Searching for all relations.")
            val query = BGFindAllRelationsForNodeQuery(serviceManager, nodeUri, direction)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                val columnNames = arrayOf("from node","relation type", "to node")

                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                    BGRelationSearchResultsController(serviceManager, returnData, columnNames, network)
                }
            }
            if (lookForGroups) {
                val group = Utility.selectGroupPopup(serviceManager, network) ?: return@addActionListener
                val groupNodeURIs = Utility.getNodeURIsForGroup(group)
                query.returnDataFilter = { relation ->
                    (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
                }
            }

            serviceManager.taskManager?.execute(TaskIterator(query))
        }
        parentMenu.add(searchAllItem)

        // Will only create the menu if the config is loaded.
        //for (relationType in serviceManager.cache.relationTypeMap.values.sortedBy { it.number }) {
        for (relationType in serviceManager.cache.filteredRelationTypeMap.values.sortedBy { it.number }) {

            // Skip the relations that cannot return data:
            val nodeType = BGNode(nodeUri, "").type
            if (nodeType != BGNodeType.Undefined) {
                if (direction == BGRelationDirection.FROM && relationType.fromType != null && nodeType != relationType.fromType) continue
                if (direction == BGRelationDirection.TO && relationType.toType != null && nodeType != relationType.toType) continue
            }

            val item = JMenuItem(relationType.description)

            item.addActionListener {
                val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        if (returnData.relationsData.size == 0) throw Exception("No relations found.")

                        val columnNames = arrayOf("from node","relation type", "to node")

                        BGLoadNodeDataFromBiogwDict.createAndRun(serviceManager, returnData.unloadedNodes, 200) {
                            println("Loaded "+it.toString()+ " nodes.")
                            BGRelationSearchResultsController(serviceManager, returnData, columnNames, network)
                        }
                    }
                }
                if (lookForGroups) {
                    val group = Utility.selectGroupPopup(serviceManager, network) ?: return@addActionListener
                    val groupNodeURIs = Utility.getNodeURIsForGroup(group)
                    query.returnDataFilter = { relation ->
                        (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
                    }
                }

                serviceManager.taskManager?.execute(TaskIterator(query))
            }

            parentMenu.add(item)
        }
        return parentMenu
    }

    protected fun createTFTGSearchMenu(network: CyNetwork, nodeType: BGNodeType, nodeUri: String, lookForGroups: Boolean = false): JMenuItem {

        var menuItemText = ""

        if (nodeType == BGNodeType.Protein) {
            menuItemText = "Find genes regulated by this protein"
        } else if (nodeType == BGNodeType.Gene) {
            menuItemText = "Find proteins regulating this gene"
        }

        val searchTFTG = JMenuItem(menuItemText)
        searchTFTG.addActionListener {
            /*
            val query = BGFindGraphRelationForNodeQuery(serviceManager, nodeType, nodeUri)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.size == 0)  {
                    if (nodeType == BGNodeType.Protein) throw Exception("No results found. Are you sure it is a transcription factor?")
                    throw Exception("No relations found.")
                }
                val columnNames = arrayOf("protein", "relation", "gene")

                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                    BGRelationSearchResultsController(serviceManager, returnData, columnNames, network)
                }
            }*/
            val relationType = serviceManager.cache.getRelationTypeForURIandGraph("http://www.w3.org/2004/02/skos/core#related", "tf-tg") ?: throw Exception("Unable to find relation type in cache!")
            val direction = when (nodeType) {
                BGNodeType.Protein -> BGRelationDirection.FROM
                BGNodeType.Gene -> BGRelationDirection.TO
                else -> {
                    throw Exception("Unable to search for TF-TG relations from a node that are not a protein or gene!")
                }}
            val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
            if (lookForGroups) {
                val group = Utility.selectGroupPopup(serviceManager, network) ?: return@addActionListener
                val groupNodeURIs = Utility.getNodeURIsForGroup(group)
                query.returnDataFilter = { relation ->
                    (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
                }
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
            Thread(Runnable {
                val returnData = query.futureReturnData.get() as  BGReturnRelationsData
                // TODO: The exception is no longer thrown to the TaskMonitor, need a popup to tell the user.
                if (returnData.relationsData.size == 0)  {
                    if (nodeType == BGNodeType.Protein) {
                        JOptionPane.showMessageDialog(null,  "No relations found. Are you sure it is a transcription factor?","No results found", JOptionPane.INFORMATION_MESSAGE)
                        return@Runnable
                    } else {
                        JOptionPane.showMessageDialog(null,  "No relations found.", "No results found", JOptionPane.INFORMATION_MESSAGE)
                        return@Runnable
                    }
                }
                val columnNames = arrayOf("protein", "relation", "gene")

                BGLoadNodeDataFromBiogwDict.createAndRun(serviceManager, returnData.unloadedNodes, 200) {
                    BGRelationSearchResultsController(serviceManager, returnData, columnNames, network)
                }
            }).start()
        }
        return searchTFTG
    }

    protected fun createPPISearchMenu(network: CyNetwork, nodeUri: String, lookForGroups: Boolean = false): JMenuItem {

        var menuItemText = "Find binary protein interactions"

        val searchTFTG = JMenuItem(menuItemText)
        searchTFTG.addActionListener {
            val query = BGFindBinaryPPIsQuery(serviceManager, nodeUri)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.isEmpty()) throw Exception("No relations found.")
                val columnNames = arrayOf("Protein", "Relation", "Protein")
                BGLoadNodeDataFromBiogwDict.createAndRun(serviceManager, returnData.unloadedNodes,200) {
                    BGRelationSearchResultsController(serviceManager, returnData, columnNames, network)
                }
            }
            if (lookForGroups) {
                val group = Utility.selectGroupPopup(serviceManager, network) ?: return@addActionListener
                val groupNodeURIs = Utility.getNodeURIsForGroup(group)
                query.returnDataFilter = { relation ->
                    (groupNodeURIs.contains(relation.fromNode.uri) || groupNodeURIs.contains(relation.toNode.uri))
                }
            }
            serviceManager.taskManager?.execute(TaskIterator(query))
        }
        return searchTFTG
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

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(uri))
                }
            }
            return menuItem
        }
        return null
    }
}