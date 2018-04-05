package eu.biogateway.cytoscape.internal.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import eu.biogateway.cytoscape.internal.libs.JCheckBoxTree;
import eu.biogateway.cytoscape.internal.BGServiceManager;
import eu.biogateway.cytoscape.internal.util.Utility;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

public class BGControlPanel extends JPanel implements CytoPanelComponent {


    private BGServiceManager serviceManager;
    private JPanel mainPanel;
    private JComboBox comboBox1;
    private JPanel treePanel;
    private JButton resetBioGatewayStyleButton;
    private JButton reloadMetadataButton;
    private JCheckBoxTree tree;


    public BGControlPanel(BGServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.add(mainPanel);

        setupTreePanel();
        setUpActions();
    }

    private void setSelectionForNode(DefaultMutableTreeNode node) {

    }


    public void setupTreePanel() {
        treePanel.removeAll();
        DefaultTreeModel model = serviceManager.getCache().getConfigPanelTreeModel();
        tree = new JCheckBoxTree(model);
        //tree.expandRow(0);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        treePanel.add(tree);

        tree.addCheckChangeEventListener(event -> serviceManager.getDataModelController().setActiveNodesForPaths(tree.getCheckedPaths()));

        serviceManager.getDataModelController().setSelectionFromPreferences(tree);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        TreePath path = new TreePath(root.getPath());
        tree.fireCheckChangeEvent(new JCheckBoxTree.CheckChangeEvent(path));
    }

    private void setUpActions() {
        resetBioGatewayStyleButton.addActionListener(e -> {
            Utility.INSTANCE.resetBioGatewayVisualStyle(serviceManager);
        });
        reloadMetadataButton.addActionListener(e -> {
            serviceManager.getDataModelController().getNetworkBuilder().reloadMetadataForRelationsInCurrentNetwork();
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
    public String getTitle() {
        return "BioGateway";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(panel1, BorderLayout.NORTH);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Taxon", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, new Color(-16777216)));
        final JLabel label1 = new JLabel();
        label1.setText("Default Taxon: ");
        panel1.add(label1);
        comboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Homo Sapiens");
        defaultComboBoxModel1.addElement("Mus Musculus");
        defaultComboBoxModel1.addElement("Rattus Norwegicus");
        comboBox1.setModel(defaultComboBoxModel1);
        panel1.add(comboBox1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        mainPanel.add(panel2, BorderLayout.CENTER);
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Active Properties"));
        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout(0, 0));
        panel2.add(treePanel, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, BorderLayout.SOUTH);
        resetBioGatewayStyleButton = new JButton();
        resetBioGatewayStyleButton.setText("Reset Layout Style");
        resetBioGatewayStyleButton.setToolTipText("Resets the BioGateway layout style to the default configuration encoded in the app.");
        panel3.add(resetBioGatewayStyleButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reloadMetadataButton = new JButton();
        reloadMetadataButton.setText("Reload Metadata");
        panel3.add(reloadMetadataButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}