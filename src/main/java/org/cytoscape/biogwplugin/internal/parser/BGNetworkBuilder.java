package org.cytoscape.biogwplugin.internal.parser;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.query.BGNode;
import org.cytoscape.biogwplugin.internal.query.BGRelation;
import org.cytoscape.model.*;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.create.CreateNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import java.util.*;

/**
 * Created by sholmas on 24/03/2017.
 */
public class BGNetworkBuilder {

    public static CyNetwork createNetworkFromBGNodes(ArrayList<BGNode> nodes, BGServiceManager serviceManager) {
        CyNetwork network = serviceManager.getNetworkFactory().createNetwork();
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTable.createColumn("identifier uri", String.class, false);


        for (BGNode node : nodes) {
            CyNode newNode = network.addNode();
            nodeTable.getRow(newNode.getSUID()).set("identifier uri", node.URI);
            nodeTable.getRow(newNode.getSUID()).set("name", node.commonName);
        }

        return network;
    }

    public static void addBGNodesToNetwork(CyNetwork network, ArrayList<BGNode> nodes, BGServiceManager serviceManager) {
        for (BGNode node : nodes) {
            Set<CyNode> matchingNodes = getNodesWithValue(network, network.getDefaultNodeTable(), "identifier uri", node.URI);
            if (matchingNodes.isEmpty()) {
                CyNode newNode = network.addNode();
                network.getDefaultNodeTable().getRow(newNode.getSUID()).set("identifier uri", node.URI);
                network.getDefaultNodeTable().getRow(newNode.getSUID()).set("name", node.commonName);
            }
        }
    }

    public static void addBGRelationsToNetwork(CyNetwork network, ArrayList<BGRelation> relations, BGServiceManager serviceManager) {
        Set<BGNode> toNodes = new HashSet<>();
        Set<BGNode> fromNodes = new HashSet<>();
        CyTable nodeTable = network.getDefaultNodeTable();
        CyTable edgeTable = network.getDefaultEdgeTable();
        if (edgeTable.getColumn("identifier uri") == null) edgeTable.createColumn("identifier uri", String.class, false);

        // Fetch all nodes involved in the relations:
        for (BGRelation relation : relations) {
            fromNodes.add(relation.fromNode);
            toNodes.add(relation.toNode);
        }

        // TODO: Add the missing nodes to the network. For now we assume it's being done elsewhere.

        for (BGRelation relation : relations) {
            CyNode fromNode = getNodeForUri(relation.fromNode.URI, network, nodeTable);
            CyNode toNode = getNodeForUri(relation.toNode.URI, network, nodeTable);

            // TODO: Create unique edge identifiers to assure that duplicate edges are not added. See old KT-App code for details.
            CyEdge edge = network.addEdge(fromNode, toNode, true);
            edgeTable.getRow(edge.getSUID()).set("identifier uri", relation.URI);
            edgeTable.getRow(edge.getSUID()).set("name", serviceManager.getCache().getNameForRelationType(relation.URI));
        }
    }

    private static CyNode getNodeForUri(String nodeUri, CyNetwork network, CyTable table) {

        Collection<CyNode> nodes = getNodesWithValue(network, table, "identifier uri", nodeUri);
        // Node uri should be unique, so there should be no more than one match.
        if (nodes.size() == 1) {
            return nodes.iterator().next(); // Just return the first one, it's just one anyway.
        }
        return null;
    }

    // TODO: A similar method is already defined in the BGCache class. Consolidate them?
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

    public static void destroyAndRecreateNetworkView(CyNetwork network, BGServiceManager serviceManager) {
        // Destroy all views.
        for (CyNetworkView view : serviceManager.getViewManager().getNetworkViews(network)) {
            serviceManager.getViewManager().destroyNetworkView(view);
        }
        CreateNetworkViewTaskFactory createNetworkViewTaskFactory = serviceManager.getCreateNetworkViewTaskFactory();
        TaskIterator taskIterator = createNetworkViewTaskFactory.createTaskIterator(Collections.singleton(network));
        serviceManager.getTaskManager().execute(taskIterator);

    }

    public static void createNetworkView(CyNetwork network, BGServiceManager serviceManager) {
        CreateNetworkViewTaskFactory viewTaskFactory = serviceManager.getCreateNetworkViewTaskFactory();
        TaskIterator taskIterator = viewTaskFactory.createTaskIterator(Collections.singleton(network));
        serviceManager.getTaskManager().execute(taskIterator);
    }
}
