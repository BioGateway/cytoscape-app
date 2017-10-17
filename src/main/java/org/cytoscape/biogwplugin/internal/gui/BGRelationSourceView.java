package org.cytoscape.biogwplugin.internal.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;

public class BGRelationSourceView {
    private final JFrame mainFrame;
    private JPanel panel1;
    private JButton openPubmedURLButton;
    private JTable sourceInformationTable;


    public BGRelationSourceView(ActionListener listener, JComponent parentComponent) {

        mainFrame = new JFrame("Relation source data");
        mainFrame.setPreferredSize(new Dimension(400, 300));
        mainFrame.setContentPane(this.panel1);
        openPubmedURLButton.addActionListener(listener);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(parentComponent);
        mainFrame.setVisible(true);
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sourceInformationTable.setModel(tableModel);
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JButton getOpenPubmedURLButton() {
        return openPubmedURLButton;
    }

    public JTable getSourceInformationTable() {
        return sourceInformationTable;
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
        openPubmedURLButton = new JButton();
        openPubmedURLButton.setText("Open Pubmed URL");
        panel2.add(openPubmedURLButton, BorderLayout.EAST);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.CENTER);
        sourceInformationTable = new JTable();
        sourceInformationTable.setAutoCreateRowSorter(false);
        sourceInformationTable.setEnabled(true);
        panel3.add(sourceInformationTable, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}