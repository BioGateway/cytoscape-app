package eu.biogateway.app.internal.gui;

import eu.biogateway.app.internal.gui.conversion.BGImportExportController;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class BGImportExportView {
    private final BGImportExportController controller;
    private JFrame mainFrame;
    private JPanel panel1;
    private JButton nextButton;
    private JPanel nodeImportsPanel;
    private JPanel nodeImportIdentifiersPanel;
    private JButton addIdentifierColumnButton;
    private JPanel convertColumnsPanel;
    private JButton addConvertedColumnsButton;
    private JButton addLineButton;
    private JComboBox sourceNetworkComboBox;

    public BGImportExportView(BGImportExportController controller) {
        this.controller = controller;
        mainFrame = new JFrame("BioGateway Column Converter");
        $$$setupUI$$$();
        mainFrame.setPreferredSize(new Dimension(700, 600));
        mainFrame.setContentPane(this.panel1);
        setupActions();
        mainFrame.pack();
        mainFrame.setVisible(true);
    }


    private void setupActions() {
        nextButton.addActionListener(e -> {
            //tabbedPane2.setSelectedIndex(1);
            controller.runImport();
        });
        addIdentifierColumnButton.addActionListener(e -> {
            controller.addIdentifierLine();
            nodeImportIdentifiersPanel.updateUI();
        });
        addConvertedColumnsButton.addActionListener(e -> {
            controller.runConvertColumns();
        });
    }

    public JPanel getNodeImportsPanel() {
        return nodeImportsPanel;
    }

    public JPanel getNodeImportIdentifiersPanel() {
        return nodeImportIdentifiersPanel;
    }

    public JPanel getConvertColumnsPanel() {
        return convertColumnsPanel;
    }

    public JComboBox getSourceNetworkComboBox() {
        return sourceNetworkComboBox;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        nodeImportsPanel = new JPanel();
        nodeImportsPanel.setLayout(new GridBagLayout());

        nodeImportIdentifiersPanel = new JPanel();
        nodeImportIdentifiersPanel.setLayout(new BoxLayout(nodeImportIdentifiersPanel, BoxLayout.Y_AXIS));

        convertColumnsPanel = new JPanel();
        convertColumnsPanel.setLayout(new GridBagLayout());
//        convertColumnsPanel.setLayout(new BoxLayout(convertColumnsPanel, BoxLayout.Y_AXIS));
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
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("Convert to BioGateway", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel2.add(panel3, BorderLayout.SOUTH);
        nextButton = new JButton();
        nextButton.setText("Convert");
        panel3.add(nextButton, BorderLayout.EAST);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel3.add(panel4, BorderLayout.WEST);
        addIdentifierColumnButton = new JButton();
        addIdentifierColumnButton.setText("Add identifier column");
        panel4.add(addIdentifierColumnButton);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel2.add(scrollPane1, BorderLayout.CENTER);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(null, "Additional columns", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane1.setViewportView(nodeImportsPanel);
        panel2.add(nodeImportIdentifiersPanel, BorderLayout.NORTH);
        nodeImportIdentifiersPanel.setBorder(BorderFactory.createTitledBorder(null, "Node Identifier columns", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        tabbedPane1.addTab("Convert Columns", panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.SOUTH);
        addConvertedColumnsButton = new JButton();
        addConvertedColumnsButton.setText("Add Converted Columns");
        panel6.add(addConvertedColumnsButton, BorderLayout.EAST);
        addLineButton = new JButton();
        addLineButton.setText("Add Line");
        panel6.add(addLineButton, BorderLayout.WEST);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        panel5.add(scrollPane2, BorderLayout.CENTER);
        scrollPane2.setBorder(BorderFactory.createTitledBorder(null, "Column Conversions", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane2.setViewportView(convertColumnsPanel);
        convertColumnsPanel.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(panel7, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Source Network:");
        panel7.add(label1);
        sourceNetworkComboBox = new JComboBox();
        panel7.add(sourceNetworkComboBox);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}