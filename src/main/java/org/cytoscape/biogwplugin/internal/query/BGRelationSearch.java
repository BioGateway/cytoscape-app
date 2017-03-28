package org.cytoscape.biogwplugin.internal.query;

/**
 * Created by sholmas on 27/03/2017.
 */
public class BGRelationSearch implements Runnable {

    private final static String SERVER_PATH = "http://www.semantic-systems-biology.org/biogateway/endpoint";


    BGRelationsQuery preQuery;
    BGRelationsQuery postQuery;

    public BGRelationSearch(String nodeURI) {
        preQuery = new BGRelationsQuery(SERVER_PATH, nodeURI, BGRelationsQuery.BGRelationDirection.PRE);
        postQuery = new BGRelationsQuery(SERVER_PATH, nodeURI, BGRelationsQuery.BGRelationDirection.POST);
    }

    @Override
    public void run() {
        Runnable callback = () -> {

        };
        preQuery.addCallback(callback);
        postQuery.addCallback(callback);
        preQuery.run();
        postQuery.run();
    }
}
