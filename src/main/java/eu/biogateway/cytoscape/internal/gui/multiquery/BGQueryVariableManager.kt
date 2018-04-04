package eu.biogateway.cytoscape.internal.gui.multiquery

import eu.biogateway.cytoscape.internal.util.Constants
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

class BGQueryVariableManager {
    val possibleVariables = "ABCDEFGHIJKLMNOPQRST".toCharArray().map { it.toString() }
    var usedVariables = HashMap<JComboBox<String>, String>()
    var URIcomboBoxes = HashSet<JComboBox<String>>()
    var previouslyAssignedVariable: String = "A"

    fun getNextFreeVariable(): String? {
        for (variable in possibleVariables) {
            if (!usedVariables.values.contains(variable)) {
                // The variable is not taken by any component!
                return variable
            }
        }
        return null
    }

    private fun getUsedVariables(): Array<String> {
        return usedVariables.values.toHashSet().sorted().toTypedArray()
    }

    fun unRegisterUseOfVariableForComponent(jComboBox: JComboBox<String>) {
        usedVariables.remove(jComboBox)
        updateComboBoxModels()
    }

    fun registerUseOfVariableForComponent(variable: String, jComboBox: JComboBox<String>) {
        usedVariables[jComboBox] = variable
        previouslyAssignedVariable = variable

        updateComboBoxModels()
    }

    private fun updateComboBoxModels() {
        val comboBoxes = usedVariables.keys + URIcomboBoxes
        for (comboBox in comboBoxes) {
            val model = comboBox.model as DefaultComboBoxModel<String>
            val selected = model.selectedItem
            val lastIndex = model.size -1
            var containsNextVariable = false
            if (lastIndex < 1) {
                return
            }

            for (i in lastIndex.downTo(1)) {
                // Iterating backwards because we are deleting elements.
                val element = model.getElementAt(i)
                if (element != selected && !usedVariables.values.contains(element) && element != getNextFreeVariable()) {
                    model.removeElementAt(i)
                }
                if (element == getNextFreeVariable()) {
                    containsNextVariable = true
                }
            }
            if (!containsNextVariable) {
                model.addElement(getNextFreeVariable())
            }
        }
    }

    fun getShownVariables(): Array<String> {
        var usedVariables = getUsedVariables().map { it.toString() }.toTypedArray()
        var nextFreeChar = getNextFreeVariable().toString()
        var shownVariables = arrayOf(Constants.BG_QUERYBUILDER_ENTITY_LABEL) + usedVariables
        nextFreeChar.let {
            shownVariables += it
        }
        return shownVariables
    }
}