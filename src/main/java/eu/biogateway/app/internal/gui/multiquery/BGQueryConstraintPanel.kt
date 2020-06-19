package eu.biogateway.app.internal.gui.multiquery

import eu.biogateway.app.internal.model.BGQueryConstraint
import java.awt.FlowLayout
import java.util.ArrayList
import javax.swing.*

class InvalidInputValueException(override var message: String): Exception(message)

class BGQueryConstraintPanel(val constraints: HashSet<BGQueryConstraint>): JPanel() {

    class ConstraintUIComponent(val constraint: BGQueryConstraint, val component: JComponent, val checkBox: JCheckBox)
    class ConstraintValue(val stringValue: String, val isEnabled: Boolean)

    private val constraintUIComponents = ArrayList<ConstraintUIComponent>()


    init {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)

        // For each constraint:
        for (constraint in constraints) {

            // Create the label.
            //val label = JLabel(constraint.label)
            //this.add(label)
            // Check the type.
            val panel = JPanel(FlowLayout(FlowLayout.LEFT))

            val checkBox = JCheckBox(constraint.label+":")
            panel.add(checkBox)
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
                BGQueryConstraint.InputType.BOOLEAN -> {
                    JLabel(constraint.label)
                }
            }
            panel.add(inputComponent)
            this.add(panel)
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

    fun validateConstraints(): String? {
        constraintUIComponents.filter { it.checkBox.isSelected }
                .forEach {
                    when (it.constraint.inputType) {
                        BGQueryConstraint.InputType.COMBOBOX -> {

                        }
                        BGQueryConstraint.InputType.NUMBER -> {
                            val textField = it.component as? JTextField ?: throw Exception()
                            try {
                                val number = textField.text.toDouble()
                            } catch (exception: NumberFormatException) {
                                return "The "+it.constraint.label+" constraint is enabled, but isn't a valid number!"
                            }
                        }
                        BGQueryConstraint.InputType.TEXT -> {
                            val textField = it.component as? JTextField ?: throw Exception()
                            if (textField.text.length == 0) return "The "+it.constraint.label+" constraint is enabled, but no value is set!"
                        }
                    }
                }
        return null
    }

    fun getConstraintValues(): HashMap<BGQueryConstraint, BGQueryConstraint.ConstraintValue> {

        val values = HashMap<BGQueryConstraint, BGQueryConstraint.ConstraintValue>()

        for (constraintComponent in constraintUIComponents) {
            val type = constraintComponent.constraint.inputType
            val enabled = constraintComponent.checkBox.isSelected

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
                    val text = textField.text
                    if (text.length == 0 && enabled) {
                        throw InvalidInputValueException("The "+constraintComponent.constraint.label+" constraint is enabled, but no value is set!")
                    }
                    "\""+text+"\""
                }
                BGQueryConstraint.InputType.BOOLEAN -> {
                    ""
                }
            }

            values[constraintComponent.constraint] = BGQueryConstraint.ConstraintValue(inputValue, enabled)
        }
        return values
    }
}