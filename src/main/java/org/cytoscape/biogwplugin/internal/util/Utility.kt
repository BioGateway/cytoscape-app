package org.cytoscape.biogwplugin.internal.util

import org.cytoscape.biogwplugin.internal.BGServiceManager
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
