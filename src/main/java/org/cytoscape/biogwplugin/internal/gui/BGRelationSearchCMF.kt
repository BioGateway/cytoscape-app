package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Created by sholmas on 06/07/2017.
 */

class BGRelationSearchCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNodeViewContextMenuFactory {

    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val nodeUri = netView?.model?.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get(Constants.BG_FIELD_IDENTIFIER_URI, String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")

        var parentMenu = JMenu("BioGateway")

        parentMenu.add(createRelationSearchMenu("Fetch Relations from this node", netView, nodeUri, BGRelationDirection.FROM))
        parentMenu.addSeparator() // Weird bug that doesn't show even numbered menu items, so we're adding a separator (that won't be shown) as a workaround.
        parentMenu.add(createRelationSearchMenu("Fetch Relations to this node", netView, nodeUri, BGRelationDirection.TO))


        if (nodeUri.contains("ncbigene")) {
            parentMenu.addSeparator()
            parentMenu.add(createTFTGSearchMenu(netView, BGNodeType.Gene, nodeUri))
            parentMenu.addSeparator()
            parentMenu.add(createFetchAssociatedGeneOrProteinMenuItem(netView, BGNodeType.Gene, nodeUri))
        } else if (nodeUri.contains("uniprot")) {
            parentMenu.addSeparator()
            parentMenu.add(createTFTGSearchMenu(netView, BGNodeType.Protein, nodeUri))
            parentMenu.addSeparator()
            parentMenu.add(createFetchAssociatedGeneOrProteinMenuItem(netView, BGNodeType.Protein, nodeUri))
        }

        return CyMenuItem(parentMenu, gravity)
    }

    fun createFetchAssociatedGeneOrProteinMenuItem(netView: CyNetworkView, nodeType: BGNodeType, nodeUri: String): JMenuItem {
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
        val encodesUri = "http://semanticscience.org/resource/SIO_010078"
        val relationType = serviceManager.server.cache.relationTypeMap.get(encodesUri) ?: throw Exception("Relation type with uri: "+encodesUri+" not found in cache.")
        val menuItem = JMenuItem(menuItemText)

        menuItem.addActionListener {
            val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData
                if (returnData != null) {
                    if (returnData.relationsData.size == 0) {
                        throw Exception("No relationsFound found.")
                    }
                    val network = netView.model
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
                    BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                        serviceManager.server.networkBuilder.addRelationsToNetwork(network, relationsData)
                    }
                }
            }
            serviceManager.taskManager.execute(TaskIterator(query))
        }
        return menuItem
    }


    fun createRelationSearchMenu(description: String, netView: CyNetworkView, nodeUri: String, direction: BGRelationDirection): JMenu {

        val parentMenu = JMenu(description)

        val searchAllItem = JMenuItem("Search for all relation types")
        searchAllItem.addActionListener {
            println("Searching for all relations.")
            val query = BGFindAllRelationsForNodeQuery(serviceManager, nodeUri, direction)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                val network = netView.model
                val columnNames = arrayOf("from node","relation type", "to node")

                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                    BGRelationSearchResultsController(serviceManager, returnData.relationsData, columnNames, network)
                }
            }
            serviceManager.taskManager.execute(TaskIterator(query))
        }
        parentMenu.add(searchAllItem)

        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypeMap.values.sortedBy { it.number }) {
            val item = JMenuItem(relationType.name)

            item.addActionListener(ActionListener {
                val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        val network = netView.model
                        if (returnData.relationsData.size == 0) throw Exception("No relationsFound found.")

                        val columnNames = arrayOf("from node","relation type", "to node")

                        BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                            println("Loaded "+it.toString()+ " nodes.")
                            BGRelationSearchResultsController(serviceManager, returnData.relationsData, columnNames, network)
                        }
                    }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            })

            parentMenu.add(item)
        }
        return parentMenu
    }

    fun createTFTGSearchMenu(netView: CyNetworkView, nodeType: BGNodeType, nodeUri: String): JMenuItem {

        var menuItemText = ""

        if (nodeType == BGNodeType.Protein) {
            menuItemText = "Find genes regulated by this protein"
        } else if (nodeType == BGNodeType.Gene) {
            menuItemText = "Find proteins regulating this gene"
        }

        val searchTFTG = JMenuItem(menuItemText)
        searchTFTG.addActionListener {
            val query = BGFindGraphRelationForNodeQuery(serviceManager, nodeType!!, nodeUri)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.size == 0)  {
                    if (nodeType == BGNodeType.Protein) throw Exception("No results found. Are you sure it is a transcription factor?")
                    throw Exception("No relations found.")
                }
                val network = netView.model
                //serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
                val columnNames = arrayOf("protein", "relation", "gene")

                BGLoadUnloadedNodes.createAndRun(serviceManager, returnData.unloadedNodes) {
                    BGRelationSearchResultsController(serviceManager, returnData.relationsData, columnNames, network)
                }
            }

            serviceManager.taskManager.execute(TaskIterator(query))
        }
        return searchTFTG
    }
}