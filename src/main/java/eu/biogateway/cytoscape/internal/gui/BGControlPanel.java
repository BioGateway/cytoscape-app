package eu.biogateway.cytoscape.internal.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import eu.biogateway.cytoscape.internal.gui.conversion.BGImportExportController;
import eu.biogateway.cytoscape.internal.gui.multiquery.BGQueryConstraintPanel;
import eu.biogateway.cytoscape.internal.model.BGDataModelController;
import eu.biogateway.cytoscape.internal.model.BGQueryConstraint;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import eu.biogateway.cytoscape.internal.libs.JCheckBoxTree;
import eu.biogateway.cytoscape.internal.BGServiceManager;
import eu.biogateway.cytoscape.internal.util.Utility;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class BGControlPanel extends JPanel implements CytoPanelComponent2 {

    private JPanel mainPanel;
    private JPanel treePanel;
    private JButton resetBioGatewayStyleButton;
    private JButton reloadMetadataButton;
    private JButton queryBuilderButton;
    private JScrollPane scrollPane;
    private JPanel rootPanel;
    private JFormattedTextField fontSizeField;
    private JPanel constraintsPanel;
    private JPanel filtersPanel;
    private JCheckBoxTree tree;
    public BGQueryConstraintPanel queryConstraintPanel;


    public BGControlPanel() {
        $$$setupUI$$$();
        this.add(mainPanel);

        setupTreePanel();
        setUpActions();
        setupConstraintPanel();
    }

    private void setSelectionForNode(DefaultMutableTreeNode node) {

    }


    public void setupConstraintPanel() {
        this.constraintsPanel.removeAll();
        BGQueryConstraintPanel newPanel = new BGQueryConstraintPanel(BGServiceManager.INSTANCE.getConfig().getActiveConstraints());
        this.constraintsPanel.add(newPanel);
        this.queryConstraintPanel = newPanel;
        this.updateUI();

    }

    public void setupTreePanel() {
        treePanel.removeAll();
        DefaultTreeModel model = BGServiceManager.INSTANCE.getConfig().getConfigPanelTreeModel();
        tree = new JCheckBoxTree(model);
        //tree.expandRow(0);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        treePanel.add(tree);

        tree.addCheckChangeEventListener(event -> {
            BGServiceManager.INSTANCE.getDataModelController().setActiveNodesForPaths(tree.getCheckedPaths());
            TreePath path = (TreePath) event.getSource();
            if (path.getPathCount() > 1) {
                if (path.getPathComponent(1).toString() == "Query Constraints") {
                    BGServiceManager.INSTANCE.getControlPanel().setupConstraintPanel();
                }
            }
        });

        BGServiceManager.INSTANCE.getDataModelController().setSelectionFromPreferences(tree);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        TreePath path = new TreePath(root.getPath());
        tree.fireCheckChangeEvent(new JCheckBoxTree.CheckChangeEvent(path));
    }

    private void setUpActions() {
        resetBioGatewayStyleButton.addActionListener(e -> {
            Utility.INSTANCE.resetBioGatewayVisualStyle();
        });
        reloadMetadataButton.addActionListener(e -> {
            BGServiceManager.INSTANCE.getDataModelController().getNetworkBuilder().reloadMetadataForRelationsInCurrentNetwork();
            BGServiceManager.INSTANCE.getDataModelController().getNetworkBuilder().reloadMetadataForNodesInCurrentNetwork();

        });
        queryBuilderButton.addActionListener(e -> {
            new BGQueryBuilderController();
        });
    }

// CytoPanel implementations:

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public String getIdentifier() {
        return "biogatewayControlPanel";
    }

    @Override
    public String getTitle() {
        return "BioGateway";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        NumberFormat format = DecimalFormat.getInstance();
        format.setMaximumFractionDigits(1);
        format.setMinimumFractionDigits(1);
        format.setMaximumIntegerDigits(2);
        format.setRoundingMode(RoundingMode.HALF_UP);
        fontSizeField = new JFormattedTextField(format);
        fontSizeField.setText(Double.toString(BGServiceManager.INSTANCE.getConfig().getDefaultFontSize()));

        fontSizeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                BGServiceManager.INSTANCE.getConfig().setDefaultFontSize(Double.parseDouble(fontSizeField.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // Ignore if null.
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                BGServiceManager.INSTANCE.getConfig().setDefaultFontSize(Double.parseDouble(fontSizeField.getText()));

            }

            public void updateFontSize() {
                String text = fontSizeField.getText();
                Double size = Double.parseDouble(text);

            }
        });

    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(31);
        scrollPane.setVerticalScrollBarPolicy(20);
        rootPanel.add(scrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        scrollPane.setViewportView(mainPanel);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        mainPanel.add(panel1, BorderLayout.NORTH);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Active Properties"));
        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout(0, 0));
        panel1.add(treePanel, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 20, 0), -1, -1));
        mainPanel.add(panel2, BorderLayout.CENTER);
        resetBioGatewayStyleButton = new JButton();
        resetBioGatewayStyleButton.setText("Reset Layout Style");
        resetBioGatewayStyleButton.setToolTipText("Resets the BioGateway layout style to the default configuration encoded in the app.");
        panel2.add(resetBioGatewayStyleButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reloadMetadataButton = new JButton();
        reloadMetadataButton.setText("Reload Metadata");
        panel2.add(reloadMetadataButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        final JLabel label1 = new JLabel();
        label1.setText("Font Size: ");
        panel3.add(label1, BorderLayout.WEST);
        fontSizeField.setColumns(4);
        panel3.add(fontSizeField, BorderLayout.EAST);
        queryBuilderButton = new JButton();
        queryBuilderButton.setText("Query Builder");
        panel2.add(queryBuilderButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        mainPanel.add(panel4, BorderLayout.SOUTH);
        constraintsPanel = new JPanel();
        constraintsPanel.setLayout(new BorderLayout(0, 0));
        panel4.add(constraintsPanel, BorderLayout.NORTH);
        constraintsPanel.setBorder(BorderFactory.createTitledBorder("Query Constraints"));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}