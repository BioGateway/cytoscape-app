package org.cytoscape.biogwplugin.internal.old.util

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL



fun String.sanitizeParameter(): String {
    return this.replace("\"", "").replace(" ", "")
}



class Utility {
    // TODO: Identify a set of legal characters.
    /*
    fun sanitize(input: String): String {
        return input.replace("\"".toRegex(), "")
    }
    */

    companion object instance {
        fun sanitizeParameter(parameter: String): String {
            return parameter.sanitizeParameter()
        }

        fun validateURI(uri: String): Boolean {

            try {
                val url = URL(uri)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    return true
                } else {
                    return false
                }

            } catch (e: MalformedURLException) {
                e.printStackTrace()
                return false
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        }
    }
}
