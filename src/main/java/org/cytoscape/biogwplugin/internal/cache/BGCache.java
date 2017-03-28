package org.cytoscape.biogwplugin.internal.cache;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.query.BGNode;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import java.util.*;

/**
 * Created by sholmas on 24/03/2017.
 */
public class BGCache {

    private HashMap<String, BGNode> nodeCache = new HashMap<>();
    private BGServiceManager serviceManager;

    public BGCache(BGServiceManager serviceManager) {
        this.serviceManager = serviceManager;
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
}
