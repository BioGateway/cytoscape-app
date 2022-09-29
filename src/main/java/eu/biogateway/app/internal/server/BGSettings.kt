package eu.biogateway.app.internal.server

import eu.biogateway.app.internal.util.Constants
import java.util.prefs.Preferences

class BGSettings {
    private val PREFID_BIOGWSTYLEDEFAULT = "useBioGatewayLayoutStyleAsDefault"
    private val PREFID_CONFIGURL = "configXMLFilePath"
    private val PREFID_DB_VERSION = "selectedDBVersion"


    var useBioGatewayLayoutStyleAsDefault: Boolean
    var configXMLFilePath: String
    var databaseVersion: Int
    var availableVersions = arrayOf<Int>()

    private val preferences = Preferences.userRoot().node(javaClass.name)

    init {
        configXMLFilePath = preferences.get(PREFID_CONFIGURL, Constants.BG_CONFIG_FILE_URL)
        databaseVersion = preferences.getInt(PREFID_DB_VERSION, 0)
        useBioGatewayLayoutStyleAsDefault = !preferences.get(PREFID_BIOGWSTYLEDEFAULT, "true").equals("false")
    }

    fun saveParameters() {
        val styleDefaultString = when (useBioGatewayLayoutStyleAsDefault) {
            true -> "true"
            false -> "false"
        }
        preferences.put(PREFID_BIOGWSTYLEDEFAULT, styleDefaultString)
        preferences.put(PREFID_CONFIGURL, configXMLFilePath)
        preferences.putInt(PREFID_DB_VERSION, databaseVersion)
    }
}