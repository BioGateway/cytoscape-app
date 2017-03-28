package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import java.util.ArrayList;

/**
 * Created by sholmas on 28/03/2017.
 */
public class BGQueryFactory extends AbstractTaskFactory {

    enum QueryType {
            NODE_FETCH, NODE_SEARCH, RELATION_SEARCH
    }

    private String serverUrl;
    private String queryString;
    private BGServiceManager serviceManager;
    private BGRelationsQuery.BGRelationDirection direction;
    private QueryType type;
    private ArrayList<Runnable> callbacks;

    public BGQueryFactory(String serverUrl, String queryString, BGServiceManager serviceManager, QueryType type) {
        this.serverUrl = serverUrl;
        this.queryString = queryString;
        this.serviceManager = serviceManager;
        this.type = type;
    }

    public BGQueryFactory(String serverUrl, String queryString, BGRelationsQuery.BGRelationDirection direction, BGServiceManager serviceManager, QueryType type) {
        this.serverUrl = serverUrl;
        this.queryString = queryString;
        this.direction = direction;
        this.type = type;
    }

    @Override
    public TaskIterator createTaskIterator() {
        BGQuery query = null;
        switch (this.type) {
            case NODE_FETCH:
                query = new BGFetchNodeQuery(serverUrl, queryString, serviceManager);
                break;
            case NODE_SEARCH:
                query = new BGNodeSearchQuery(serverUrl, queryString, serviceManager);
                break;
            case RELATION_SEARCH:
                // TODO: Warning, this will crash if the wrong constructor is used!
                assert direction != null;
                query = new BGRelationsQuery(serverUrl, queryString, direction);
                break;
            default:
                break;
        }
        query.setCallbacks(callbacks);
        TaskIterator taskIterator = new TaskIterator(query);
        return taskIterator;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void setType(QueryType type) {
        this.type = type;
    }

    public void setDirection(BGRelationsQuery.BGRelationDirection direction) {
        this.direction = direction;
    }

    public void addCallback(Runnable callback) {
        this.callbacks.add(callback);
    }
}
