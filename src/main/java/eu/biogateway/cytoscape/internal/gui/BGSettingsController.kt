package eu.biogateway.cytoscape.internal.gui

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGDataModelController
import eu.biogateway.cytoscape.internal.util.Constants
import java.io.File
import javax.swing.JFileChooser
import java.util.prefs.Preferences
import javax.swing.filechooser.FileFilter

class BGSettingsController {
    private var preferences = Preferences.userRoot().node(javaClass.name)
    private var settings = BGServiceManager.dataModelController.settings

    private var view: BGSettingsView

    init {
        view = BGSettingsView(this)
        loadParameters()
    }

    fun browseForConfigFile() {
        val lastDir = preferences.get(Constants.BG_PREFERENCES_LAST_FOLDER, File(".").absolutePath)

        val chooser = when (lastDir != null) {
            true -> JFileChooser(lastDir)
            false -> JFileChooser()
        }

        val filter = object : FileFilter() {
            override fun getDescription(): String {
                return "Biogateway XML Config File"
            }

            override fun accept(f: File): Boolean {
                if (f.name.toLowerCase().endsWith(Constants.BG_CONFIG_FILE_EXTENSION)) return true
                if (f.isDirectory) return true
                return false
            }
        }
        chooser.fileFilter = filter
        val choice = chooser.showOpenDialog(view.mainFrame)
        if (choice == JFileChooser.APPROVE_OPTION) {
            preferences.put(Constants.BG_PREFERENCES_LAST_FOLDER, chooser.selectedFile.parent)

            val path = chooser.selectedFile.absolutePath
            // TODO: Store new path
            println("Setting path: " + path)

            setConfigFilePath(path)
            loadParameters()
        }

        // Do nothing?
    }

    fun useDefaults() {
        settings.configXMLFilePath = ""
        settings.saveParameters()
        loadParameters()
    }


    fun reloadConfigFile() {
        BGServiceManager.dataModelController = BGDataModelController()
        BGServiceManager.controlPanel?.setupTreePanel()
    }

    fun setConfigFilePath(path: String) {
        settings.configXMLFilePath = path
        settings.saveParameters()
    }

    private fun loadParameters() {
        view.setConfigFileURlFieldText(settings.configXMLFilePath)
    }
}