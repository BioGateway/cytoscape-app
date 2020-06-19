package eu.biogateway.app.internal.gui

import eu.biogateway.app.internal.model.BGNodeType
import java.awt.Color
import java.awt.Component
import javax.swing.*

interface BGColorableText {
    val textColor: Color
}

class BGNodeTypeComboBoxRenderer(val comboBox: JComboBox<BGNodeType>): JPanel(), ListCellRenderer<BGNodeType> {

    private val textPanel = JPanel()
    private val textLabel = JLabel()

    var acceptedNodeTypes: Collection<BGNodeType>? = null

    init {
        textPanel.add(this)
        textLabel.isOpaque = true
        textLabel.font = comboBox.font
        textPanel.add(textLabel)
    }

    override fun getListCellRendererComponent(list: JList<out BGNodeType>?, value: BGNodeType?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        if (isSelected) {
            background = list!!.selectionBackground
        } else {
            background = list!!.background
        }
        textLabel.background = background
        textLabel.text = value.toString()

        val color = if (acceptedNodeTypes?.contains(value) == true) Color.BLACK else Color.RED

        textLabel.foreground = color

        return textLabel
    }

}

class BGColorComboBoxRenderer(val comboBox: JComboBox<BGColorableText>): JPanel(), ListCellRenderer<BGColorableText> {

    private val textPanel = JPanel()
    private val textLabel = JLabel()

    init {
        textPanel.add(this)
        textLabel.isOpaque = true
        textLabel.font = comboBox.font
        textPanel.add(textLabel)
    }


    override fun getListCellRendererComponent(list: JList<out BGColorableText>?, value: BGColorableText?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        if (isSelected) {
            background = list!!.selectionBackground
        } else {
            background = list!!.background
        }
        textLabel.background = background
        textLabel.text = value?.toString()
        textLabel.foreground = value?.textColor

        return textLabel
    }

}

