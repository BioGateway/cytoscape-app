package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.parser.BGNetworkBuilder;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import java.util.ArrayList;

/**
 * Created by sholmas on 29/03/2017.
 */
public class BGMultiRelationsQuery extends AbstractTask {

    private String urlString;
    private ArrayList<String> nodeURIs;
    private BGRelationsQuery.Direction direction;
    private BGServiceManager serviceManager;
    private ArrayList<BGRelation> resultData = new ArrayList<BGRelation>();
    private Runnable callback;

    public BGMultiRelationsQuery(String urlString, ArrayList<String> nodeURIs, BGRelationsQuery.Direction direction, BGServiceManager serviceManager) {
        this.direction = direction;
        this.urlString = urlString;
        this.nodeURIs = nodeURIs;
        this.serviceManager = serviceManager;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        ArrayList<BGRelationsQuery> queries = new ArrayList<>();

        // TODO: Figure out this concurrency-thing...
        for (String nodeURI : nodeURIs) {
            BGRelationsQuery query = new BGRelationsQuery(urlString, nodeURI, direction, serviceManager);
            queries.add(query);

        }
        }
    private void parseRelations(ArrayList<ArrayList<BGRelation>> results) {
        for (ArrayList<BGRelation> relations : results) {
            for (BGRelation relation : relations) {
                resultData.add(relation);
            }
        }
        callback.run();
    }

    public ArrayList<BGRelation> getResultData() {
        return resultData;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }
}

