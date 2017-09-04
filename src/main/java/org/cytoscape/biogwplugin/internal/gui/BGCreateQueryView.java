package org.cytoscape.biogwplugin.internal.gui;

import net.miginfocom.swing.MigLayout;
import org.cytoscape.biogwplugin.internal.query.BGQuery;
import org.cytoscape.biogwplugin.internal.query.BGQueryParameter;
import org.cytoscape.biogwplugin.internal.query.QueryTemplate;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;

import static org.cytoscape.biogwplugin.internal.gui.BGQueryBuilderController.*;

/**
 * Created by sholmas on 14/03/2017.
 */
public class BGCreateQueryView implements ChangeListener {
    private final ActionListener listener;

    public HashMap<String, JComponent> parameterComponents = new HashMap<>();
    public HashMap<String, JComponent> chainedParametersComponents = new HashMap<>();

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
    private JPanel chainedParametersPanel;
    private JButton validateURIsButton;
    private JButton addLineButton;
    private JButton removeLineButton;

    public BGCreateQueryView(ActionListener listener) {
        this.listener = listener;
        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setContentPane(this.mainPanel);
        this.createUIComponents();
        frame.pack();
        frame.setVisible(true);
    }


    private void createUIComponents() {
        // TODO: Add support for all the result types
        DefaultTableModel resultTableModel = new DefaultTableModel();
        resultTableModel.setColumnIdentifiers(new String[]{"Common name", "OPTIONAL_URI"});
        resultTable.setModel(resultTableModel);

        querySelectionBox.setModel(new SortedComboBoxModel<String>());

        parameterPanel.setLayout(new MigLayout("wrap 2"));
        chainedParametersPanel.setLayout(new MigLayout("wrap 2"));

        runQueryButton.addActionListener(listener);
        runQueryButton.setActionCommand(Companion.getACTION_RUN_QUERY());
        querySelectionBox.addActionListener(listener);
        querySelectionBox.setActionCommand(Companion.getACTION_CHANGED_QUERY());
        importToNewButton.addActionListener(listener);
        importToNewButton.setActionCommand(Companion.getACTION_IMPORT_TO_NEW());
        importToSelectedNetworkButton.addActionListener(listener);
        importToSelectedNetworkButton.setActionCommand(Companion.getACTION_IMPORT_TO_SELECTED());

        runChainQueryButton.addActionListener(listener);
        runChainQueryButton.setActionCommand(Companion.getACTION_RUN_CHAIN_QUERY());
        addLineButton.addActionListener(listener);
        addLineButton.setActionCommand(Companion.getACTION_ADD_CHAIN_RELATION());
        removeLineButton.addActionListener(listener);
        removeLineButton.setActionCommand(Companion.getACTION_REMOVE_CHAIN_RELATION());
        validateURIsButton.addActionListener(listener);
        validateURIsButton.setActionCommand(Companion.getACTION_VALIDATE_URIS());
    }


    public void removeParameterField(BGQueryParameter parameter) {
        JComponent component = chainedParametersComponents.get(parameter.getId());
        if (component != null) {
            chainedParametersPanel.remove(component);
            chainedParametersComponents.remove(parameter.getId());
        }
        JComponent label = chainedParametersComponents.get(parameter.getId() + "_label");
        if (label != null) {
            chainedParametersPanel.remove(label);
            chainedParametersComponents.remove(parameter.getId() + "_label");
        }
        chainedParametersPanel.repaint();
    }

    public void addChainedParameterLine(BGQueryParameter relationParameter, BGQueryParameter nodeParameter) {
        addChainedParameterField(relationParameter);
        addChainedParameterField(nodeParameter);
    }

    public void addChainedParameterField(BGQueryParameter parameter) {
        JLabel label = new JLabel(parameter.getName() + ":");
        JComponent component;
        switch (parameter.getType()) {
            case OPTIONAL_URI:
                JTextField optionalField = new JTextField();
                optionalField.setPreferredSize(new Dimension(280, 20));
                component = new BGOptionalURIField(optionalField, listener);
                break;
            case COMBOBOX:
                component = new JComboBox<>(parameter.getOptions().keySet().toArray());
                break;
            default:
                component = null;
                // TODO: Exception should be thrown.
                break;
        }
        chainedParametersComponents.put(parameter.getId(), component);
        chainedParametersComponents.put(parameter.getId() + "_label", label);
        chainedParametersPanel.add(label);
        chainedParametersPanel.add(component);
        mainFrame.repaint();
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
        queryPanel = new JPanel();
        queryPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Query", queryPanel);
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        queryPanel.add(buttonPanel, BorderLayout.SOUTH);
        runQueryButton = new JButton();
        runQueryButton.setText("Run Query");
        buttonPanel.add(runQueryButton);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        queryPanel.add(panel1, BorderLayout.CENTER);
        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel1.add(descriptionPanel, BorderLayout.NORTH);
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new GridBagLayout());
        panel1.add(parameterPanel, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Chained Relations Query", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel2.add(panel3, BorderLayout.SOUTH);
        addLineButton = new JButton();
        addLineButton.setText("Add Line");
        panel3.add(addLineButton);
        removeLineButton = new JButton();
        removeLineButton.setText("Remove Line");
        panel3.add(removeLineButton);
        validateURIsButton = new JButton();
        validateURIsButton.setText("Validate URIs");
        panel3.add(validateURIsButton);
        runChainQueryButton = new JButton();
        runChainQueryButton.setText("Run Query");
        panel3.add(runChainQueryButton);
        chainedParametersPanel = new JPanel();
        chainedParametersPanel.setLayout(new BorderLayout(0, 0));
        panel2.add(chainedParametersPanel, BorderLayout.CENTER);
        sparqlPanel = new JPanel();
        sparqlPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("SparQL", sparqlPanel);
        final JScrollPane scrollPane1 = new JScrollPane();
        sparqlPanel.add(scrollPane1, BorderLayout.CENTER);
        sparqlTextArea = new JTextArea();
        scrollPane1.setViewportView(sparqlTextArea);
        resultPanel = new JPanel();
        resultPanel.setLayout(new BorderLayout(0, 0));
        tabPanel.addTab("Query Result", resultPanel);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        resultPanel.add(panel4, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel4.add(scrollPane2, BorderLayout.CENTER);
        resultTable = new JTable();
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);
        scrollPane2.setViewportView(resultTable);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel4.add(panel5, BorderLayout.SOUTH);
        importToNewButton = new JButton();
        importToNewButton.setText("Import to new Network");
        panel5.add(importToNewButton);
        importToSelectedNetworkButton = new JButton();
        importToSelectedNetworkButton.setText("Import to selected Network");
        panel5.add(importToSelectedNetworkButton);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(panel6, BorderLayout.NORTH);
        querySelectionBox = new JComboBox();
        panel6.add(querySelectionBox);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
