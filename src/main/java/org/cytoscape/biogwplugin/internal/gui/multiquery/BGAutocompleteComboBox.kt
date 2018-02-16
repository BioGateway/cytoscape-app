package org.cytoscape.biogwplugin.internal.gui.multiquery

import org.cytoscape.biogwplugin.internal.server.BGDictEndpoint
import org.cytoscape.biogwplugin.internal.server.SearchSuggestion
import org.cytoscape.biogwplugin.internal.server.Suggestion
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.JTextComponent
import java.awt.event.*
import java.util.ArrayList

class BGAutocompleteComboBox(private val typeComboBox: JComboBox<String>, private val endpoint: BGDictEndpoint) : JComboBox<Suggestion>(DefaultComboBoxModel<Suggestion>()) {

    private val comboBoxModel: DefaultComboBoxModel<Suggestion>

    var selectedUri: String? = null // This should be set when an item is selected.

    private val searchSuggestion = SearchSuggestion()
    private val searchBoxEditorComponent: JTextComponent

    var text: String get() {
       return searchSuggestion.searchString
    }
    set(value) {
        searchSuggestion.searchString = value
    }

    init {
        this.comboBoxModel = this.model as DefaultComboBoxModel<Suggestion>
        searchBoxEditorComponent = this.getEditor().editorComponent as JTextComponent
        this.preferredSize = Dimension(250, 20)

        val self = this

        this.addActionListener {
            val selectedSuggestion = self.selectedItem as Suggestion
            if (selectedSuggestion !== searchSuggestion) {
                selectSuggestion(selectedSuggestion)
            }
        }

        this.setEditable(true)

        val searchKeyListener = object : KeyAdapter() {

            override fun keyReleased(e: KeyEvent) {
                val key = e.keyChar
                if (!(Character.isLetterOrDigit(key) || Character.isSpaceChar(key))) return
                val text = searchBoxEditorComponent.text
                updateSearchComboBoxModel(text)


                super.keyReleased(e)
            }
        }
        this.getEditor().editorComponent.addKeyListener(searchKeyListener)
        comboBoxModel.addElement(searchSuggestion)
    }

    private fun updateSearchComboBoxModel(text: String) {

        searchSuggestion.searchString = text

        val suggestions = searchForTerm(text)

        val selectedItem = this.selectedItem
        println(selectedItem)

        for (i in comboBoxModel.size - 1 downTo 1) {
            comboBoxModel.removeElementAt(i)
        }

        for (i in suggestions.indices) {
            comboBoxModel.addElement(suggestions[i])
        }

        this.showPopup()

    }

    private fun searchForTerm(term: String): ArrayList<Suggestion> {

        // TODO: Do a better typing than just lowercasing.
        val type = (typeComboBox.selectedItem as String).toLowerCase()

        return if (type == "go-term") {
            endpoint.searchForLabel(term, type, 10)
        } else endpoint.searchForPrefix(term, type, 10)
    }

    private fun selectSuggestion(suggestion: Suggestion) {
        val prefLabel = suggestion.prefLabel
        val uri = suggestion._id
        searchSuggestion.searchString = prefLabel
        selectedUri = uri
        this.hidePopup()
        this.selectedIndex = 0
        println("Selected $prefLabel with URI: $uri")
    }
}
