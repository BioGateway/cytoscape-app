package eu.biogateway.cytoscape.internal.server


import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.net.URL
import java.net.URLEncoder

import java.util.ArrayList


class SearchSuggestion(): Suggestion("search", "", null, null) {
    var searchString: String = ""

    override fun toString(): String {
        return searchString
    }
}

open class Suggestion(val _id: String, val prefLabel: String, val altLabel: String?, val definition: String?) {
    override fun toString(): String {
        var string = prefLabel
        if (altLabel != null) {
            string += ": " + altLabel
        }
        if (definition != null) {
            string += " - " + definition
        }
       return  string
    }
}

enum class EntityType(val typeName: String) {
    PROTEIN("protein"),
    GENE("gene"),
    GO_TERM("go-term"),
    ANY("any")
}

class BGDictEndpoint(internal var endpointUrl: String) {

    internal val client: HttpClient = HttpClients.createDefault()
    internal val gson = Gson()

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

    fun searchForPrefix(prefix: String, type: String, limit: Int): ArrayList<Suggestion> {

        if (prefix.length == 0) {
            return ArrayList()
        }

        val url = URL(endpointUrl + "prefixLabelSearch/?term=" + URLEncoder.encode(prefix, "UTF-8")+"&type="+type+"&limit="+limit).toURI()

        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)
        //val typeToken = object : TypeToken<List<Suggestion>>() {}.type
        //val otherList: List<Suggestion> = gson.fromJson(data, typeToken)

        val suggestions = ArrayList(gson.fromJson<List<Suggestion>>(data))

        for (suggestion in suggestions) {
            println(suggestion.prefLabel)
        }

        return ArrayList(suggestions)
    }

    fun getSuggestionForURI(uri: String): Suggestion? {
        val url = URL(endpointUrl + "fetch/?uri=" +URLEncoder.encode(uri, "UTF-8")).toURI()

        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)

        return try {
            gson.fromJson<Suggestion>(data)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun searchForLabel(term: String, type: String, limit: Int): ArrayList<Suggestion> {

        if (term.length == 0) {
            return ArrayList()
        }

        val url = URL(endpointUrl + "labelSearch/?term=" +URLEncoder.encode(term, "UTF-8")+"&type="+type+"&limit="+limit).toURI()

        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)
        //val typeToken = object : TypeToken<List<Suggestion>>() {}.type
        //val otherList: List<Suggestion> = gson.fromJson(data, typeToken)

        val suggestions = ArrayList(gson.fromJson<List<Suggestion>>(data))

        for (suggestion in suggestions) {
            //println(suggestion.prefLabel)
        }

        return ArrayList(suggestions)
    }
}
