package eu.biogateway.cytoscape.internal.gui;

import eu.biogateway.cytoscape.internal.util.Utility;
import net.miginfocom.swing.MigLayout;
import eu.biogateway.cytoscape.internal.BGServiceManager;
import eu.biogateway.cytoscape.internal.gui.multiquery.BGMultiQueryPanel;
import eu.biogateway.cytoscape.internal.gui.multiquery.BGQueryConstraintPanel;
import eu.biogateway.cytoscape.internal.query.BGQueryParameter;
import eu.biogateway.cytoscape.internal.query.QueryTemplate;
import eu.biogateway.cytoscape.internal.util.SortedComboBoxModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import static eu.biogateway.cytoscape.internal.gui.BGQueryBuilderController.*;

/**
 * Created by sholmas on 14/03/2017.
 */
@SuppressWarnings("FieldCanBeLocal")
public class BGQueryBuilderView implements ChangeListener {
    private final ActionListener listener;
    private final BGRelationResultViewTooltipDataSource tableTooltipDataSource;

    public HashMap<String, JComponent> parameterComponents = new HashMap<>();

    private final JFrame mainFrame;
    private JTabbedPane tabPanel;
    private JPanel mainPanel;
    private JPanel queryPanel;
    private JPanel sparqlPanel;
    private JPanel resultPanel;
    private JPanel buttonPanel;
    private JPanel parameterPanel;
    private JButton runQueryButton;
    private JTextArea sparqlTextArea;
    private JTable resultTable;
    private JComboBox<String> querySelectionBox;
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
    private JTextPane bulkImportTextPane;
    private JComboBox bulkImportTypeComboBox;
    private JButton bulkSearchButton;
    private JTable bulkImportResultTable;
    private JButton bulkImportSelectedNodesNewButton;
    private JButton bulkImportSelectedCurrentButton;
    private JTextField bulkFilterTextField;
    private JCheckBox filterRelationsFROMExistingCheckBox;
    private JComboBox exampleQueryBox;
    private BGQueryConstraintPanel queryConstraintsPanel;
    private TableRowSorter<TableModel> sorter;
    private final DocumentListener filterDocumentListener = new DocumentListener() {
        void filter() {
            filterResultRows(sorter, filterResultsTextField);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            filter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filter();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            filter();
        }
    };
    private TableRowSorter<TableModel> bulkSorter;
    private final DocumentListener bulkFilterDocumentListener = new DocumentListener() {
        void filter() {
            filterResultRows(bulkSorter, bulkFilterTextField);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            filter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filter();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            filter();
        }
    };


    /**
     * @param listener
     * @param tableTooltipDataSource
     */
    public BGQueryBuilderView(ActionListener listener, BGRelationResultViewTooltipDataSource tableTooltipDataSource) {
        this.listener = listener;
        this.tableTooltipDataSource = tableTooltipDataSource;
        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;

        $$$setupUI$$$();
        frame.setPreferredSize(new Dimension(1440, 480));
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
        DefaultTableModel bulkTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        sorter = new TableRowSorter<>(tableModel);
        resultTable.setModel(tableModel);
        resultTable.setRowSorter(sorter);
        filterResultsTextField.getDocument().addDocumentListener(filterDocumentListener);

        bulkSorter = new TableRowSorter<>(bulkTableModel);
        bulkImportResultTable.setModel(bulkTableModel);
        bulkImportResultTable.setRowSorter(bulkSorter);
        bulkFilterTextField.getDocument().addDocumentListener(bulkFilterDocumentListener);

        filterSelectedCheckBox.addActionListener(e -> updateFilterBySelectedRows());

        queryConstraintsPanel = new BGQueryConstraintPanel(BGServiceManager.INSTANCE.getConfig().getActiveConstraints());
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
        filterRelationsFROMExistingCheckBox.addActionListener(listener);
        filterRelationsFROMExistingCheckBox.setActionCommand(Companion.getACTION_FILTER_EDGES_TO_EXISTING());

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

    private void filterResultRows(TableRowSorter<TableModel> rowSorter, JTextField textField) {
        rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + textField.getText()));
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
            filterResultRows(sorter, filterResultsTextField);
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
                for (int selectedRow : selectedRows) {
                    if (selectedRow == viewRow) {
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

        //JLabel description = new JLabel(query.getDescription());
        // description.setFont(description.getFont().deriveFont(Font.ITALIC));
        //descriptionPanel.add(description);

        parameterPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), query.getDescription()));


        for (BGQueryParameter parameter : query.getParameters()) {

            JLabel label = new JLabel(parameter.getName() + ": ");
            JComponent component;


            // TODO: FIX THIS CODE SO IT WORKS WITH DYNAMIC NODE TYPES.
            /*
            switch (parameter.getType()) {
                case GENE:
                    component = new BGAutocompleteComboBox(BGServiceManager.INSTANCE.getEndpoint(), () -> BGServiceManager.INSTANCE.getConfig().getNodeTypes().get());
                    break;
                case TAXON:
                    component = new BGAutocompleteComboBox(BGServiceManager.INSTANCE.getEndpoint(), () -> BGNodeType.Taxon);
                    break;
                case GO_TERM:
                    component = new BGAutocompleteComboBox(BGServiceManager.INSTANCE.getEndpoint(), () -> BGNodeType.GOTerm);
                    break;
                case PROTEIN:
                    component = new BGAutocompleteComboBox(BGServiceManager.INSTANCE.getEndpoint(), () -> BGNodeType.Protein);
                    break;
                case OPTIONAL_URI:
                    JTextField optionalField = new JTextField();
                    //optionalField.setPreferredSize(new Dimension(280, Utility.INSTANCE.getJTextFieldHeight()));
                    optionalField.setColumns(Constants.INSTANCE.getBG_QUERY_BUILDER_URI_FIELD_COLUMNS());
                    //component = optionalField;
                    component = new BGOptionalURIField(optionalField);
                    break;
                case TEXT:
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
                    JComboBox<String> comboBox = new JComboBox<>((String[]) parameter.getOptions().keySet().toArray());
                    component = new BGRelationTypeField(comboBox);
                    break;
                case RELATION_QUERY_ROW:
                    component = new BGRelationQueryRow((String[]) parameter.getOptions().keySet().toArray());
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
                    checkBox.addActionListener(e -> {
                        if (checkBox.isSelected()) {
                            component.setEnabled(dependency.isEnabled());
                        } else {
                            component.setEnabled(!dependency.isEnabled());
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
            */
        }
        mainFrame.repaint();
    }

    public void addMultiQueryLine() {
        this.multiQueryPanel.addQueryLine();
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

    public void setUpMultiQueryPanel() {
        this.queryConstraintsPanel = new BGQueryConstraintPanel(BGServiceManager.INSTANCE.getConfig().getActiveConstraints());
        queryConstraintsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Query Constraints"));
        this.multiQueryPanel = new BGMultiQueryPanel(queryConstraintsPanel);
        multiQueryPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Queries"));
        this.multiQueryPanel.addQueryLine();
        this.multiQueryContainer.add(queryConstraintsPanel, BorderLayout.NORTH);
        this.multiQueryContainer.add(multiQueryPanel, BorderLayout.CENTER);
        mainFrame.repaint();
        if (BGServiceManager.INSTANCE.getConfig().getActiveRelationTypes().isEmpty()) {
            // No Datasets are enabled!
            JOptionPane.showMessageDialog(mainFrame, "Select one or more relation types for the datasets in the BioGateway tab of the Control Panel.", "No datasets selected!", JOptionPane.WARNING_MESSAGE);
            mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
            Utility.INSTANCE.selectBioGatewayControlPanelTab();
//            BGTutorial tutorial = new BGTutorial();
        }
    }

    public void appendToPane(JTextPane tp, String msg, Color textColor) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, textColor);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
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

    public JComboBox getExampleQueryBox() {
        return exampleQueryBox;
    }

    public JTextPane getBulkImportTextPane() {
        return bulkImportTextPane;
    }

    public JComboBox getBulkImportTypeComboBox() {
        return bulkImportTypeComboBox;
    }

    public JTable getBulkImportResultTable() {
        return bulkImportResultTable;
    }

    public BGQueryConstraintPanel getQueryConstraintsPanel() {
        return queryConstraintsPanel;
    }

    public JCheckBox getFilterRelationsToExistingCheckBox() {
        return filterRelationsToExistingCheckBox;
    }

    public JCheckBox getFilterRelationsFROMExistingCheckBox() {
        return filterRelationsFROMExistingCheckBox;
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
        runChainQueryButton = new JButton();
        runChainQueryButton.setText("Run Query");
        panel2.add(runChainQueryButton);
        multiQueryContainer = new JPanel();
        multiQueryContainer.setLayout(new BorderLayout(0, 0));
        panel1.add(multiQueryContainer, BorderLayout.CENTER);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(panel3, BorderLayout.NORTH);
        panel3.setBorder(BorderFactory.createTitledBorder("Stored Queries"));
        exampleQueryBox = new JComboBox();
        panel3.add(exampleQueryBox);
        loadQueryButton = new JButton();
        loadQueryButton.setText("Load Query");
        panel3.add(loadQueryButton);
        saveQueryButton = new JButton();
        saveQueryButton.setText("Save Query");
        panel3.add(saveQueryButton);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Bulk Query", panel4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel4.add(panel5, BorderLayout.SOUTH);
        bulkSearchButton = new JButton();
        bulkSearchButton.setText("Bulk Search");
        panel5.add(bulkSearchButton);
        bulkImportSelectedNodesNewButton = new JButton();
        bulkImportSelectedNodesNewButton.setText("Import selected nodes to new network");
        panel5.add(bulkImportSelectedNodesNewButton);
        bulkImportSelectedCurrentButton = new JButton();
        bulkImportSelectedCurrentButton.setText("Import selected nodes to current network");
        panel5.add(bulkImportSelectedCurrentButton);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel4.add(panel6, BorderLayout.CENTER);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new BorderLayout(0, 0));
        panel6.add(panel7, BorderLayout.WEST);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel7.add(scrollPane1, BorderLayout.CENTER);
        bulkImportTextPane = new JTextPane();
        scrollPane1.setViewportView(bulkImportTextPane);
        final JLabel label1 = new JLabel();
        label1.setText("  Paste genes/proteins here:  ");
        panel7.add(label1, BorderLayout.NORTH);
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new BorderLayout(0, 0));
        panel6.add(panel8, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel8.add(scrollPane2, BorderLayout.CENTER);
        bulkImportResultTable = new JTable();
        bulkImportResultTable.setAutoCreateRowSorter(false);
        scrollPane2.setViewportView(bulkImportResultTable);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new BorderLayout(0, 0));
        panel4.add(panel9, BorderLayout.EAST);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new BorderLayout(0, 0));
        panel4.add(panel10, BorderLayout.NORTH);
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel10.add(panel11, BorderLayout.WEST);
        bulkImportTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Uniprot IDs");
        defaultComboBoxModel1.addElement("Entrez IDs");
        defaultComboBoxModel1.addElement("ENSEMBL IDs");
        defaultComboBoxModel1.addElement("Gene Symbols");
        defaultComboBoxModel1.addElement("GO terms");
        defaultComboBoxModel1.addElement("Protein names");
        bulkImportTypeComboBox.setModel(defaultComboBoxModel1);
        panel11.add(bulkImportTypeComboBox);
        final JLabel label2 = new JLabel();
        label2.setText("to import:");
        panel11.add(label2);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel10.add(panel12, BorderLayout.EAST);
        final JLabel label3 = new JLabel();
        label3.setText("Filter results:");
        panel12.add(label3);
        bulkFilterTextField = new JTextField();
        bulkFilterTextField.setColumns(10);
        panel12.add(bulkFilterTextField);
        queryPanel = new JPanel();
        queryPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Predefined Queries", queryPanel);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        queryPanel.add(buttonPanel, BorderLayout.SOUTH);
        runQueryButton = new JButton();
        runQueryButton.setText("Run Query");
        buttonPanel.add(runQueryButton);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new BorderLayout(0, 0));
        queryPanel.add(panel13, BorderLayout.CENTER);
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new GridBagLayout());
        panel13.add(parameterPanel, BorderLayout.CENTER);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new BorderLayout(0, 0));
        panel13.add(panel14, BorderLayout.NORTH);
        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout(0, 0));
        panel14.add(descriptionPanel, BorderLayout.SOUTH);
        querySelectionBox = new JComboBox();
        panel14.add(querySelectionBox, BorderLayout.NORTH);
        sparqlPanel = new JPanel();
        sparqlPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("SPARQL Code", sparqlPanel);
        final JScrollPane scrollPane3 = new JScrollPane();
        sparqlPanel.add(scrollPane3, BorderLayout.CENTER);
        sparqlTextArea = new JTextArea();
        scrollPane3.setViewportView(sparqlTextArea);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        sparqlPanel.add(panel15, BorderLayout.SOUTH);
        parseSPARQLButton = new JButton();
        parseSPARQLButton.setText("Parse SPARQL to Biogateway Query");
        panel15.add(parseSPARQLButton);
        resultPanel = new JPanel();
        resultPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Query Result", resultPanel);
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new BorderLayout(0, 0));
        resultPanel.add(panel16, BorderLayout.CENTER);
        final JScrollPane scrollPane4 = new JScrollPane();
        panel16.add(scrollPane4, BorderLayout.CENTER);
        resultTable.setAutoCreateRowSorter(false);
        resultTable.setFillsViewportHeight(false);
        scrollPane4.setViewportView(resultTable);
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel16.add(panel17, BorderLayout.SOUTH);
        importToNewButton = new JButton();
        importToNewButton.setText("Import to new Network");
        panel17.add(importToNewButton);
        importToSelectedNetworkButton = new JButton();
        importToSelectedNetworkButton.setText("Import to selected Network");
        panel17.add(importToSelectedNetworkButton);
        filterRelationsToExistingCheckBox = new JCheckBox();
        filterRelationsToExistingCheckBox.setText("Only relations TO nodes in current network");
        panel17.add(filterRelationsToExistingCheckBox);
        filterRelationsFROMExistingCheckBox = new JCheckBox();
        filterRelationsFROMExistingCheckBox.setText("Only relations FROM nodes in current network");
        panel17.add(filterRelationsFROMExistingCheckBox);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new BorderLayout(0, 0));
        panel16.add(panel18, BorderLayout.NORTH);
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel18.add(panel19, BorderLayout.WEST);
        selectUpstreamRelationsButton = new JButton();
        selectUpstreamRelationsButton.setHorizontalTextPosition(11);
        selectUpstreamRelationsButton.setText("Select relations leading to selection");
        selectUpstreamRelationsButton.setToolTipText("Select all relations leading to the relations currently selected.");
        panel19.add(selectUpstreamRelationsButton);
        filterSelectedCheckBox = new JCheckBox();
        filterSelectedCheckBox.setText("Filter selected");
        filterSelectedCheckBox.setToolTipText("Only show currently selected relations.");
        panel19.add(filterSelectedCheckBox);
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel18.add(panel20, BorderLayout.EAST);
        final JLabel label4 = new JLabel();
        label4.setText("Filter results:");
        panel20.add(label4);
        filterResultsTextField = new JTextField();
        filterResultsTextField.setColumns(10);
        panel20.add(filterResultsTextField);
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(panel21, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
