package eu.biogateway.app.internal.gui;

import eu.biogateway.app.internal.BGServiceManager;
import org.osgi.framework.Version;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

@SuppressWarnings("FieldCanBeLocal")
public class BGSettingsView {

    final JFrame mainFrame;

    private JTextField configFileURlField;
    private JButton saveChangesButton;
    private JButton useDefaultButton;
    private JButton browseButton;
    private JButton reloadConfigButton;
    private JPanel mainPanel;
    private JLabel versionLabel;
    private JComboBox comboBox1;
    private JTabbedPane tabbedPane1;
    private JButton saveDBSelection;
    private BGSettingsController controller;


    public BGSettingsView(BGSettingsController controller) {
        this.controller = controller;
        $$$setupUI$$$();
        mainFrame = new JFrame("BioGateway Settings");
        mainFrame.setContentPane(this.mainPanel);

        setupButtons();

        Version version = BGServiceManager.INSTANCE.getConfig().getCurrentVersion();

        if (version != null) {
            versionLabel.setText(version.toString());
        }

        for (Integer dbVersion : controller.getAvailableVersions()) {
            comboBox1.addItem(dbVersion);
        }
        Integer selectedVersion = controller.getSelectedVersion();
        if (selectedVersion != null) {
            comboBox1.setSelectedItem(selectedVersion);
        }


        mainFrame.pack();
        mainFrame.setVisible(true);

    }

    void setConfigFileURlFieldText(String text) {
        configFileURlField.setText(text);
    }

    private void setupButtons() {
        saveChangesButton.addActionListener(e -> {
            String url = configFileURlField.getText();
            controller.setConfigFilePath(url);
        });
        useDefaultButton.addActionListener(e -> {
            controller.useDefaults();
        });
        browseButton.addActionListener(e -> {
            controller.browseForConfigFile();
        });
        reloadConfigButton.addActionListener(e -> {
            controller.reloadConfigFile();
        });
        saveDBSelection.addActionListener(e -> {
            Integer selectedVersion = (Integer) comboBox1.getSelectedItem();
            if (selectedVersion != null && selectedVersion > 0) {
                controller.setDBVersion(selectedVersion);
            }
        });
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
        tabbedPane1 = new JTabbedPane();
        mainPanel.add(tabbedPane1, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 10));
        tabbedPane1.addTab("Config File", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.NORTH);
        panel2.setBorder(BorderFactory.createTitledBorder(null, "BioGateway DB Version", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        comboBox1 = new JComboBox();
        panel2.add(comboBox1, BorderLayout.WEST);
        saveDBSelection = new JButton();
        saveDBSelection.setText("Load");
        panel2.add(saveDBSelection, BorderLayout.EAST);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        panel1.add(panel3, BorderLayout.CENTER);
        panel3.setBorder(BorderFactory.createTitledBorder(null, "Custom XML Configuration (Advanced)", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel3.add(panel4, BorderLayout.SOUTH);
        useDefaultButton = new JButton();
        useDefaultButton.setText("Use Default");
        panel4.add(useDefaultButton);
        browseButton = new JButton();
        browseButton.setText("Browse");
        panel4.add(browseButton);
        configFileURlField = new JTextField();
        configFileURlField.setColumns(40);
        configFileURlField.setEditable(true);
        configFileURlField.setText("");
        panel3.add(configFileURlField, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        panel1.add(panel5, BorderLayout.SOUTH);
        versionLabel = new JLabel();
        versionLabel.setText("Version 0.0.0");
        panel5.add(versionLabel, BorderLayout.WEST);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, BorderLayout.EAST);
        saveChangesButton = new JButton();
        saveChangesButton.setText("Save changes");
        panel6.add(saveChangesButton, BorderLayout.WEST);
        reloadConfigButton = new JButton();
        reloadConfigButton.setText("Reload Config");
        panel6.add(reloadConfigButton, BorderLayout.CENTER);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
