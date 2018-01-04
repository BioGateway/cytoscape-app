@file:Suppress("LocalVariableName")

package org.cytoscape.biogwplugin.internal.util

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.getName
import org.cytoscape.biogwplugin.internal.parser.getUri
import org.cytoscape.group.CyGroup
import org.cytoscape.group.CyGroupManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.vizmap.VisualStyle
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.awt.EventQueue
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import javax.swing.JFrame
import org.cytoscape.model.subnetwork.CyRootNetwork
import org.cytoscape.model.subnetwork.CySubNetwork
import javax.swing.JComboBox
import javax.swing.JOptionPane


fun String.sanitizeParameter(): String {
    return this.replace("\"", "").trim()
}

class Tuple<X, Y>(val x: X, val y: Y)

object Utility {
    // TODO: Identify a set of legal characters.
    /*
    fun sanitize(input: String): String {
        return input.replace("\"".toRegex(), "")
    }
    */

    fun removeNodesNotInRelationSet(nodes: Collection<BGNode>, relations: Collection<BGRelation>): Collection<BGNode> {
        var allNodes = relations.map { it.toNode }.toHashSet().union(relations.map { it.fromNode }.toHashSet())
        return nodes.filter { allNodes.contains(it) }.toHashSet()
    }

    fun getNodeURIsForGroup(group: CyGroup): ArrayList<String> {
        val network = group.groupNetwork
        val cyNodes = group.nodeList
        var nodeURIs = ArrayList<String>()

        for (node in cyNodes) {
            val nodeURI = node.getUri(network)
            nodeURIs.add(nodeURI)
        }
        return nodeURIs
    }

    fun selectGroupPopup(serviceManager: BGServiceManager, network: CyNetwork): CyGroup? {
        val groups = Utility.findNodeGroupsInNetwork(serviceManager, network)
        val groupNames = groups.keys.toTypedArray()

        val comboBox = JComboBox(groupNames)
        val options = arrayOf("Search", "Cancel")
        val selection = JOptionPane.showOptionDialog(null, comboBox, "Select group to search to", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null)

        if (selection == 0) {
            val group = groups[comboBox.selectedItem]
            return group
        }
        return null
    }

    fun getGroupName(group: CyGroup): String {
        val rootNetwork = group.rootNetwork
        return rootNetwork.getRow(group.groupNode, CyRootNetwork.SHARED_ATTRS).get(CyRootNetwork.SHARED_NAME, String::class.java)
    }

    fun findNodeGroupsInNetwork(serviceManager: BGServiceManager,network: CyNetwork): HashMap<String, CyGroup> {
        val groups = serviceManager.adapter.cyGroupManager.getGroupSet(network)
        val groupMap = HashMap<String, CyGroup>()

        for (group in groups) {
            val groupName = getGroupName(group)
            groupMap[groupName] = group
        }
        return groupMap
    }

    fun createRelationTypeIdentifier(uri: String, graph: String): String {

        val graphName = graph.split("/").last()
        val identifier = graphName.replace("<", "").replace(">", "") + ":" + uri

        return identifier
    }

    fun getOrCreateBioGatewayVisualStyle(serviceManager: BGServiceManager): VisualStyle {
        val styles = serviceManager.adapter.visualMappingManager.allVisualStyles

        for (style in styles) {
            if (style.title.equals("BioGateway")) {
                return style
            }
        }
        val style = serviceManager.visualStyleBuilder.generateStyle()
        serviceManager.adapter.visualMappingManager.addVisualStyle(style)
        return style
    }

    fun reloadCurrentVisualStyleCurrentNetworkView(serviceManager: BGServiceManager) {
        val view = serviceManager.applicationManager.currentNetworkView
        val style = serviceManager.adapter.visualMappingManager.currentVisualStyle
        view?.let {
            serviceManager.eventHelper.flushPayloadEvents()
            style.apply(it)
        }
    }

    fun sanitizeParameter(parameter: String): String {
        return parameter.sanitizeParameter()
    }

    fun getJTextFieldHeight(): Int {
        val osName = System.getProperty("os.name")

        if (osName.startsWith("Mac")) return 20
        if (osName.startsWith("Windows")) return 20
        if (osName.startsWith("Linux")) return 25
        return 20
    }

    fun fightForFocus(frame: JFrame) {
        EventQueue.invokeLater {
            frame.toFront()
            frame.isAlwaysOnTop = true
            frame.isAlwaysOnTop = false
            frame.requestFocus()
        }
    }

    fun generateUniprotURI(uniprotId: String): String {
        return "http://identifiers.org/uniprot/"+uniprotId
    }

    fun generateGOTermURI(goTerm: String): String {
        val goTerm = goTerm.replace(":", "_")
        return "http://purl.obolibrary.org/obo/"+goTerm
    }

    fun encodeUrl(serviceManager: BGServiceManager, queryString: String): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"

        val queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(queryString, "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)

        return queryURL
    }


    fun validateURI(uri: String): Boolean {

        try {
            val url = URL(uri.replace("http://", "https://")) // Always use HTTPS!
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val responseCode = connection.responseCode

            return (responseCode == 200) or (responseCode == 302)

        } catch (e: MalformedURLException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun countMatchingRowsQuery(serviceManager: BGServiceManager, queryString: String): Int? {
        val url = Utility.encodeUrl(serviceManager, queryString)
        val stream = url?.openStream()
        if (stream != null) {
            val reader = BufferedReader(InputStreamReader(stream))
            val headerLine = reader.readLine()
            val countLine = reader.readLine()
            val count = countLine.toIntOrNull()
            return count
        }
        return null
    }
}
