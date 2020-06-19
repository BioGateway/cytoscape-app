package eu.biogateway.app.internal.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

@SuppressWarnings("FieldCanBeLocal")
public class BGRelationSearchResultsView {

    public static final String ACTION_IMPORT = "import nodes";
    public static final String ACTION_IMPORT_BETWEEN_EXISTING = "import relations to exsisting nodes";


    private final ActionListener listener;
    private JFrame mainFrame;
    private JPanel panel1;
    private JButton importButton;
    private JTable resultTable;
    private JButton importRelationsBetweenExistingButton;
    private JTextField filterTextField;
    private TableRowSorter<TableModel> sorter;
    private final BGRelationResultViewTooltipDataSource tooltipDataSource;
    private DocumentListener filterDocumentListener = new DocumentListener() {
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
    };

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
        sorter = new TableRowSorter<>(tableModel);
        resultTable.setModel(tableModel);
        resultTable.setRowSorter(sorter);

        filterTextField.getDocument().addDocumentListener(filterDocumentListener);
    }

    public void clearFilterField() {
        filterTextField.setText("");
    }

    private void filterRows() {
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filterTextField.getText()));
    }


    private void filterBySelectedRows(Boolean shouldFilterBySelected) {
        if (shouldFilterBySelected) {
            filterTextField.getDocument().removeDocumentListener(filterDocumentListener);
            clearFilterField();
            filterTextField.setEnabled(false);

            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    // Slightly inefficient code, but should not be noticeable with normal data sets.
                    int modelRow = entry.getIdentifier();
                    int viewRow = resultTable.convertRowIndexToView(modelRow);
                    int[] selectedRows = resultTable.getSelectedRows();
                    for (int selectedRow : selectedRows) {
                        if (selectedRow == viewRow) {
                            return true;
                        }
                    }
                    return false;
                }
            });

        } else {
            filterTextField.getDocument().addDocumentListener(filterDocumentListener);
            filterTextField.setEnabled(true);
            filterRows();
        }
    }


    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JTable getResultTable() {
        return resultTable;
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
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }
}
