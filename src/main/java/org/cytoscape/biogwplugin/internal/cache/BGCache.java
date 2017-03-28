package org.cytoscape.biogwplugin.internal.cache;

import org.cytoscape.biogwplugin.internal.query.BGNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by sholmas on 24/03/2017.
 */
public class BGCache {

    private HashMap<String, BGNode> nodeCache = new HashMap<>();

    public BGNode getNodeWithURI(String uri) {

        BGNode node = nodeCache.get(uri);

        if (node == null) {
            System.out.println("Node not found! Should fetch it somewhere else... but that's not implemented yet.");
        }

        return node;
    }

    public ArrayList<BGNode> getNodesWithURIs(Collection<String> uris) {
        ArrayList<BGNode> nodes = new ArrayList();
        for (String uri : uris) {
            BGNode node = getNodeWithURI(uri);
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public void addNode(BGNode node) {
        nodeCache.put(node.URI, node);
    }
}
