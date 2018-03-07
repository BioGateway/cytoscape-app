package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.model.BGQueryConstraint
import java.awt.FlowLayout
import java.awt.TextField
import java.util.ArrayList
import javax.swing.*

class BGQueryConstraintPanel(val constraints: HashMap<String, BGQueryConstraint>): JPanel() {

    class ConstraintUIComponent(val constraint: BGQueryConstraint, val component: JComponent, val checkBox: JCheckBox)
    class ConstraintValue(val stringValue: String, val isEnabled: Boolean)

    private val constraintUIComponents = ArrayList<ConstraintUIComponent>()


    init {
        this.layout = FlowLayout(FlowLayout.LEFT)

        // For each constraint:
        for (constraint in constraints.values) {

            // Create the label.
            //val label = JLabel(constraint.label)
            //this.add(label)
            // Check the type.
            val checkBox = JCheckBox(constraint.label)
            this.add(checkBox)
            val columns = constraint.columns ?: 10

            val inputComponent: JComponent = when (constraint.inputType) {
                BGQueryConstraint.InputType.COMBOBOX -> {
                    JComboBox(constraint.getOptionNames())
                }
                BGQueryConstraint.InputType.TEXT -> {
                    JTextField(columns)
                }
                BGQueryConstraint.InputType.NUMBER -> {
                    JTextField(columns)
                }
            }
            this.add(inputComponent)

            this.constraintUIComponents.add(ConstraintUIComponent(constraint, inputComponent, checkBox))
        }
    }

    fun setConstraintValue(id: String, enabled: Boolean, value: String) {
        constraintUIComponents.filter { it.constraint.id == id }.forEach {
            it.checkBox.isSelected = enabled

            if (it.component is JTextField) {
                it.component.text = value
            } else if (it.component is JComboBox<*>) {
                val option = it.constraint.options.filter { it.value == value }.first()
                it.component.selectedItem = option.label
            }
        }

    }

    fun getConstraintValues(): HashMap<BGQueryConstraint, ConstraintValue> {

        val values = HashMap<BGQueryConstraint, ConstraintValue>()

        for (constraintComponent in constraintUIComponents) {
            val type = constraintComponent.constraint.inputType

            val inputValue: String = when (type) {
                BGQueryConstraint.InputType.COMBOBOX -> {
                    // Get the component as JComboBox.
                    val comboBox = constraintComponent.component as? JComboBox<*> ?: throw Exception("Invalid ComboBox component.")
                    val selectedValueString = comboBox.selectedItem as? String ?: throw Exception("Invalid ComboBox selection.")
                    constraintComponent.constraint.options.filter { it.label.equals(selectedValueString) }.first().value
                }
                BGQueryConstraint.InputType.NUMBER -> {
                    val textField = constraintComponent.component as? JTextField ?: throw Exception()
                    textField.text
                }
                BGQueryConstraint.InputType.TEXT -> {
                    val textField = constraintComponent.component as? JTextField ?: throw Exception()
                    "\""+textField.text+"\""
                }
            }

            val enabled = constraintComponent.checkBox.isSelected
            values[constraintComponent.constraint] = ConstraintValue(inputValue, enabled)
        }
        return values
    }
}