package org.cytoscape.biogwplugin.internal.parser;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.query.BGNode;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sholmas on 24/03/2017.
 */
public class BGNetworkBuilder {

    public static CyNetwork createNetworkFromBGNodes(ArrayList<BGNode> nodes, BGServiceManager serviceManager) {
        CyNetwork network = serviceManager.getNetworkFactory().createNetwork();
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTable.createColumn("identifier uri", String.class, false);


        for (BGNode node : nodes) {
            if (node.cyNode == null) {
                CyNode newNode = network.addNode();
                node.cyNode = newNode;
                nodeTable.getRow(newNode.getSUID()).set("identifier uri", node.URI);
                nodeTable.getRow(newNode.getSUID()).set("name", node.commonName);
            } else {
                // Need to figure out how to add one node to several networks. Maybe a 1-to-1 doesn't work?
            }
        }
        return network;
    }

    public static void addBGNodesToNetwork(CyNetwork network, ArrayList<BGNode> nodes, BGServiceManager serviceManager) {
        for (BGNode node : nodes) {
            Set<CyNode> matchingNodes = getNodesWithValue(network, network.getDefaultNodeTable(), "identifier uri", node.URI);
            if (matchingNodes.isEmpty()) {
                if (node.cyNode == null) {
                    CyNode newNode = network.addNode();
                    node.cyNode = newNode;
                }
                network.getDefaultNodeTable().getRow(node.cyNode.getSUID()).set("identifier uri", node.URI);
                network.getDefaultNodeTable().getRow(node.cyNode.getSUID()).set("name", node.commonName);
            }
        }
    }


    private static Set<CyNode> getNodesWithValue(final CyNetwork net, final CyTable table, final String colname, final Object value) {
        final Collection<CyRow> matchingRows = table.getMatchingRows(colname, value);
        final Set<CyNode> nodes = new HashSet<CyNode>();
        final String primaryKeyColname = table.getPrimaryKey().getName();
        for (final CyRow row : matchingRows)
        {
            final Long nodeId = row.get(primaryKeyColname, Long.class);
            if (nodeId == null)
                continue;
            final CyNode node = net.getNode(nodeId);
            if (node == null)
                continue;
            nodes.add(node);
        }
        return nodes;
    }
}
