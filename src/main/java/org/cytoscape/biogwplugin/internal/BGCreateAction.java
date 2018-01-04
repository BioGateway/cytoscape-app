package org.cytoscape.biogwplugin.internal;

import org.cytoscape.application.swing.AbstractCyAction;

import java.awt.event.ActionEvent;

/**
 * Created by sholmas on 23/03/2017.
 */

interface BGAction {
    void action(BGServiceManager serviceManager);
}

public class BGCreateAction extends AbstractCyAction {
    private final BGAction action;

    private final BGServiceManager serviceManager;

    public BGCreateAction(String name, String enableFor, BGServiceManager serviceManager, BGAction action) {
        super(name, serviceManager.getApplicationManager(), enableFor, serviceManager.getViewManager());
        setPreferredMenu("Apps.BioGateway");
        this.serviceManager = serviceManager;
        this.action = action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.action(serviceManager);
    }
}
