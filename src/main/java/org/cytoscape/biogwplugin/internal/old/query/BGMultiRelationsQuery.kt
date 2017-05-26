package org.cytoscape.biogwplugin.internal.old.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import java.util.ArrayList

/**
 * Created by sholmas on 29/03/2017.
 */
class BGMultiRelationsQuery(private val urlString: String, private val nodeURIs: ArrayList<String>, private val direction: BGRelationsQuery.Direction, private val serviceManager: BGServiceManager) : AbstractTask() {
    val resultData = ArrayList<BGRelation>()
    private var callback: Runnable? = null

    @Throws(Exception::class)
    override fun run(taskMonitor: TaskMonitor) {
        val queries = ArrayList<BGRelationsQuery>()

        // TODO: Figure out this concurrency-thing...
        for (nodeURI in nodeURIs) {
            val query = BGRelationsQuery(urlString, nodeURI, direction, serviceManager)
            queries.add(query)

        }
    }

    private fun parseRelations(results: ArrayList<ArrayList<BGRelation>>) {
        for (relations in results) {
            for (relation in relations) {
                resultData.add(relation)
            }
        }
        callback!!.run()
    }

    fun setCallback(callback: Runnable) {
        this.callback = callback
    }
}

