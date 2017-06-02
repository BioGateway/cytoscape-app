package org.cytoscape.biogwplugin.internal.old.query;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.old.parser.BGNetworkBuilder;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Created by sholmas on 28/03/2017.
 */
public class BGRelationSearchCMF implements CyNodeViewContextMenuFactory, ActionListener {

    private CyNetworkView networkView;
    private View<CyNode> nodeView;
    private BGServiceManager serviceManager;
    private BGRelationsQuery.Direction direction;
    private String menuDescription;

    public BGRelationSearchCMF(BGServiceManager serviceManager, BGRelationsQuery.Direction direction, String menuDescription) {
        this.serviceManager = serviceManager;
        this.direction = direction;
        this.menuDescription = menuDescription;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Get the node uri
        CyNode node = nodeView.getModel();
        CyNetwork network = networkView.getModel();
        String nodeUri = network.getDefaultNodeTable().getRow(node.getSUID()).get("identifier uri", String.class);

        // Create a BGRelationQuery
        BGRelationsQuery query = new BGRelationsQuery(serviceManager.SERVER_PATH, nodeUri, this.direction, serviceManager);
        ArrayList<BGNode> newNodes = new ArrayList<>();

        // Create a completion block for the query, where the results are added to the current network.
        Runnable callback = () -> {
            for (BGRelation relation : query.getReturnData()) {
                switch (this.direction) {
                    case PRE:
                        newNodes.add(relation.fromNode);
                        break;
                    case POST:
                        newNodes.add(relation.toNode);
                        break;
                }
            }
            BGNetworkBuilder.INSTANCE.addBGNodesToNetwork(network, newNodes, serviceManager);
            BGNetworkBuilder.INSTANCE.addBGRelationsToNetwork(network, query.getReturnData(), serviceManager);
            BGNetworkBuilder.INSTANCE.destroyAndRecreateNetworkView(network, serviceManager);
        };
        //query.addCallback(callback);

        // Run the BGRelationQuery
        serviceManager.getTaskManager().execute(new TaskIterator(query));
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView, View<CyNode> nodeView) {
        this.networkView = netView;
        this.nodeView = nodeView;
        JMenuItem item = new JMenuItem(menuDescription);
        item.addActionListener(this);
        return new CyMenuItem(item, 0);
    }
}