package eu.biogateway.cytoscape.internal.server


import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGConfig
import eu.biogateway.cytoscape.internal.model.BGSearchType
import eu.biogateway.cytoscape.internal.model.BGTaxon
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Utility
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import java.net.URL
import java.net.URLEncoder

import java.util.ArrayList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.JOptionPane


class SearchSuggestion(): BGSuggestion("search", "") {
    var searchString: String = ""

    override fun toString(): String {
        return searchString
    }
}

class BGDictEndpoint(internal var endpointUrl: String) {

    internal val client: HttpClient = HttpClients.createDefault()
    internal val gson = Gson()

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)


    fun getSuggestionsForQueryPath(path: String): JsonObject? {

        val url = URL(endpointUrl + path).toURI()
        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)
        if (statusCode in 200..399) {
            val element = JsonParser().parse(data)
            val json = element.asJsonObject
            return json
        } else return null
    }

    fun searchForPrefix(prefix: String, type: String, limit: Int): ArrayList<BGSuggestion> {
        val taxa = if (BGServiceManager.config.activeTaxa.size != BGServiceManager.config.availableTaxa.size) BGServiceManager.config.activeTaxa else null
        val future = searchForPrefixAsync(prefix, type, limit, taxa = taxa)

        try {
            return future.get(5000, TimeUnit.MILLISECONDS)
        } catch (timeout: TimeoutException) {
            showConnectionErrorDialog()
            return ArrayList()
        }

    }

    fun searchForPrefixAsync(prefix: String, type: String, limit: Int, taxa: Collection<BGTaxon>? = null): CompletableFuture<ArrayList<BGSuggestion>> {
        val future = CompletableFuture<ArrayList<BGSuggestion>>()
        if (prefix.length == 0) {
            future.complete(ArrayList())
        }

        Executors.newCachedThreadPool().submit {
            val url = URL(endpointUrl + "prefixLabelSearch").toURI()
            var taxaJson = ""
            taxa?.let {
                if (it.size > 0) {
                    val terms = it.joinToString { bgTaxon -> "\"${bgTaxon.uri}\"" }
                    taxaJson = ", \"taxa\": [$terms]"
                }
            }
            val json = "{ \"term\": \"${prefix}\", \"type\": \"${type}\", \"limit\": \"${limit}\"$taxaJson}"
            val httpPost = HttpPost(url)
            httpPost.entity = StringEntity(json)
            httpPost.addHeader("Content-Type", "application/json");
            val response = client.execute(httpPost)
            val statusCode = response.statusLine.statusCode
            val data = EntityUtils.toString(response.entity)

            if (statusCode in 200..399) {
                val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

                future.complete(ArrayList(suggestions))
            } else {
                future.complete(ArrayList())
            }
        }
        return future
    }

    fun getSuggestionsForFieldValue(field: String, value: String, nodeType: String, limit: Int = 10): ArrayList<BGSuggestion> {
        val future = CompletableFuture<ArrayList<BGSuggestion>>()

        Executors.newCachedThreadPool().submit {
            val url = URL(endpointUrl + "findNodesWithFieldValue/"
                    + "?field=" + URLEncoder.encode(field, "UTF-8")
                    + "&value=" + URLEncoder.encode(value, "UTF-8")
                    + "&type=" + URLEncoder.encode(nodeType, "UTF-8")
                    + "&limit=" + limit).toURI()

            val httpGet = HttpGet(url)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            val data = EntityUtils.toString(response.entity)

            if (statusCode in 200..399) {
                val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

                future.complete(ArrayList(suggestions))
            } else {
                future.complete(ArrayList())
            }
        }


        try {
            return future.get(5000, TimeUnit.MILLISECONDS)
        } catch (timeout: TimeoutException) {
            showConnectionErrorDialog()
            return ArrayList()
        }
    }

    fun getSuggestionForURI(uri: String): BGSuggestion? {
//        val url = URL(endpointUrl + "fetch/?uri=" +URLEncoder.encode(uri, "UTF-8")).toURI()
//
//        val httpGet = HttpGet(url)
//        val response = client.execute(httpGet)
//        val statusCode = response.statusLine.statusCode
//        val data = EntityUtils.toString(response.entity)
//
//        return try {
//            gson.fromJson<BGSuggestion>(data)
//        } catch (e: JsonSyntaxException) {
//            null
//        }

        val future = getSuggestionForURIAsync(uri)

        try {
            return future.get(5000, TimeUnit.MILLISECONDS)
        } catch (timeout: TimeoutException) {
            showConnectionErrorDialog()
            return null
        }
    }

    fun getSuggestionForURIAsync(uri: String): CompletableFuture<BGSuggestion?> {
        val future = CompletableFuture<BGSuggestion?>()

        Executors.newCachedThreadPool().submit {
            val url = URL(endpointUrl + "fetch/?uri=" + URLEncoder.encode(uri, "UTF-8")).toURI()

            val httpGet = HttpGet(url)
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode
            val data = EntityUtils.toString(response.entity)

            try {
                future.complete(gson.fromJson<BGSuggestion>(data))
            } catch (e: JsonSyntaxException) {
                future.complete(null)
            }
        }
        return future
    }

    fun findNodesForSearchType(nodeList: Collection<String>, searchType: BGSearchType): ArrayList<BGSuggestion> {
        // TODO: Finish this for Bulk Import!

        val future = CompletableFuture<ArrayList<BGSuggestion>>()

        Executors.newCachedThreadPool().submit {
            val url = endpointUrl + searchType.restPath
            val prefix = searchType.prefix ?: ""

            var taxaString = ""
            if (BGServiceManager.config.activeTaxaNotNullOrAll) {
                taxaString = "\"taxa\": [" + BGServiceManager.config.activeTaxa.map { "\"${it.uri}\"" }.reduce { list, taxon -> "$list, $taxon" } + "], "
            }
            val terms = nodeList.map { "\"$prefix$it\"" }.reduce { list, node -> "$list, $node" }
            val parameterString = ""
            val json = "{ \"returnType\": \"${searchType.returnType}\", \"nodeType\": \"${searchType.nodeType.id}\",$taxaString \"values\": [$terms]$parameterString}"
            val httpPost = HttpPost(url)
            httpPost.entity = StringEntity(json)
            httpPost.addHeader("Content-Type", "application/json");
            val response = client.execute(httpPost)
            val data = EntityUtils.toString(response.entity)
            val statusCode = response.statusLine.statusCode

            if (statusCode in 200..399) {
                val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

                future.complete(ArrayList(suggestions))
            }

            future.complete(ArrayList())
        }

        try {
            return future.get(5000, TimeUnit.MILLISECONDS)
        } catch (timeout: TimeoutException) {
            showConnectionErrorDialog()
            return ArrayList()
        }
    }

    fun getGenesWithSymbols(symbols: Collection<String>): ArrayList<BGSuggestion> {
        return ArrayList()
    }

    fun searchForLabel(term: String, type: String, limit: Int): ArrayList<BGSuggestion> {
        val future = CompletableFuture<ArrayList<BGSuggestion>>()
        if (term.length == 0) {
            future.complete(ArrayList())
        }

        Executors.newCachedThreadPool().submit {
            val url = URL(endpointUrl + "labelSearch/?term=" + URLEncoder.encode(term, "UTF-8") + "&type=" + type + "&limit=" + limit).toURI()

            val httpGet = HttpGet(url)

            val queryStart = System.currentTimeMillis()
            val response = client.execute(httpGet)
            val statusCode = response.statusLine.statusCode

            if (Constants.PROFILING) {
                println("Label search time: " + (System.currentTimeMillis() - queryStart) + " ms. Status: " + statusCode + ". Label: " + term)
            }
            val data = EntityUtils.toString(response.entity)
            //val typeToken = object : TypeToken<List<BGSuggestion>>() {}.dataType
            //val otherList: List<BGSuggestion> = gson.fromJson(data, typeToken)

            val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

            for (suggestion in suggestions) {
                //println(suggestion.prefLabel)
            }

            future.complete(ArrayList(suggestions))
        }

        try {
            return future.get(5000, TimeUnit.MILLISECONDS)
        } catch (timeout: TimeoutException) {
            showConnectionErrorDialog()
            return ArrayList()
        }
    }

    private fun showConnectionErrorDialog() {
        val port = endpointUrl.split(":").last().replace("/", "")
        val message = "Unable to connect to BioGateway server hosting entity metadata. Ensure that you are connected to the internet, \nand that your network allows outgoing connections on port 80." +
                "\n\nSee www.biogateway.eu/troubleshooting for more information."
        JOptionPane.showMessageDialog(null, message, "Unable to connect to BioGateway server", JOptionPane.WARNING_MESSAGE)

    }
}
