package org.cytoscape.biogwplugin.internal.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class BGRelationSearchResultsView {

    public static String ACTION_IMPORT = "import nodes";
    public static String ACTION_IMPORT_TO_EXISTING = "import relations to exsisting nodes";


    private final ActionListener listener;
    private JFrame mainFrame;
    private JPanel panel1;
    private JButton importButton;
    private JTable resultTable;
    private JButton importToExisting;


    public BGRelationSearchResultsView(ActionListener listener) {
        this.listener = listener;
        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setContentPane(this.panel1);
        this.setupUI();
        frame.pack();
        frame.setVisible(true);
    }

    private void setupUI() {
        importButton.setActionCommand(ACTION_IMPORT);
        importButton.addActionListener(listener);
        importToExisting.setActionCommand(ACTION_IMPORT_TO_EXISTING);
        importToExisting.addActionListener(listener);
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JButton getImportButton() {
        return importButton;
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
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.SOUTH);
        importButton = new JButton();
        importButton.setText("Import Selected");
        panel2.add(importButton);
        importToExisting = new JButton();
        importToExisting.setText("Import relations between existing nodes");
        importToExisting.setToolTipText("Imports all the results which are between existing nodes in the network.");
        panel2.add(importToExisting);
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
