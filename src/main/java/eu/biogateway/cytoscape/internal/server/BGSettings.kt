package eu.biogateway.cytoscape.internal.server

import eu.biogateway.cytoscape.internal.util.Constants
import java.util.prefs.Preferences

class BGSettings {
    private val PREFID_BIOGWSTYLEDEFAULT = "useBioGatewayLayoutStyleAsDefault"
    private val PREFID_CONFIGURL = "configXMLFilePath"

    var useBioGatewayLayoutStyleAsDefault: Boolean
    var configXMLFilePath: String

    private val preferences = Preferences.userRoot().node(javaClass.name)

    init {
        configXMLFilePath = preferences.get(PREFID_CONFIGURL, Constants.BG_CONFIG_FILE_URL)
        useBioGatewayLayoutStyleAsDefault = !preferences.get(PREFID_BIOGWSTYLEDEFAULT, "true").equals("false")
    }

    fun saveParameters() {
        val styleDefaultString = when (useBioGatewayLayoutStyleAsDefault) {
            true -> "true"
            false -> "false"
        }
        preferences.put(PREFID_BIOGWSTYLEDEFAULT, styleDefaultString)
        preferences.put(PREFID_CONFIGURL, configXMLFilePath)
    }
}