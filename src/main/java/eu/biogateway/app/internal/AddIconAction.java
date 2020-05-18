package eu.biogateway.app.internal;

import eu.biogateway.app.internal.gui.BGQueryBuilderController;
import org.cytoscape.application.swing.AbstractCyAction;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddIconAction extends AbstractCyAction {

    public AddIconAction() {
        super("BioGateway");
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("biogateway_icon.png"));
        putValue(LARGE_ICON_KEY, icon);
    }

    @Override
    public float getToolbarGravity() {
        return 100F;
    }

    @Override
    public boolean isInToolBar() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        BGQueryBuilderController queryBuilderController = new BGQueryBuilderController();
    }
}
