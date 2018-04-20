package eu.biogateway.cytoscape.internal.server


import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import eu.biogateway.cytoscape.internal.util.Constants
import java.net.URL
import java.net.URLEncoder

import java.util.ArrayList


class SearchSuggestion(): BGSuggestion("search", "") {
    var searchString: String = ""

    override fun toString(): String {
        return searchString
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

    fun searchForPrefix(prefix: String, type: String, limit: Int): ArrayList<BGSuggestion> {

        if (prefix.length == 0) {
            return ArrayList()
        }

        val url = URL(endpointUrl + "prefixLabelSearch/?term=" + URLEncoder.encode(prefix, "UTF-8")+"&type="+type+"&limit="+limit).toURI()

        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)
        //val typeToken = object : TypeToken<List<BGSuggestion>>() {}.dataType
        //val otherList: List<BGSuggestion> = gson.fromJson(data, typeToken)

        if (statusCode in 200..399) {
            val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

//            for (suggestion in suggestions) {
//                println(suggestion.prefLabel)
//            }

            return ArrayList(suggestions)
        } else return ArrayList()
    }

    fun getSuggestionsForFieldValue(field: String, value: String, nodeType: String, limit: Int = 10): ArrayList<BGSuggestion> {
        val url = URL(endpointUrl + "findNodesWithFieldValue/"
                +"?field="+URLEncoder.encode(field, "UTF-8")
                +"&value="+URLEncoder.encode(value, "UTF-8")
                +"&type="+URLEncoder.encode(nodeType, "UTF-8")
                +"&limit="+limit).toURI()

        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)

        if (statusCode in 200..399) {
            val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

            return ArrayList(suggestions)
        } else return ArrayList()
    }

    fun getSuggestionForURI(uri: String): BGSuggestion? {
        val url = URL(endpointUrl + "fetch/?uri=" +URLEncoder.encode(uri, "UTF-8")).toURI()

        val httpGet = HttpGet(url)
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode
        val data = EntityUtils.toString(response.entity)

        return try {
            gson.fromJson<BGSuggestion>(data)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun searchForLabel(term: String, type: String, limit: Int): ArrayList<BGSuggestion> {

        if (term.length == 0) {
            return ArrayList()
        }

        val url = URL(endpointUrl + "labelSearch/?term=" +URLEncoder.encode(term, "UTF-8")+"&type="+type+"&limit="+limit).toURI()

        val httpGet = HttpGet(url)

        val queryStart = System.currentTimeMillis()
        val response = client.execute(httpGet)
        val statusCode = response.statusLine.statusCode

        if (Constants.PROFILING) {
            println("Label search time: "+(System.currentTimeMillis()-queryStart)+" ms. Status: "+statusCode+". Label: "+term)
        }
        val data = EntityUtils.toString(response.entity)
        //val typeToken = object : TypeToken<List<BGSuggestion>>() {}.dataType
        //val otherList: List<BGSuggestion> = gson.fromJson(data, typeToken)

        val suggestions = ArrayList(gson.fromJson<List<BGSuggestion>>(data))

        for (suggestion in suggestions) {
            //println(suggestion.prefLabel)
        }

        return ArrayList(suggestions)
    }
}
