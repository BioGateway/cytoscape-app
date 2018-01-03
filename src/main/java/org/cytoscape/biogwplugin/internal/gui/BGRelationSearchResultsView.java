package org.cytoscape.biogwplugin.internal.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class BGRelationSearchResultsView {

    public static String ACTION_IMPORT = "import nodes";
    public static String ACTION_IMPORT_BETWEEN_EXISTING = "import relations to exsisting nodes";
    public static String ACTION_FILTER_RESULTS = "filter results";

    private final ActionListener listener;
    private JFrame mainFrame;
    private JPanel panel1;
    private JButton importButton;
    private JTable resultTable;
    private JButton importRelationsBetweenExistingButton;
    private JTextField filterTextField;
    private JButton selectRelationsLeadingToButton;
    private JCheckBox filterSelectedCheckBox;
    private TableRowSorter<TableModel> sorter;
    private BGRelationResultViewTooltipDataSource tooltipDataSource;

    public BGRelationSearchResultsView(ActionListener listener, BGRelationResultViewTooltipDataSource tooltipDataSource) {
        this.listener = listener;
        this.tooltipDataSource = tooltipDataSource;
        JFrame frame = new JFrame("Biogateway Query Results");
        this.mainFrame = frame;
        $$$setupUI$$$();
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setContentPane(this.panel1);
        this.setupUI();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupUI() {
        importButton.setActionCommand(ACTION_IMPORT);
        importButton.addActionListener(listener);
        importRelationsBetweenExistingButton.setActionCommand(ACTION_IMPORT_BETWEEN_EXISTING);
        importRelationsBetweenExistingButton.addActionListener(listener);
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (this.getRowCount() > 0 && this.getColumnCount() > columnIndex) {
                    Object value = this.getValueAt(0, columnIndex);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return super.getColumnClass(columnIndex);
            }
        };
        sorter = new TableRowSorter<TableModel>(tableModel);
        resultTable.setModel(tableModel);
        resultTable.setRowSorter(sorter);
        //filterTextField.setPreferredSize(new Dimension(200, Utility.INSTANCE.getJTextFieldHeight()));
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterRows();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterRows();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterRows();
            }
        });
    }

    private void filterRows() {
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filterTextField.getText()));
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

    public JTextField getFilterTextField() {
        return filterTextField;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        resultTable = new JTable() {
            //Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                return tooltipDataSource.getTooltipForResultRowAndColumn(rowIndex, colIndex);
            }
        };
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
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.SOUTH);
        importButton = new JButton();
        importButton.setText("Import Selected");
        panel2.add(importButton);
        importRelationsBetweenExistingButton = new JButton();
        importRelationsBetweenExistingButton.setText("Import relations between existing nodes");
        importRelationsBetweenExistingButton.setToolTipText("Imports all the results which are between existing nodes in the network.");
        panel2.add(importRelationsBetweenExistingButton);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, BorderLayout.CENTER);
        resultTable.setAutoCreateRowSorter(false);
        scrollPane1.setViewportView(resultTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.NORTH);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        panel3.add(panel4, BorderLayout.EAST);
        final JLabel label1 = new JLabel();
        label1.setText("Filter results:");
        panel4.add(label1);
        filterTextField = new JTextField();
        filterTextField.setColumns(10);
        panel4.add(filterTextField);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel3.add(panel5, BorderLayout.WEST);
        selectRelationsLeadingToButton = new JButton();
        selectRelationsLeadingToButton.setHorizontalTextPosition(11);
        selectRelationsLeadingToButton.setText("Select relations leading to selection");
        selectRelationsLeadingToButton.setToolTipText("Select all relations leading to the relations currently selected.");
        panel5.add(selectRelationsLeadingToButton);
        filterSelectedCheckBox = new JCheckBox();
        filterSelectedCheckBox.setText("Filter selected");
        filterSelectedCheckBox.setToolTipText("Only show currently selected relations.");
        panel5.add(filterSelectedCheckBox);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
