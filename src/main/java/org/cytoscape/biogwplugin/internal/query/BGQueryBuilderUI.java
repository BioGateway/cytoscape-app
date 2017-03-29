package org.cytoscape.biogwplugin.internal.query;

import net.miginfocom.swing.MigLayout;
import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.parser.BGNetworkBuilder;
import org.cytoscape.biogwplugin.internal.util.Utility;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskIterator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sholmas on 14/03/2017.
 */
public class BGQueryBuilderUI implements ActionListener, ChangeListener {

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
    private HashMap<String, JComponent> parameterComponents = new HashMap<>();
    private BGServiceManager serviceManager;

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
    private QueryTemplate currentQuery;

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

    public BGQueryBuilderUI(BGServiceManager serviceManager) {

        JFrame frame = new JFrame("Biogateway Query Builder");
        this.mainFrame = frame;
        this.serviceManager = serviceManager;
        model = new BGQueryBuilderModel(serviceManager);

        frame.setPreferredSize(new Dimension(600, 400));
        frame.setContentPane(this.mainPanel);
        this.createUIComponents();
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        this.loadXMLFileFromServer();
    }

    private void loadXMLFileFromServer() {
        try {
            URL queryFileUrl = new URL("https://dl.dropboxusercontent.com/u/32368359/BiogatewayQueries.xml");
            URLConnection connection = queryFileUrl.openConnection();
            InputStream is = connection.getInputStream();
            model.parseXMLFile(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateUIAfterXMLLoad();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        DefaultTableModel resultTableModel = new DefaultTableModel();
        resultTableModel.setColumnIdentifiers(new String[]{"Common name", "URI"});
        resultTable.setModel(resultTableModel);

        querySelectionBox.setModel(new SortedComboBoxModel<String>());

        parameterPanel.setLayout(new MigLayout("wrap 2"));

        runQueryButton.addActionListener(this);
        runQueryButton.setActionCommand(ACTION_RUN_QUERY);
        querySelectionBox.addActionListener(this);
        querySelectionBox.setActionCommand(ACTION_CHANGED_QUERY);
        importToNewButton.addActionListener(this);
        importToNewButton.setActionCommand(ACTION_IMPORT_TO_NEW);
        importToSelectedNetworkButton.addActionListener(this);
        importToSelectedNetworkButton.setActionCommand(ACTION_IMPORT_TO_SELECTED);
    }

    private void updateUIAfterXMLLoad() {
        querySelectionBox.removeAllItems();
        for (String queryName : model.queries.keySet()) {
            querySelectionBox.addItem(queryName);
        }
    }

    private void generateParameterFields(QueryTemplate query) {
        // Clear old data:
        parameterPanel.removeAll();
        parameterComponents = new HashMap<>();

        for (QueryParameter parameter : query.parameters) {

            //JPanel panel = createBoxPanel();
            //JPanel panel = new JPanel(new BorderLayout(0, 0));
            JLabel label = new JLabel(parameter.name + ": ");
            //panel.add(label, BorderLayout.WEST);
            JComponent component;
            switch (parameter.type) {
                case TEXT:
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
                    component = new JComboBox<>(parameter.options.keySet().toArray());
                    break;
                default:
                    // Crash..!
                    component = null;
                    break;
            }
            parameterComponents.put(parameter.id, component);
            parameterPanel.add(label);
            parameterPanel.add(component);
        }
        mainFrame.repaint();
    }

    private void readParameterComponents() {
        for (QueryParameter parameter : currentQuery.parameters) {
            JComponent component = parameterComponents.get(parameter.id);

            switch (parameter.type) {
                case TEXT:
                    parameter.value = Utility.sanitizeParameter(((JTextField) component).getText());
                    break;
                case CHECKBOX:
                    parameter.value = ((JCheckBox) component).isSelected() ? "true" : "false";
                    break;
                case COMBOBOX:
                    JComboBox<String> box = (JComboBox<String>) component;
                    String selected = (String) box.getSelectedItem();
                    parameter.value = parameter.options.get(selected);
                    break;
                case UNIPROT_ID:
                    String uniprotID = ((JTextField) component).getText();
                    if (!uniprotID.startsWith(UNIPROT_PREFIX)) {
                        uniprotID = UNIPROT_PREFIX + uniprotID;
                    }
                    parameter.value = Utility.sanitizeParameter(uniprotID);
                    break;
                case ONTOLOGY:
                    String ontology = ((JTextField) component).getText();
                    if (!ontology.startsWith(ONTOLOGY_PREFIX)) {
                        ontology = ONTOLOGY_PREFIX + ontology;
                    }
                    parameter.value = Utility.sanitizeParameter(ontology);
                default:
                    break;
            }
        }
    }

    private void updateSelectedQuery() {
        currentQuery = model.queries.get((String) querySelectionBox.getSelectedItem());
        if (currentQuery != null) {
            generateParameterFields(currentQuery);
        }
    }

    private void openXMLFile() {
        try {
            InputStream inputStream = new FileInputStream(openFileChooser());
            model.parseXMLFile(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void createQuery() {

    }

    private void runQuery() {
        if (validatePropertyFields()) {
            readParameterComponents();
            String queryString = model.createQueryString(currentQuery);
            this.sparqlTextArea.setText(queryString);

            BGNodeSearchQuery query = new BGNodeSearchQuery(SERVER_PATH, queryString, serviceManager);
            Runnable callback = () -> {
                for (BGNode node : query.returnData) {
                    DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                    String[] row = {node.commonName, node.URI};
                    model.addRow(row);
                }
                tabPanel.setSelectedIndex(2);
            };
            query.addCallback(callback);

            TaskIterator iterator = new TaskIterator(query);
            serviceManager.getTaskManager().execute(iterator);
        } else {
            JOptionPane.showMessageDialog(this.mainFrame, "All text fields must be filled out!");
        }
    }

    private void importSelectedResults(CyNetwork network) {

        // 1. Get the selected lines from the table.
        ArrayList<String> selectedURIs = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        for (int row : resultTable.getSelectedRows()) {
            String uri = (String) model.getValueAt(row, 1);
            selectedURIs.add(uri);
        }

        // 2. The nodes have already been fetched. There should be a cache storing them somewhere.

        ArrayList<BGNode> bgNodes = serviceManager.getCache().getNodesWithURIs(selectedURIs);

        if (network == null) {
            network = BGNetworkBuilder.createNetworkFromBGNodes(bgNodes, serviceManager);
        } else {
            BGNetworkBuilder.addBGNodesToNetwork(network, bgNodes, serviceManager);
        }
        if (!serviceManager.getNetworkManager().networkExists(network.getSUID())) {
            serviceManager.getNetworkManager().addNetwork(network);
            BGNetworkBuilder.createNetworkView(network, serviceManager);
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case ACTION_OPEN_XML_FILE:
                openXMLFile();
                break;
            case ACTION_CREATE_QUERY:
                createQuery();
                break;
            case ACTION_RUN_QUERY:
                runQuery();
                break;
            case ACTION_CHANGED_QUERY:
                updateSelectedQuery();
                break;
            case ACTION_IMPORT_TO_SELECTED:
                CyNetwork network = serviceManager.getApplicationManager().getCurrentNetwork();
                importSelectedResults(network);
                break;
            case ACTION_IMPORT_TO_NEW:
                importSelectedResults(null);
            default:
                break;
        }
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

    private boolean validatePropertyFields() {
        for (QueryParameter parameter : currentQuery.parameters) {
            JComponent component = parameterComponents.get(parameter.id);
            if (component instanceof JTextField) {
                JTextField field = (JTextField) component;
                field.setText(Utility.sanitizeParameter(field.getText()));
                if (field.getText().length() == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private File openFileChooser() {
        JFileChooser chooser = new JFileChooser();

        FileFilter filter = new FileFilter() {
            @Override
            public String getDescription() {
                return "XML File";
            }

            @Override
            public boolean accept(File f) {

                if (f.getName().toLowerCase().endsWith("xml")) return true;
                if (f.isDirectory()) return true;

                return false;
            }
        };

        chooser.setFileFilter(filter);

        int choice = chooser.showOpenDialog(this.mainFrame);
        if (choice != JFileChooser.APPROVE_OPTION) return null;
        File chosenFile = chooser.getSelectedFile();

        return chosenFile;
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
