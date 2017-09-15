package org.cytoscape.biogwplugin.internal.gui;

import org.cytoscape.biogwplugin.internal.model.BGNodeType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class BGURILookupView {

    private final ActionListener listener;
    private final JFrame mainFrame;

    private JPanel panel1;
    private JTextField searchField;
    private JButton searchButton;
    private JCheckBox regexCheckBox;
    private JTable resultTable;
    private JButton useURIButton;
    private JComboBox nodeTypeComboBox;

    public static final String ACTION_SELECT_NODE = "Choose selected node";
    public static final String ACTION_SEARCH = "Search for URIs";


    public BGURILookupView(ActionListener listener) {
        this.listener = listener;
        mainFrame = new JFrame("URI Lookup");
        mainFrame.setPreferredSize(new Dimension(600, 400));
        mainFrame.setContentPane(this.panel1);
        setupUI();
        mainFrame.pack();
        mainFrame.setVisible(true);

    }

    private void setupUI() {
        nodeTypeComboBox.setModel(new DefaultComboBoxModel(BGNodeType.values()));
        searchButton.addActionListener(listener);
        searchButton.setActionCommand(ACTION_SEARCH);
        useURIButton.setActionCommand(ACTION_SELECT_NODE);
        useURIButton.addActionListener(listener);
    }


    public JCheckBox getRegexCheckBox() {
        return regexCheckBox;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    public JComboBox getNodeTypeComboBox() {
        return nodeTypeComboBox;
    }

    public JTable getResultTable() {
        return resultTable;
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
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.NORTH);
        searchField = new JTextField();
        panel2.add(searchField, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        panel2.add(panel3, BorderLayout.EAST);
        regexCheckBox = new JCheckBox();
        regexCheckBox.setText("Regex");
        panel3.add(regexCheckBox);
        nodeTypeComboBox = new JComboBox();
        panel3.add(nodeTypeComboBox);
        searchButton = new JButton();
        searchButton.setText("Search");
        panel3.add(searchButton);
        final JLabel label1 = new JLabel();
        label1.setText("Node name:");
        panel2.add(label1, BorderLayout.WEST);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel4, BorderLayout.SOUTH);
        useURIButton = new JButton();
        useURIButton.setText("Use URI");
        panel4.add(useURIButton);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, BorderLayout.CENTER);
        resultTable = new JTable();
        scrollPane1.setViewportView(resultTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
