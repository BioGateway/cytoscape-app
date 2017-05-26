package org.cytoscape.biogwplugin.internal.old.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator

import java.util.ArrayList

/**
 * Created by sholmas on 28/03/2017.
 */
class BGQueryFactory(var serverUrl: String, var queryString: String, var serviceManager: BGServiceManager, var type: QueryType) : AbstractTaskFactory() {

    enum class QueryType {
        NODE_FETCH, NODE_SEARCH, RELATION_SEARCH, NOT_INITIALIZED
    }

    public constructor(serverUrl: String, queryString: String, serviceManager: BGServiceManager, type: QueryType, direction: BGRelationsQuery.Direction) : this(serverUrl, queryString, serviceManager, type) {
        this.direction = direction
    }

    var direction: BGRelationsQuery.Direction? = null

    private val callbacks = ArrayList<(BGQueryResult) -> Unit>()

    override fun createTaskIterator(): TaskIterator {
        val query: BGQuery = when (this.type) {
            BGQueryFactory.QueryType.NODE_FETCH ->  BGFetchNodeQuery(serverUrl, queryString, serviceManager)
            BGQueryFactory.QueryType.NODE_SEARCH -> BGNodeSearchQuery(serverUrl, queryString, serviceManager)
            BGQueryFactory.QueryType.RELATION_SEARCH -> {
                // TODO: Warning, this will crash if the wrong constructor is used!
                if(direction == null) throw Exception("Cannot run relation search without defining direction.")
                BGRelationsQuery(serverUrl, queryString, direction!!, serviceManager!!)
            }
            BGQueryFactory.QueryType.NOT_INITIALIZED -> throw Exception("Cannot run an un-initialized query.")
        }
        query.callbacks = callbacks
        val taskIterator = TaskIterator(query)
        return taskIterator
    }

    fun addCallback(callback: (BGQueryResult) -> Unit) {
        this.callbacks.add(callback)
    }
}
