package eu.biogateway.app.internal.model

import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.gui.multiquery.InvalidInputValueException
import javax.swing.JOptionPane

class BGQueryConstraint(val id: String, val label: String, val inputType: InputType, val columns: Int? = null) {

    class ConstraintValue(val stringValue: String, val isEnabled: Boolean)

    var enabledByDefault = false

    enum class InputType {
        COMBOBOX, TEXT, NUMBER, BOOLEAN
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

    companion object {

        fun generateTaxonConstraintValue(): ConstraintValue? {
            BGTaxon.generateTaxonConstraint()?.let {
                return ConstraintValue(stringValue = it, isEnabled = true)
            }
            return null
        }

        fun generateConstraintQueries(triples: Collection<Triple<String, BGRelationType, String>>): String {
            val constraintValues = BGServiceManager.controlPanel?.queryConstraintPanel?.getConstraintValues() ?: return ""
            BGServiceManager.config.taxonConstraint?.let { constraint ->
                generateTaxonConstraintValue()?.let { value ->
                    constraintValues[constraint] = value
                }
            }
            return generateConstraintQueries(constraintValues, triples)
        }


        fun generateConstraintQueries(constraintValues: HashMap<BGQueryConstraint, ConstraintValue>, triples: Collection<Triple<String, BGRelationType, String>>): String {

            // Graphs are key, then all the queries on the graphs.
            val constraintQueries = HashMap<String, HashSet<String>>()
            fun addToQueries(key: String, sparql: String) {
                if (!constraintQueries.containsKey(key)) {
                    constraintQueries[key] = HashSet()
                }
                constraintQueries[key]?.add(sparql)
            }

            var sourceConstraintCounter = 1
            var sourceConstraintFilters = ""


            for (triple in triples) {
                val graph = triple.second.defaultGraphURI ?: continue
                val pair = BGDatasetSource.generateSourceConstraint(triple.second, triple.first, triple.third, sourceConstraintCounter)
                sourceConstraintCounter++

                pair?.let {
                    sourceConstraintFilters += it.first
                    addToQueries(graph, it.second)
                }

            }

            try {
                var uniqueIdNumber = 1

                for ((constraint, value) in constraintValues) {
                    // Skip this if it's disabled.
                    if (!value.isEnabled) continue

                    for (triple in triples) {
                        for (action in constraint.actions) {
                            if (!action.relationTypes.contains(triple.second)) continue
                            val sparql = action.sparqlTemplate
                                    .replace("@first", triple.first)
                                    .replace("@last", triple.third)
                                    .replace("@value", value.stringValue)
                                    .replace("@uniqueId", uniqueIdNumber.toString())
                            uniqueIdNumber++

                            if (action.parameter == BGQueryConstraint.ActionParameter.FIRST && triple.first.startsWith("?")) {
                                addToQueries(action.graph, sparql)
                            }
                            if (action.parameter == BGQueryConstraint.ActionParameter.LAST && triple.third.startsWith("?")) {
                                addToQueries(action.graph, sparql)
                            }
                            if (action.parameter == BGQueryConstraint.ActionParameter.BOTH &&
                                    (triple.third.startsWith("?") || triple.first.startsWith("?"))) {
                                addToQueries(action.graph, sparql)
                            }
                        }
                    }
                }


                if (constraintQueries.count() > 0) {
                    val constraintHeader = "\n#QueryConstraints:\n" + constraintValues
                            .filter { it.value.isEnabled }
                            .map { "#Constraint: " + it.key.id + "=" + it.value.stringValue + "\n" }
                            .fold("") { acc, s -> acc + s }

                    var constraintQueryString = "\n\n"

                    for (graph in constraintQueries.keys) {

                        val lines = constraintQueries[graph] ?: continue

                        constraintQueryString += "\n" +
                                "GRAPH <" + graph + "> { \n"
                        for (line in lines) {
                            constraintQueryString += line + "\n"
                        }
                        constraintQueryString += "}\n"
                    }
                    return sourceConstraintFilters + constraintHeader + constraintQueryString
                } else return ""
            } catch (exception: InvalidInputValueException) {
                JOptionPane.showMessageDialog(null, exception.message, "Invalid query constraints", JOptionPane.ERROR_MESSAGE)
                return ""
            }
        }
    }
}