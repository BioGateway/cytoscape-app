package eu.biogateway.app.internal.query

import java.util.HashMap

class BGQueryParameter(var id: String, var name: String, var type: BGQueryParameter.ParameterType) {

    enum class ParameterType {
        TEXT, CHECKBOX, COMBOBOX, PROTEIN, GO_TERM, GENE, TAXON, OPTIONAL_URI, RELATION_COMBOBOX, RELATION_QUERY_ROW
    }

    class EnabledDependency(val dependingParameter: String, val isEnabled: Boolean, val forParameterValue: String)

    var value: String? = null
    var direction: BGRelationDirection? = null
    var dependency: EnabledDependency? = null
    var options: HashMap<String, String> = HashMap<String, String>()

    fun addOption(name: String, value: String) {
        options.put(name, value)
    }
}
