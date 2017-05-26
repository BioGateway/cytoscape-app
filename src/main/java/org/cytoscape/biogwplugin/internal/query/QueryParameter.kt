package org.cytoscape.biogwplugin.internal.query

import java.util.HashMap

class QueryParameter(var id: String, var name: String, var type: QueryParameter.ParameterType) {

    enum class ParameterType {
        TEXT, CHECKBOX, COMBOBOX, UNIPROT_ID, ONTOLOGY, OPTIONAL_URI
    }

    var value: String? = null
    var options: HashMap<String, String> = HashMap<String, String>()

    fun addOption(name: String, value: String) {
        options.put(name, value)
    }
}
