package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.model.BGQueryConstraint
import org.intellij.lang.annotations.JdkConstants
import java.awt.FlowLayout
import javax.swing.*

class BGQueryConstraintPanel(val constraints: Collection<BGQueryConstraint>): JPanel() {

    init {
        this.layout = FlowLayout(FlowLayout.LEFT)

        // For each constraint:
        for (constraint in constraints) {

            // Create the label.
            val label = JLabel(constraint.label)
            this.add(label)
            // Check the type.

            val inputComponent: JComponent = when (constraint.inputType) {
                BGQueryConstraint.InputType.COMBOBOX -> {
                    JComboBox(constraint.getOptionNames())
                }
                BGQueryConstraint.InputType.TEXT -> {
                    JTextField(20)
                }
                BGQueryConstraint.InputType.NUMBER -> {
                    JTextField(5)
                }
            }
            this.add(inputComponent)
        }
    }
}