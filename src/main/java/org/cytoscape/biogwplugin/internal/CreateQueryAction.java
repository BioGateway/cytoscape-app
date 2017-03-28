package org.cytoscape.biogwplugin.internal;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.biogwplugin.internal.query.BGQueryBuilderUI;

import java.awt.event.ActionEvent;

/**
 * Created by sholmas on 23/03/2017.
 */
public class CreateQueryAction extends AbstractCyAction {

    private BGServiceManager serviceManager;

    public CreateQueryAction(String name, String enableFor, BGServiceManager serviceManager) {
        super(name, serviceManager.getApplicationManager(), enableFor, serviceManager.getViewManager());
        setPreferredMenu("Apps.Biogateway");
        this.serviceManager = serviceManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Action Pressed");

        BGQueryBuilderUI queryBuilderUI = new BGQueryBuilderUI(serviceManager);
    }
}
