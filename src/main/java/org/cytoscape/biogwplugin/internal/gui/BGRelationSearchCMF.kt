package org.cytoscape.biogwplugin.internal.gui

import org.cytoscape.application.swing.CyMenuItem
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory
import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNodeType
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import java.awt.event.ActionListener
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * Created by sholmas on 06/07/2017.
 */

class BGRelationSearchCMF(val gravity: Float, val serviceManager: BGServiceManager): CyNodeViewContextMenuFactory {

    /*
    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val nodeUri = netView?.model?.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get("identifier uri", String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")
        var parentMenu = JMenu(description)


        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypes.values) {
            val item = JMenuItem(relationType.description)

            item.addActionListener(ActionListener {
                println("TODO: Should search for relations of type \""+relationType.description+"\" from node "+nodeUri)
                val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        val network = netView.model
                        serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
                    }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            })

            parentMenu.add(item)
        }
        return CyMenuItem(parentMenu, 0F)
    }
    */

    override fun createMenuItem(netView: CyNetworkView?, nodeView: View<CyNode>?): CyMenuItem {
        val nodeUri = netView?.model?.defaultNodeTable?.getRow(nodeView?.model?.suid)?.get("identifier uri", String::class.java) ?: throw Exception("Node URI not found in CyNetwork table. Are you sure you are querying a node created with this plugin?")

        var parentMenu = JMenu("BioGateway")

        parentMenu.add(createRelationSearchMenu("Fetch Relations from this node", netView, nodeUri, BGRelationDirection.FROM))
        parentMenu.addSeparator() // Weird bug that doesn't show even numbered menu items, so we're adding a separator (that won't be shown) as a workaround.
        parentMenu.add(createRelationSearchMenu("Fetch Relations to this node", netView, nodeUri, BGRelationDirection.TO))


        if (nodeUri.contains("ncbigene")) {
            parentMenu.addSeparator()
            parentMenu.add(createTFTGSearchMenu(netView, BGNodeType.GENE, nodeUri))
        } else if (nodeUri.contains("uniprot")) {
            parentMenu.addSeparator()
            parentMenu.add(createTFTGSearchMenu(netView, BGNodeType.PROTEIN, nodeUri))
        }

        return CyMenuItem(parentMenu, gravity)
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
                //serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
                val columnNames = arrayOf("from node","relation type", "to node")
                BGRelationSearchResultsController(serviceManager, returnData.relationsData, columnNames, network)
            }
            serviceManager.taskManager.execute(TaskIterator(query))
        }
        parentMenu.add(searchAllItem)

        // Will only create the menu if the config is loaded.
        for (relationType in serviceManager.cache.relationTypes.values) {
            val item = JMenuItem(relationType.description)

            item.addActionListener(ActionListener {
                val query = BGFindRelationForNodeQuery(serviceManager, relationType, nodeUri, direction)
                query.addCompletion {
                    val returnData = it as? BGReturnRelationsData
                    if (returnData != null) {
                        if (returnData.relationsData.size == 0) {
                            // TODO: Find a slightly better way of notifying the user of a lack of results.
                            throw Exception("No results found.")
                        }
                        val network = netView.model
                        serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
                    }
                }
                serviceManager.taskManager.execute(TaskIterator(query))
            })

            parentMenu.add(item)
        }
        return parentMenu
    }

    fun createTFTGSearchMenu(netView: CyNetworkView, nodeType: BGNodeType, nodeUri: String): JMenuItem {

        var menuItemText = when (nodeType) {
            BGNodeType.GENE -> "Find proteins regulating this gene"
            BGNodeType.PROTEIN -> "Find genes regulated by this protein"
        }

        val searchTFTG = JMenuItem(menuItemText)
        searchTFTG.addActionListener {
            val query = BGFindGraphRelationForNodeQuery(serviceManager, nodeType!!, nodeUri)
            query.addCompletion {
                val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
                if (returnData.relationsData.size == 0) throw Exception("No relations found.")
                val network = netView.model
                //serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
                val columnNames = when (nodeType) {
                    BGNodeType.PROTEIN -> arrayOf("protein", "relation", "gene")
                    BGNodeType.GENE -> arrayOf("gene", "relation", "protein")
                }
                BGRelationSearchResultsController(serviceManager, returnData.relationsData, columnNames, network)
            }

//                    val returnData = it as? BGReturnRelationsData ?: throw Exception("Invalid return data!")
//                    if (returnData.relationsData.size == 0) throw Exception("No relations found.")
//                    val network = netView.model
//                    serviceManager.server.networkBuilder.addRelationsToNetwork(network, returnData.relationsData)
            serviceManager.taskManager.execute(TaskIterator(query))
        }
        return searchTFTG
    }
}