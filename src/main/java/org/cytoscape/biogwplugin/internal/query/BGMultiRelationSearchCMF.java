package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sholmas on 29/03/2017.
 */
public class BGMultiRelationSearchCMF implements CyNetworkViewContextMenuFactory, ActionListener {

    private CyNetworkView networkView;
    private BGServiceManager serviceManager;
    private BGRelationsQuery.Direction direction;
    private String menuDescription;

    public BGMultiRelationSearchCMF(BGServiceManager serviceManager, BGRelationsQuery.Direction direction, String menuDescription) {
        this.serviceManager = serviceManager;
        this.direction = direction;
        this.menuDescription = menuDescription;
    }

    @Override
    public CyMenuItem createMenuItem(CyNetworkView netView) {
        this.networkView = netView;
        JMenuItem item = new JMenuItem(menuDescription);
        item.addActionListener(this);
        return new CyMenuItem(item, 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CyNetwork network = networkView.getModel();
        // Get the node uris:
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, "selected", true);

        ArrayList<BGNode> newNodes = new ArrayList<>();

        // Create an OR-statement based on all the URIs.
        String nodesUri = "";
        for (CyNode node : nodes) {
            String nodeUri = network.getDefaultNodeTable().getRow(node.getSUID()).get("identifier uri", String.class);
            nodesUri += " | "+nodeUri;
        }
        System.out.println("Nodes URI: "+nodesUri);



    }
}
