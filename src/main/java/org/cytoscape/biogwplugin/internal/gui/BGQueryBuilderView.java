package org.cytoscape.biogwplugin.internal.gui;

import net.miginfocom.swing.MigLayout;
import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.query.BGQueryParameter;
import org.cytoscape.biogwplugin.internal.query.QueryTemplate;
import org.cytoscape.biogwplugin.internal.util.Constants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import static org.cytoscape.biogwplugin.internal.gui.BGQueryBuilderController.*;

/**
 * Created by sholmas on 14/03/2017.
 */
public class BGQueryBuilderView implements ChangeListener {
    private final ActionListener listener;
    private final BGRelationResultViewTooltipDataSource tableTooltipDataSource;
    private final BGServiceManager serviceManager;

    public HashMap<String, JComponent> parameterComponents = new HashMap<>();

    private int numberOfChainedParameters = 0;


    private JFrame mainFrame;
    private JTabbedPane tabPanel;
    private JPanel mainPanel;
    private JPanel queryPanel;
    private JPanel sparqlPanel;
    private JPanel resultPanel;
    private JPanel buttonPanel;
    private JPanel parameterPanel;
    private JButton openXMLFileButton;
    private JButton createQueryButton;
    private JButton runQueryButton;
    private JTextArea sparqlTextArea;
    private JTable resultTable;
    private JComboBox querySelectionBox;
    private JButton importToNewButton;
    private JButton importToSelectedNetworkButton;
    private JPanel descriptionPanel;
    private JButton runChainQueryButton;
    private JPanel multiQueryContainer;
    private BGMultiQueryPanel multiQueryPanel;
    private JButton generateSPARQLButton;
    private JButton addLineButton;
    private JCheckBox filterRelationsToExistingCheckBox;
    private JButton parseSPARQLButton;
    private JButton loadQueryButton;
    private JButton saveQueryButton;
    private JTextField filterResultsTextField;
    private JButton selectUpstreamRelationsButton;
    private JCheckBox filterSelectedCheckBox;
    private JTextArea bulkImportTextArea;
    private JComboBox bulkImportTypeComboBox;
    private JButton bulkSearchButton;
    private JTable bulkImportResultTable;
    private JButton bulkImportSelectedNodesNewButton;
    private JButton bulkImportSelectedCurrentButton;
    private TableRowSorter<TableModel> sorter;
    private DocumentListener filterDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            filterResultRows();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filterResultRows();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            filterResultRows();
        }
    };


    public BGQueryBuilderView(ActionListener listener, BGRelationResultViewTooltipDataSource tableTooltipDataSource, BGServiceManager serviceManager) {
        this.listener = listener;
        this.tableTooltipDataSource = tableTooltipDataSource;
        this.serviceManager = serviceManager;
        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;

        $$$setupUI$$$();
        frame.setPreferredSize(new Dimension(1200, 480));
        frame.setContentPane(this.mainPanel);

        setupUI();
        setUpActionListeners();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // Custom UI creation code that is executed before the main GUI Designer generated code. (Used for components with "custom create" checked)
        resultTable = new JTable() {
            //Implement table cell tool tips.
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                return tableTooltipDataSource.getTooltipForResultRowAndColumn(rowIndex, colIndex);
            }
        };
    }

    private void setupUI() {
        querySelectionBox.setModel(new SortedComboBoxModel<String>());
        parameterPanel.setLayout(new MigLayout("wrap 2"));

        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sorter = new TableRowSorter<TableModel>(tableModel);
        resultTable.setModel(tableModel);
        resultTable.setRowSorter(sorter);
        //filterResultsTextField.setPreferredSize(new Dimension(200, Utility.INSTANCE.getJTextFieldHeight()));
        filterResultsTextField.getDocument().addDocumentListener(filterDocumentListener);
        filterSelectedCheckBox.addActionListener(e -> updateFilterBySelectedRows());
    }

    private void setUpActionListeners() {
        runQueryButton.addActionListener(listener);
        runQueryButton.setActionCommand(Companion.getACTION_RUN_QUERY());
        querySelectionBox.addActionListener(listener);
        querySelectionBox.setActionCommand(Companion.getACTION_CHANGED_QUERY());
        importToNewButton.addActionListener(listener);
        importToNewButton.setActionCommand(Companion.getACTION_IMPORT_TO_NEW());
        importToSelectedNetworkButton.addActionListener(listener);
        importToSelectedNetworkButton.setActionCommand(Companion.getACTION_IMPORT_TO_SELECTED());
        filterRelationsToExistingCheckBox.addActionListener(listener);
        filterRelationsToExistingCheckBox.setActionCommand(Companion.getACTION_FILTER_EDGES_TO_EXISTING());

        runChainQueryButton.addActionListener(listener);
        runChainQueryButton.setActionCommand(Companion.getACTION_RUN_MULTIQUERY());
        addLineButton.addActionListener(listener);
        addLineButton.setActionCommand(Companion.getACTION_ADD_MULTIQUERY_LINE());
        generateSPARQLButton.addActionListener(listener);
        generateSPARQLButton.setActionCommand(Companion.getACTION_GENERATE_SPARQL());
        parseSPARQLButton.addActionListener(listener);
        parseSPARQLButton.setActionCommand(Companion.getACTION_PARSE_SPARQL());
        loadQueryButton.addActionListener(listener);
        loadQueryButton.setActionCommand(Companion.getACTION_LOAD_SPARQL());
        saveQueryButton.addActionListener(listener);
        saveQueryButton.setActionCommand(Companion.getACTION_WRITE_SPARQL());
        selectUpstreamRelationsButton.addActionListener(listener);
        selectUpstreamRelationsButton.setActionCommand(Companion.getACTION_SELECT_UPSTREAM_RELATIONS());
        bulkSearchButton.addActionListener(listener);
        bulkSearchButton.setActionCommand(Companion.getACTION_RUN_BULK_IMPORT());
        bulkImportSelectedCurrentButton.addActionListener(listener);
        bulkImportSelectedCurrentButton.setActionCommand(Companion.getACTION_BULK_IMPORT_TO_CURRENT_NETWORK());
        bulkImportSelectedNodesNewButton.addActionListener(listener);
        bulkImportSelectedNodesNewButton.setActionCommand(Companion.getACTION_BULK_IMPORT_TO_NEW_NETWORK());
    }

    public void clearFilterField() {
        filterResultsTextField.setText("");
    }

    private void filterResultRows() {
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filterResultsTextField.getText()));
    }

    private void updateFilterBySelectedRows() {
        boolean shouldFilterBySelected = filterSelectedCheckBox.isSelected();

        if (shouldFilterBySelected) {
            filterResultsTextField.getDocument().removeDocumentListener(filterDocumentListener);
            clearFilterField();
            filterResultsTextField.setEnabled(false);
            filterSelectedRows();
        } else {
            filterResultsTextField.getDocument().addDocumentListener(filterDocumentListener);
            filterResultsTextField.setEnabled(true);
            filterResultRows();
        }
    }

    private void filterSelectedRows() {
        sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                // Slightly inefficient code, but should not be noticeable with normal data sets.
                int modelRow = entry.getIdentifier();
                int viewRow = resultTable.convertRowIndexToView(modelRow);
                int[] selectedRows = resultTable.getSelectedRows();
                for (int i = 0; i < selectedRows.length; i++) {
                    if (selectedRows[i] == viewRow) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void generateParameterFields(QueryTemplate query) {
        // Clear old data:
        parameterPanel.removeAll();
        descriptionPanel.removeAll();

        parameterComponents = new HashMap<>();

        JLabel description = new JLabel(query.getDescription());
        description.setFont(description.getFont().deriveFont(Font.ITALIC));
        descriptionPanel.add(description);

        for (BGQueryParameter parameter : query.getParameters()) {

            JLabel label = new JLabel(parameter.getName() + ": ");
            JComponent component;
            switch (parameter.getType()) {
                case OPTIONAL_URI:
                    JTextField optionalField = new JTextField();
                    //optionalField.setPreferredSize(new Dimension(280, Utility.INSTANCE.getJTextFieldHeight()));
                    optionalField.setColumns(Constants.INSTANCE.getBG_QUERY_BUILDER_URI_FIELD_COLUMNS());
                    //component = optionalField;
                    component = new BGOptionalURIField(optionalField, serviceManager);
                    break;
                case ONTOLOGY:
                case TEXT:
                case UNIPROT_ID:
                    JTextField field = new JTextField();
                    field.setColumns(Constants.INSTANCE.getBG_QUERY_BUILDER_URI_FIELD_COLUMNS());
                    //field.setPreferredSize(new Dimension(280, Utility.INSTANCE.getJTextFieldHeight()));
                    component = field;
                    break;
                case CHECKBOX:
                    component = new JCheckBox();
                    break;
                case COMBOBOX:
                    component = new JComboBox<>(parameter.getOptions().keySet().toArray());
                    break;
                case RELATION_COMBOBOX:
                    JComboBox comboBox = new JComboBox<>((String[]) parameter.getOptions().keySet().toArray());
                    component = new BGRelationTypeField(comboBox);
                    break;
                case RELATION_QUERY_ROW:
                    component = new BGRelationQueryRow((String[]) parameter.getOptions().keySet().toArray(), serviceManager);
                    break;
                default:
                    // Crash..!
                    component = null;
                    break;
            }

            BGQueryParameter.EnabledDependency dependency = parameter.getDependency();

            if (dependency != null) {
                JComponent dependingComponent = parameterComponents.get(dependency.getDependingParameter());
                if (dependingComponent != null && dependingComponent instanceof JCheckBox) {
                    JCheckBox checkBox = (JCheckBox) dependingComponent;
                    checkBox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (checkBox.isSelected()) {
                                component.setEnabled(dependency.isEnabled());
                            } else {
                                component.setEnabled(!dependency.isEnabled());
                            }
                        }
                    });
                    if (checkBox.isSelected()) {
                        component.setEnabled(dependency.isEnabled());
                    } else {
                        component.setEnabled(!dependency.isEnabled());
                    }
                }
            }

            parameterComponents.put(parameter.getId(), component);
            parameterPanel.add(label);
            parameterPanel.add(component);
        }
        mainFrame.repaint();
    }

    public void addMultiQueryLine() {
        this.multiQueryPanel.addQueryLine();
        mainFrame.repaint();
    }

    public void removeLastMultiQueryLine() {
        int lastIndex = multiQueryPanel.getQueryLines().size() - 1;
        if (lastIndex < 1) {
            return;
        }
        BGMultiQueryLine lastLine = multiQueryPanel.getQueryLines().get(lastIndex);
        this.multiQueryPanel.removeQueryLine(lastLine);
        mainFrame.repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof JTabbedPane) {
            int index = ((JTabbedPane) e.getSource()).getSelectedIndex();
            switch (index) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                default:
                    break;
            }
        }
    }

    public void setUpMultiQueryPanel(BGMultiQueryPanel multiQueryPanel) {
        this.multiQueryContainer.add(multiQueryPanel, BorderLayout.CENTER);
        this.multiQueryPanel = multiQueryPanel;
        mainFrame.repaint();
    }

    public BGMultiQueryPanel getMultiQueryPanel() {
        return multiQueryPanel;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public JTabbedPane getTabPanel() {
        return tabPanel;
    }

    public JTextArea getSparqlTextArea() {
        return sparqlTextArea;
    }

    public JTable getResultTable() {
        return resultTable;
    }

    public JComboBox getQuerySelectionBox() {
        return querySelectionBox;
    }

    public JTextArea getBulkImportTextArea() {
        return bulkImportTextArea;
    }

    public JComboBox getBulkImportTypeComboBox() {
        return bulkImportTypeComboBox;
    }

    public JTable getBulkImportResultTable() {
        return bulkImportResultTable;
    }

    public JButton getBulkImportSelectedNodesNewButton() {
        return bulkImportSelectedNodesNewButton;
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        tabPanel = new JTabbedPane();
        mainPanel.add(tabPanel, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Build Query", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.SOUTH);
        addLineButton = new JButton();
        addLineButton.setText("Add Line");
        panel2.add(addLineButton);
        generateSPARQLButton = new JButton();
        generateSPARQLButton.setText("Generate SPARQL");
        panel2.add(generateSPARQLButton);
        loadQueryButton = new JButton();
        loadQueryButton.setText("Load Query");
        panel2.add(loadQueryButton);
        saveQueryButton = new JButton();
        saveQueryButton.setText("Save Query");
        panel2.add(saveQueryButton);
        runChainQueryButton = new JButton();
        runChainQueryButton.setText("Run Query");
        panel2.add(runChainQueryButton);
        multiQueryContainer = new JPanel();
        multiQueryContainer.setLayout(new BorderLayout(0, 0));
        panel1.add(multiQueryContainer, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Bulk Import", panel3);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel3.add(panel4, BorderLayout.SOUTH);
        bulkSearchButton = new JButton();
        bulkSearchButton.setText("Bulk Search");
        panel4.add(bulkSearchButton);
        bulkImportSelectedNodesNewButton = new JButton();
        bulkImportSelectedNodesNewButton.setText("Import selected nodes to new network");
        panel4.add(bulkImportSelectedNodesNewButton);
        bulkImportSelectedCurrentButton = new JButton();
        bulkImportSelectedCurrentButton.setText("Import selected nodes to current network");
        panel4.add(bulkImportSelectedCurrentButton);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel3.add(panel5, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.WEST);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel6.add(scrollPane1, BorderLayout.CENTER);
        bulkImportTextArea = new JTextArea();
        bulkImportTextArea.setColumns(20);
        scrollPane1.setViewportView(bulkImportTextArea);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new BorderLayout(0, 0));
        panel5.add(panel7, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel7.add(scrollPane2, BorderLayout.CENTER);
        bulkImportResultTable = new JTable();
        bulkImportResultTable.setAutoCreateRowSorter(true);
        scrollPane2.setViewportView(bulkImportResultTable);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        panel3.add(panel8, BorderLayout.EAST);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel3.add(panel9, BorderLayout.NORTH);
        bulkImportTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Genes");
        defaultComboBoxModel1.addElement("Proteins");
        bulkImportTypeComboBox.setModel(defaultComboBoxModel1);
        panel9.add(bulkImportTypeComboBox);
        final JLabel label1 = new JLabel();
        label1.setText("to import:");
        panel9.add(label1);
        queryPanel = new JPanel();
        queryPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Predefined Queries", queryPanel);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        queryPanel.add(buttonPanel, BorderLayout.SOUTH);
        runQueryButton = new JButton();
        runQueryButton.setText("Run Query");
        buttonPanel.add(runQueryButton);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new BorderLayout(0, 0));
        queryPanel.add(panel10, BorderLayout.CENTER);
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new GridBagLayout());
        panel10.add(parameterPanel, BorderLayout.CENTER);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new BorderLayout(0, 0));
        panel10.add(panel11, BorderLayout.NORTH);
        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout(0, 0));
        panel11.add(descriptionPanel, BorderLayout.SOUTH);
        querySelectionBox = new JComboBox();
        panel11.add(querySelectionBox, BorderLayout.NORTH);
        sparqlPanel = new JPanel();
        sparqlPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("SPARQL Code", sparqlPanel);
        final JScrollPane scrollPane3 = new JScrollPane();
        sparqlPanel.add(scrollPane3, BorderLayout.CENTER);
        sparqlTextArea = new JTextArea();
        scrollPane3.setViewportView(sparqlTextArea);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        sparqlPanel.add(panel12, BorderLayout.SOUTH);
        parseSPARQLButton = new JButton();
        parseSPARQLButton.setText("Parse SPARQL to Biogateway Query");
        panel12.add(parseSPARQLButton);
        resultPanel = new JPanel();
        resultPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Query Result", resultPanel);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new BorderLayout(0, 0));
        resultPanel.add(panel13, BorderLayout.CENTER);
        final JScrollPane scrollPane4 = new JScrollPane();
        panel13.add(scrollPane4, BorderLayout.CENTER);
        resultTable.setAutoCreateRowSorter(false);
        resultTable.setFillsViewportHeight(false);
        scrollPane4.setViewportView(resultTable);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel13.add(panel14, BorderLayout.SOUTH);
        importToNewButton = new JButton();
        importToNewButton.setText("Import to new Network");
        panel14.add(importToNewButton);
        importToSelectedNetworkButton = new JButton();
        importToSelectedNetworkButton.setText("Import to selected Network");
        panel14.add(importToSelectedNetworkButton);
        filterRelationsToExistingCheckBox = new JCheckBox();
        filterRelationsToExistingCheckBox.setText("Only show relations to nodes in current network");
        panel14.add(filterRelationsToExistingCheckBox);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new BorderLayout(0, 0));
        panel13.add(panel15, BorderLayout.NORTH);
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel15.add(panel16, BorderLayout.WEST);
        selectUpstreamRelationsButton = new JButton();
        selectUpstreamRelationsButton.setHorizontalTextPosition(11);
        selectUpstreamRelationsButton.setText("Select upstream relations");
        selectUpstreamRelationsButton.setToolTipText("Select all relations leading to the relations currently selected.");
        panel16.add(selectUpstreamRelationsButton);
        filterSelectedCheckBox = new JCheckBox();
        filterSelectedCheckBox.setText("Filter selected");
        filterSelectedCheckBox.setToolTipText("Only show currently selected relations.");
        panel16.add(filterSelectedCheckBox);
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel15.add(panel17, BorderLayout.EAST);
        final JLabel label2 = new JLabel();
        label2.setText("Filter results:");
        panel17.add(label2);
        filterResultsTextField = new JTextField();
        filterResultsTextField.setColumns(10);
        panel17.add(filterResultsTextField);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(panel18, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
