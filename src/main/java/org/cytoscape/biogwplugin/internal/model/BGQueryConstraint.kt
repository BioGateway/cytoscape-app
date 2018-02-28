package org.cytoscape.biogwplugin.internal.model

class BGQueryConstraint(val id: String, val label: String, val inputType: InputType) {

    enum class InputType {
        COMBOBOX, TEXT, NUMBER
    }
    enum class ActionParameter {
        FIRST, LAST, BOTH
    }

    class ComboBoxOption(val label: String, val value: String) {
        override fun toString(): String { return label }
    }
    class ConstraintAction(val parameter: ActionParameter, val graph: String, val relationTypes: Collection<BGRelationType>, val sparqlTemplate: String)

    val options = ArrayList<ComboBoxOption>()
    val actions = ArrayList<ConstraintAction>()

    fun getOptionNames(): Array<String> {
        return options.map { it.label }.toTypedArray()
    }
}