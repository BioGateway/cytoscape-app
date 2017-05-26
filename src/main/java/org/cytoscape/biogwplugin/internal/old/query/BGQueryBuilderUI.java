package org.cytoscape.biogwplugin.internal.old.query;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;

import static org.cytoscape.biogwplugin.internal.gui.BGQueryBuilderController.*;

/**
 * Created by sholmas on 14/03/2017.
 */
public class BGQueryBuilderUI implements ChangeListener {
    private final ActionListener listener;/*

    private final static String SERVER_PATH = "http://www.semantic-systems-biology.org/biogateway/endpoint";

    static final String ACTION_OPEN_XML_FILE = "openXMLFile";
    static final String ACTION_PARSE_XML = "parseXML";
    static final String ACTION_CREATE_QUERY = "crateQuery";
    static final String ACTION_CHANGED_QUERY = "changedQueryComboBox";
    static final String ACTION_RUN_QUERY = "runBiogwQuery";
    static final String ACTION_IMPORT_TO_SELECTED = "importToSelectedNetwork";
    static final String ACTION_IMPORT_TO_NEW = "importToNewNetwork";


    static final String CHANGE_TAB_CHANGED = "tabbedPaneHasChanged";
    static final String UNIPROT_PREFIX = "http://identifiers.org/uniprot/";
    static final String ONTOLOGY_PREFIX = "http://purl.obolibrary.org/obo/";

    private BGQueryBuilderModel model;
    private BGServiceManager serviceManager;*/

    public HashMap<String, JComponent> parameterComponents = new HashMap<>();


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

    /*
    public static void main(String[] args) {
        JFrame frame = new JFrame("Biogateway Query Builder");
        frame.setPreferredSize(new Dimension(600, 400));
        BGQueryBuilderUI gui = new BGQueryBuilderUI(frame);
        frame.setContentPane(gui.mainPanel);
        gui.createUIComponents();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        gui.loadXMLFileFromServer();
    }*/

    public BGQueryBuilderUI(ActionListener listener) {

        this.listener = listener;

        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;

        frame.setPreferredSize(new Dimension(600, 400));
        frame.setContentPane(this.mainPanel);
        this.createUIComponents();
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
        DefaultTableModel resultTableModel = new DefaultTableModel();
        resultTableModel.setColumnIdentifiers(new String[]{"Common name", "OPTIONAL_URI"});
        resultTable.setModel(resultTableModel);

        querySelectionBox.setModel(new SortedComboBoxModel<String>());

        parameterPanel.setLayout(new MigLayout("wrap 2"));

        runQueryButton.addActionListener(listener);
        runQueryButton.setActionCommand(Companion.getACTION_CHANGED_QUERY());
        querySelectionBox.addActionListener(listener);
        querySelectionBox.setActionCommand(Companion.getACTION_CHANGED_QUERY());
        importToNewButton.addActionListener(listener);
        importToNewButton.setActionCommand(Companion.getACTION_IMPORT_TO_NEW());
        importToSelectedNetworkButton.addActionListener(listener);
        importToSelectedNetworkButton.setActionCommand(Companion.getACTION_IMPORT_TO_SELECTED());
    }

    public void generateParameterFields(QueryTemplate query) {
        // Clear old data:
        parameterPanel.removeAll();
        parameterComponents = new HashMap<>();

        for (QueryParameter parameter : query.getParameters()) {

            //JPanel panel = createBoxPanel();
            //JPanel panel = new JPanel(new BorderLayout(0, 0));
            JLabel label = new JLabel(parameter.getName() + ": ");
            //panel.add(label, BorderLayout.WEST);
            JComponent component;
            switch (parameter.getType()) {
                case TEXT:
                case OPTIONAL_URI:
                case ONTOLOGY:
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
        parameterPanel = new JPanel();
        parameterPanel.setLayout(new GridBagLayout());
        queryPanel.add(parameterPanel, BorderLayout.CENTER);
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        resultPanel.add(panel1, BorderLayout.CENTER);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, BorderLayout.CENTER);
        resultTable = new JTable();
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setFillsViewportHeight(true);
        scrollPane2.setViewportView(resultTable);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel1.add(panel2, BorderLayout.SOUTH);
        importToNewButton = new JButton();
        importToNewButton.setText("Import to new Network");
        panel2.add(importToNewButton);
        importToSelectedNetworkButton = new JButton();
        importToSelectedNetworkButton.setText("Import to selected Network");
        panel2.add(importToSelectedNetworkButton);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.add(panel3, BorderLayout.NORTH);
        querySelectionBox = new JComboBox();
        panel3.add(querySelectionBox);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
