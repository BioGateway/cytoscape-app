package org.cytoscape.biogwplugin.internal.old.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.old.parser.BGParser
import org.cytoscape.work.TaskMonitor

import java.io.IOException
import java.util.ArrayList

/**
 * Created by sholmas on 27/03/2017.
 */
class BGRelationsQuery(urlString: String, var nodeURI: String, var direction: BGRelationsQuery.Direction, var serviceManager: BGServiceManager) : BGQuery(urlString, "", ResultType.NODE_EDGE_NODE) {

    enum class Direction {
        PRE, POST
    }

    var returnData = ArrayList<BGRelation>()
        private set

    init {
        if (direction == Direction.POST) {
            queryString = POST_RELATIONS_SPARQL.replace("@identifierURI".toRegex(), nodeURI)
        } else {
            queryString = PRE_RELATIONS_SPARQL.replace("@identifierURI".toRegex(), nodeURI)
        }
    }

    override fun run() {
        val queryUrl = BGQuery.createBiopaxURL(this.urlString, this.queryString, BGQuery.RETURN_TYPE_TSV, BGQuery.BIOPAX_DEFAULT_OPTIONS)
        try {
            // Simpler way to get a String from an InputStream.
            val stream = queryUrl!!.openStream()
            //returnData = BGParser.parseNodes(stream, serviceManager.getCache());
            returnData = BGParser.parseRelations(stream, this, serviceManager.cache)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        var result = BGQueryResultRelationsData(ResultType.NODE_EDGE_NODE, this, ResultStatus.OK, returnData)

        this.runCallbacks(result)
    }


    @Throws(Exception::class)
    override fun run(taskMonitor: TaskMonitor) {
        this.run()
    }

    companion object {

        protected var POST_RELATIONS_SPARQL = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX term_id: <http://identifiers.org/uniprot/Q8L4H0> \n" +
                "SELECT ?term_name ?out_rel ?head_id ?object_name  \n" +
                "WHERE {  GRAPH  <cco> {{  \n" +
                "\t\tterm_id:       skos:prefLabel      ?term_name .  \n" +
                "\t\tterm_id:       ?out_rel   ?head_id .  \n" +
                "\t\t?head_id       skos:prefLabel      ?object_name .  }}}"

        protected var PRE_RELATIONS_SPARQL = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX term_id: <http://identifiers.org/uniprot/Q8L4H0> \n" +
                "SELECT ?term_name ?in_rel ?tail_id ?subject_name  \n" +
                "WHERE {  GRAPH  <cco> {{  \n" +
                "\t\tterm_id:       skos:prefLabel      ?term_name .  \n" +
                "\t\t?tail_id       ?in_rel    term_id: .  \n" +
                "\t\t?tail_id       skos:prefLabel      ?subject_name . }}}"
    }
}
