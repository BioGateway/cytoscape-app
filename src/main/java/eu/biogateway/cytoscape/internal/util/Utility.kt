@file:Suppress("LocalVariableName")

package eu.biogateway.cytoscape.internal.util

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.model.BGRelation
import eu.biogateway.cytoscape.internal.parser.getUri
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelComponent2
import org.cytoscape.application.swing.CytoPanelName
import org.cytoscape.group.CyGroup
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.vizmap.VisualStyle
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
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JOptionPane


fun String.sanitizeParameter(): String {
    return this.replace("\"", "").trim()
}



fun Component.setFontSize(size: Double) {
    val font = this.font.deriveFont(size.toFloat())
    this.font = font
}

fun JComponent.setChildFontSize(size: Double) {
    for (component in components) {
        component.setFontSize(size)
    }
}

object Utility {


    fun scaleDimensionHeight(dimension: Dimension, height: Int): Dimension {
        if (height == 0) return dimension
        val scaleFactor = height / dimension.height
        return Dimension(dimension.width * scaleFactor, dimension.height * scaleFactor)
    }

    fun scaleDimensionWidth(dimension: Dimension, width: Int): Dimension {
        val scaleFactor = width / dimension.width
        return Dimension(dimension.width * scaleFactor, dimension.height * scaleFactor)
    }

    fun openBrowser(url: String) {

        val os = System.getProperty("os.name").toLowerCase()
        val rt = Runtime.getRuntime()

        try {

            if (os.indexOf("win") >= 0) {

                // this doesn't support showing urls in the form of "page.html#nameLink"
                rt.exec("rundll32 url.dll,FileProtocolHandler $url")

            } else if (os.indexOf("mac") >= 0) {

                rt.exec("open $url")

            } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {

                // Do a best guess on unix until we get a platform independent way
                // Build a list of browsers to try, in this order.
                val browsers = arrayOf("epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx")

                // Build a command string which looks like "browser1 "url" || browser2 "url" ||..."
                val cmd = StringBuffer()
                for (i in browsers.indices)
                    cmd.append((if (i == 0) "" else " || ") + browsers[i] + " \"" + url + "\" ")

                rt.exec(arrayOf("sh", "-c", cmd.toString()))

            } else {
                return
            }
        } catch (e: Exception) {
            return
        }

        return
    }

    fun selectBioGatewayControlPanelTab() {
        val panel = BGServiceManager.adapter?.cySwingApplication?.getCytoPanel(CytoPanelName.WEST) ?: return
        val index = panel.indexOfComponent("biogatewayControlPanel")
        panel.selectedIndex = index
    }

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

    fun selectGroupPopup(network: CyNetwork): CyGroup? {
        val groups = Utility.findNodeGroupsInNetwork(network)
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

    fun findNodeGroupsInNetwork(network: CyNetwork): HashMap<String, CyGroup> {
        val groupMap = HashMap<String, CyGroup>()

        val groups = BGServiceManager.adapter?.cyGroupManager?.getGroupSet(network) ?: return groupMap

        for (group in groups) {
            val groupName = getGroupName(group)
            groupMap[groupName] = group
        }
        return groupMap
    }

    fun createRelationTypeIdentifier(uri: String, graph: String): String {

        val graphName = graph.replace("http://rdf.biogateway.eu/graph/", "")
        val identifier = graphName.replace("<", "").replace(">", "") + "::" + uri

        return identifier
    }

    fun getOrCreateBioGatewayVisualStyle(): VisualStyle {
        val styles = BGServiceManager.adapter?.visualMappingManager?.allVisualStyles
        if (styles != null) {
            for (style in styles) {
                if (style.title.equals("BioGateway")) {
                    return style
                }
            }
        }
        val style = BGServiceManager.visualStyleBuilder.generateStyle()
        BGServiceManager.adapter?.visualMappingManager?.addVisualStyle(style)
        return style
    }

    fun resetBioGatewayVisualStyle() {
        // Making sure we make a copy of the array of references, so we can delete them without ConcurrentModificationException.
        val styles = BGServiceManager.adapter?.visualMappingManager?.allVisualStyles?.toTypedArray()?.copyOf()

        var updateStyle = false

        if (styles != null) {
            for (style in styles) {
                if (style.title.equals("BioGateway")) {
                    updateStyle = (BGServiceManager.visualMappingManager?.currentVisualStyle == style)
                    BGServiceManager.visualMappingManager?.removeVisualStyle(style)
                }
            }
        }
        val defaultStyle = BGServiceManager.visualStyleBuilder.generateStyle()
        BGServiceManager.adapter?.visualMappingManager?.addVisualStyle(defaultStyle)
        BGServiceManager.eventHelper?.flushPayloadEvents()
        if (updateStyle) {
            BGServiceManager.applicationManager?.currentNetworkView?.let {
                defaultStyle.apply(it)
            }
        }
    }

    fun reloadCurrentVisualStyleCurrentNetworkView() {
        val view = BGServiceManager.applicationManager?.currentNetworkView
        val style = BGServiceManager.adapter?.visualMappingManager?.currentVisualStyle
        view?.let {
            BGServiceManager.eventHelper?.flushPayloadEvents()
            style?.apply(it)
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
        return Constants.PROT_PREFIX+uniprotId
    }

    @Deprecated("Genes are now identified by UniProt accessions.")
    fun generateEntrezURI(entrezId: String): String {
        return Constants.GENE_PREFIX+entrezId
    }

    fun generateGOTermURI(goTerm: String): String {
        val goTerm = goTerm.replace(":", "_")
        return "http://purl.obolibrary.org/obo/"+goTerm
    }

    fun encodeUrl(queryString: String): URL? {
        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"

        val queryURL = URL(BGServiceManager.serverPath + "?query=" + URLEncoder.encode(queryString, "UTF-8") + "&format=" + RETURN_TYPE_TSV +"&" + BIOPAX_DEFAULT_OPTIONS)

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

    fun countMatchingRowsQuery(queryString: String): Int? {
        val url = Utility.encodeUrl(queryString)
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
