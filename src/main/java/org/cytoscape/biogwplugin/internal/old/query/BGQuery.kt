package org.cytoscape.biogwplugin.internal.old.query

import org.cytoscape.work.AbstractTask

import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.ArrayList

// TODO: This class should implement 'Runnable', I think.
abstract class BGQuery(var urlString: String, var queryString: String, var resultType: ResultType) : AbstractTask(), Runnable {
    var callbacks = ArrayList<(BGQueryResult) -> Unit>()


    fun addCallback(callback: (BGQueryResult) -> Unit) {
        this.callbacks.add(callback)
    }

    fun runCallbacks(result: BGQueryResult) {
        for (callback in callbacks) {
            callback(result)
        }
    }

    companion object {

        val RETURN_TYPE_TSV = "text/tab-separated-values"
        val BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on"

        fun createBiopaxURL(serverPath: String, queryData: String, returnType: String, options: String): URL? {
            val queryURL: URL
            try {
                queryURL = URL(serverPath + "?query=" + URLEncoder.encode(queryData, "UTF-8") + "&format=" + URLEncoder.encode(returnType, "UTF-8") + "&" + options)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                return null
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return null
            }

            return queryURL
        }
    }
}
