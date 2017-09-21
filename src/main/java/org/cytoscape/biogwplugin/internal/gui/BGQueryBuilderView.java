package org.cytoscape.biogwplugin.internal.gui;

import net.miginfocom.swing.MigLayout;
import org.cytoscape.biogwplugin.internal.query.BGQueryParameter;
import org.cytoscape.biogwplugin.internal.query.QueryTemplate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import static org.cytoscape.biogwplugin.internal.gui.BGQueryBuilderController.*;

/**
 * Created by sholmas on 14/03/2017.
 */
public class BGQueryBuilderView implements ChangeListener {
    private final ActionListener listener;

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
    private JButton validateURIsButton;
    private JButton addLineButton;
    private JButton removeLineButton;
    private JCheckBox filterRelationsToExistingCheckBox;


    public BGQueryBuilderView(ActionListener listener) {
        this.listener = listener;
        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;
        frame.setPreferredSize(new Dimension(1200, 480));
        frame.setContentPane(this.mainPanel);
        this.createUIComponents();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


    private void createUIComponents() {
        // TODO: Add support for all the result types
        DefaultTableModel resultTableModel = new DefaultTableModel();
        resultTableModel.setColumnIdentifiers(new String[]{"Common name", "OPTIONAL_URI"});
        resultTable.setModel(resultTableModel);

        querySelectionBox.setModel(new SortedComboBoxModel<String>());

        parameterPanel.setLayout(new MigLayout("wrap 2"));

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
        removeLineButton.addActionListener(listener);
        removeLineButton.setActionCommand(Companion.getACTION_REMOVE_MULTIQUERY_LINE());
        validateURIsButton.addActionListener(listener);
        validateURIsButton.setActionCommand(Companion.getACTION_VALIDATE_URIS());
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
                    optionalField.setPreferredSize(new Dimension(280, 20));
                    //component = optionalField;
                    component = new BGOptionalURIField(optionalField, listener);
                    break;
                case ONTOLOGY:
                case TEXT:
                case UNIPROT_ID:
                    JTextField field = new JTextField();
                    field.setPreferredSize(new Dimension(280, 20));
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
                    component = new BGRelationQueryRow((String[]) parameter.getOptions().keySet().toArray(), listener);
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

    public HashMap<String, JComponent> getParameterComponents() {
        return parameterComponents;
    }

    public void setParameterComponents(HashMap<String, JComponent> parameterComponents) {
        this.parameterComponents = parameterComponents;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public void setMainFrame(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public JTabbedPane getTabPanel() {
        return tabPanel;
    }

    public void setTabPanel(JTabbedPane tabPanel) {
        this.tabPanel = tabPanel;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void setMainPanel(JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public JPanel getQueryPanel() {
        return queryPanel;
    }

    public void setQueryPanel(JPanel queryPanel) {
        this.queryPanel = queryPanel;
    }

    public JPanel getSparqlPanel() {
        return sparqlPanel;
    }

    public void setSparqlPanel(JPanel sparqlPanel) {
        this.sparqlPanel = sparqlPanel;
    }

    public JPanel getResultPanel() {
        return resultPanel;
    }

    public void setResultPanel(JPanel resultPanel) {
        this.resultPanel = resultPanel;
    }

    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public void setButtonPanel(JPanel buttonPanel) {
        this.buttonPanel = buttonPanel;
    }

    public JPanel getParameterPanel() {
        return parameterPanel;
    }

    public void setParameterPanel(JPanel parameterPanel) {
        this.parameterPanel = parameterPanel;
    }

    public JButton getOpenXMLFileButton() {
        return openXMLFileButton;
    }

    public void setOpenXMLFileButton(JButton openXMLFileButton) {
        this.openXMLFileButton = openXMLFileButton;
    }

    public JButton getCreateQueryButton() {
        return createQueryButton;
    }

    public void setCreateQueryButton(JButton createQueryButton) {
        this.createQueryButton = createQueryButton;
    }

    public JButton getRunQueryButton() {
        return runQueryButton;
    }

    public void setRunQueryButton(JButton runQueryButton) {
        this.runQueryButton = runQueryButton;
    }

    public JTextArea getSparqlTextArea() {
        return sparqlTextArea;
    }

    public void setSparqlTextArea(JTextArea sparqlTextArea) {
        this.sparqlTextArea = sparqlTextArea;
    }

    public JTable getResultTable() {
        return resultTable;
    }

    public void setResultTable(JTable resultTable) {
        this.resultTable = resultTable;
    }

    public JComboBox getQuerySelectionBox() {
        return querySelectionBox;
    }

    public void setQuerySelectionBox(JComboBox querySelectionBox) {
        this.querySelectionBox = querySelectionBox;
    }

    public JButton getImportToNewButton() {
        return importToNewButton;
    }

    public void setImportToNewButton(JButton importToNewButton) {
        this.importToNewButton = importToNewButton;
    }

    public JButton getImportToSelectedNetworkButton() {
        return importToSelectedNetworkButton;
    }

    public void setImportToSelectedNetworkButton(JButton importToSelectedNetworkButton) {
        this.importToSelectedNetworkButton = importToSelectedNetworkButton;
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
        removeLineButton = new JButton();
        removeLineButton.setText("Remove Line");
        panel2.add(removeLineButton);
        validateURIsButton = new JButton();
        validateURIsButton.setText("Validate URIs");
        panel2.add(validateURIsButton);
        runChainQueryButton = new JButton();
        runChainQueryButton.setText("Run Query");
        panel2.add(runChainQueryButton);
        multiQueryContainer = new JPanel();
        multiQueryContainer.setLayout(new BorderLayout(0, 0));
        panel1.add(multiQueryContainer, BorderLayout.CENTER);
        queryPanel = new JPanel();
        queryPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Predefined Queries", queryPanel);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        queryPanel.add(buttonPanel, BorderLayout.SOUTH);
        runQueryButton = new JButton();
        runQueryButton.setText("Run Query");
        buttonPanel.add(runQueryButton);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        queryPanel.add(panel3, BorderLayout.CENTER);
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new GridBagLayout());
        panel3.add(parameterPanel, BorderLayout.CENTER);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel3.add(panel4, BorderLayout.NORTH);
        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout(0, 0));
        panel4.add(descriptionPanel, BorderLayout.SOUTH);
        querySelectionBox = new JComboBox();
        panel4.add(querySelectionBox, BorderLayout.NORTH);
        sparqlPanel = new JPanel();
        sparqlPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("SPARQL Code", sparqlPanel);
        final JScrollPane scrollPane1 = new JScrollPane();
        sparqlPanel.add(scrollPane1, BorderLayout.CENTER);
        sparqlTextArea = new JTextArea();
        scrollPane1.setViewportView(sparqlTextArea);
        resultPanel = new JPanel();
        resultPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Query Result", resultPanel);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        resultPanel.add(panel5, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel5.add(scrollPane2, BorderLayout.CENTER);
        resultTable = new JTable();
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(false);
        scrollPane2.setViewportView(resultTable);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel5.add(panel6, BorderLayout.SOUTH);
        importToNewButton = new JButton();
        importToNewButton.setText("Import to new Network");
        panel6.add(importToNewButton);
        importToSelectedNetworkButton = new JButton();
        importToSelectedNetworkButton.setText("Import to selected Network");
        panel6.add(importToSelectedNetworkButton);
        filterRelationsToExistingCheckBox = new JCheckBox();
        filterRelationsToExistingCheckBox.setText("Only show relations to nodes in current network");
        panel6.add(filterRelationsToExistingCheckBox);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(panel7, BorderLayout.NORTH);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
