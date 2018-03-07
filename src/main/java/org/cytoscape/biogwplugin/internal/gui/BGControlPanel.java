package org.cytoscape.biogwplugin.internal.gui;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.biogwplugin.internal.libs.JCheckBoxTree;
import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.model.BGRelationType;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;
import java.util.prefs.Preferences;

public class BGControlPanel extends JPanel implements CytoPanelComponent {



    private BGServiceManager serviceManager;
    private JPanel mainPanel;
    private JComboBox comboBox1;
    private JPanel treePanel;
    private JCheckBoxTree tree;


    public BGControlPanel(BGServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.add(mainPanel);

        setupTreePanel();
    }

    private void setSelectionForNode(DefaultMutableTreeNode node) {

    }


    public void setupTreePanel() {
        treePanel.removeAll();
        DefaultTreeModel model = serviceManager.getCache().getAvailableGraphs();
        tree = new JCheckBoxTree(model);
        //tree.expandRow(0);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        treePanel.add(tree);

        tree.addCheckChangeEventListener(new JCheckBoxTree.CheckChangeEventListener() {
            public void checkStateChanged(JCheckBoxTree.CheckChangeEvent event) {

                serviceManager.getDataModelController().setActiveRelationsForPaths(tree.getCheckedPaths());
            }
        });

        // Set the selected nodes. Currently selects all.
        // TODO: Store this in a Preferences object and retrieve it.
//        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
//        TreePath path = new TreePath(root.getPath());
//        tree.checkSubTree(path, true);
//
         serviceManager.getDataModelController().setSelectionFromPreferences(tree);


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
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Active Graphs"));
        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout(0, 0));
        panel2.add(treePanel, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}