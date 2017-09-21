package org.cytoscape.biogwplugin.internal.util

import java.awt.EventQueue
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
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
        fun sanitizeParameter(parameter: String): String {
            return parameter.sanitizeParameter()
        }

    fun fightForFocus(frame: JFrame) {
        EventQueue.invokeLater {
            frame.toFront()
            frame.isAlwaysOnTop = true
            frame.isAlwaysOnTop = false
            frame.requestFocus()
        }
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
}
