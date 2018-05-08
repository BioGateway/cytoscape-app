package eu.biogateway.cytoscape.internal;

import org.cytoscape.application.swing.AbstractCyAction;

import java.awt.event.ActionEvent;

/**
 * Created by sholmas on 23/03/2017.
 */

interface BGAction {
    void action();
}

public class BGCreateAction extends AbstractCyAction {
    private final BGAction action;

    public BGCreateAction(String name, String enableFor, BGAction action) {
        super(name, BGServiceManager.INSTANCE.getApplicationManager(), enableFor, BGServiceManager.INSTANCE.getViewManager());
        setPreferredMenu("Apps.BioGateway");
        this.action = action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.action();
    }
}
