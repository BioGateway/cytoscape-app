package eu.biogateway.cytoscape.internal.gui.multiquery

import eu.biogateway.cytoscape.internal.util.Constants
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

open class BGQueryVariable(val value: String) {
    val name: String get() = "Set $value"

    override fun toString(): String {
        return name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    class BGQueryEntity(value: String): BGQueryVariable(value) {
        override fun toString(): String {
            return value
        }
    }

    companion object {
        val Entity: BGQueryVariable = BGQueryEntity("Entity:")
    }
}

class BGQueryVariableManager {
    val possibleVariableValues = "ABCDEFGHIJKLMNOPQRST".toCharArray().map { it.toString() }
    val possibleVariables = possibleVariableValues.associateBy( {it}, {BGQueryVariable(it)} )
    var usedVariables = HashMap<JComboBox<BGQueryVariable>, BGQueryVariable>()
    var URIcomboBoxes = HashSet<JComboBox<BGQueryVariable>>()
    var previouslyAssignedVariable = possibleVariables.values.sortedBy { it.name }.first()

    fun getNextFreeVariable(): BGQueryVariable? {
        for (variable in possibleVariables.values) {
            if (!usedVariables.values.contains(variable)) {
                // The variable is not taken by any component!
                return variable
            }
        }
        return null
    }

    fun getVariable(value: String): BGQueryVariable? {
        return possibleVariables[value]
    }

    private fun getUsedVariables(): Array<BGQueryVariable> {
        return usedVariables.values.toHashSet().sortedBy{ it.name }.toTypedArray()
    }

    fun unRegisterUseOfVariableForComponent(jComboBox: JComboBox<BGQueryVariable>) {
        usedVariables.remove(jComboBox)
        updateComboBoxModels()
    }

    fun registerUseOfVariableForComponent(variable: BGQueryVariable, jComboBox: JComboBox<BGQueryVariable>) {
        usedVariables[jComboBox] = variable
        previouslyAssignedVariable = variable

        updateComboBoxModels()
    }

    private fun updateComboBoxModels() {
        val comboBoxes = usedVariables.keys + URIcomboBoxes
        for (comboBox in comboBoxes) {
            val model = comboBox.model as DefaultComboBoxModel<BGQueryVariable>
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

    fun getShownVariables(): Array<BGQueryVariable> {
        var usedVariables = getUsedVariables()
        var nextFreeChar = getNextFreeVariable()
        var shownVariables = arrayOf(BGQueryVariable.Entity) + usedVariables
        nextFreeChar?.let {
            shownVariables += it
        }
        return shownVariables
    }
}