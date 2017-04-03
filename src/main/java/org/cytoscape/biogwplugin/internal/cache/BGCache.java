package org.cytoscape.biogwplugin.internal.cache;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.query.BGFetchRelationTypesQuery;
import org.cytoscape.biogwplugin.internal.query.BGNode;
import org.cytoscape.biogwplugin.internal.query.BGQueryBuilderModel;
import org.cytoscape.biogwplugin.internal.query.QueryTemplate;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TaskIterator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by sholmas on 24/03/2017.
 */
public class BGCache {

    private HashMap<String, BGNode> nodeCache = new HashMap<>();
    private HashMap<String, String> relationTypes = new HashMap<>();
    private BGServiceManager serviceManager;
    private HashMap<String, QueryTemplate> queryTemplateHashMap;

    public BGCache(BGServiceManager serviceManager) {
        this.serviceManager = serviceManager;

        // Load initial data

        // Load the XML file

        // Load a list of Relation Types
        BGFetchRelationTypesQuery relationTypesQuery = new BGFetchRelationTypesQuery(serviceManager.SERVER_PATH);
        Runnable callback = () -> {
            this.relationTypes = relationTypesQuery.returnData;
        };
        relationTypesQuery.addCallback(callback);
        relationTypesQuery.run();

        loadXMLFileFromServer();

    }

    private void loadXMLFileFromServer() {
        try {
            URL queryFileUrl = new URL("https://dl.dropboxusercontent.com/u/32368359/BiogatewayQueries.xml");
            URLConnection connection = queryFileUrl.openConnection();
            InputStream is = connection.getInputStream();
            queryTemplateHashMap = BGQueryBuilderModel.parseXMLFile(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, QueryTemplate> getQueryTemplateHashMap() {
        if (queryTemplateHashMap != null) {
            return queryTemplateHashMap;
        } else {
            loadXMLFileFromServer();
            return queryTemplateHashMap;
        }
    }

    public BGNode getNodeWithURI(String uri) {

        BGNode node = nodeCache.get(uri);

        if (node == null) {
            System.out.println("BGCache Warning: Node "+uri+" not found. Attempting to fetch it from the current CyNetworks.");
            for (CyNetwork network : serviceManager.getNetworkManager().getNetworkSet()) {
                node = fetchNodeFromCyNetwork(uri, network);
                if (node != null) {
                    nodeCache.put(uri, node);
                    return node;
                }
            }
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

    public void debug_printCache() {
        for (String key : nodeCache.keySet()) {
            System.out.println(nodeCache.get(key).URI);
        }
    }

    private BGNode fetchNodeFromCyNetwork(String nodeUri, CyNetwork network) {
        CyTable table = network.getDefaultNodeTable();
        Set<CyNode> nodes = getNodesWithValue(network, table, "identifier uri", nodeUri);
        if (nodes.size() == 1) {
            // TODO: Make sure that the identifier uri is found!
            CyNode cyNode = nodes.iterator().next();
            String commonName = table.getRow(cyNode.getSUID()).get("name", String.class);
            BGNode bgNode = new BGNode(nodeUri);
            bgNode.commonName = commonName;
            return bgNode;
        } else if (nodes.size() > 1) {
            System.out.println("BGCache Warning: CyNetwork inconsistency: Found multiple nodes with same URI in the same network!");
            return null;
        } else {
            System.out.println("BGCache Warning: Did not find missing node in CyNetwork table either.");
            return null;
        }
    }

    private static Set<CyNode> getNodesWithValue(CyNetwork network, CyTable table, String columnName, Object value) {
        final Collection<CyRow> matchingRows = table.getMatchingRows(columnName, value);
        final Set<CyNode> nodes = new HashSet<CyNode>();
        final String primaryKeyColname = table.getPrimaryKey().getName();
        for (final CyRow row : matchingRows)
        {
            final Long nodeId = row.get(primaryKeyColname, Long.class);
            if (nodeId == null)
                continue;
            final CyNode node = network.getNode(nodeId);
            if (node == null)
                continue;
            nodes.add(node);
        }
        return nodes;
    }

    public HashMap<String, String> getRelationTypes() {
        return relationTypes;
    }
    public String getNameForRelationType(String relationTypeURI) {
        return relationTypes.get(relationTypeURI);
    }
}
