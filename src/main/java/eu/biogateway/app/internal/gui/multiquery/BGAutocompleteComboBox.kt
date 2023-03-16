package eu.biogateway.app.internal.gui.multiquery

import eu.biogateway.app.internal.model.BGNodeType
import eu.biogateway.app.internal.server.BGDictEndpoint
import eu.biogateway.app.internal.server.SearchSuggestion
import eu.biogateway.app.internal.server.BGSuggestion
import org.apache.commons.lang3.SystemUtils
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.JTextComponent
import java.awt.event.*
import java.util.ArrayList


class BGAutocompleteComboBox(private val endpoint: BGDictEndpoint, private val typeSource: () -> BGNodeType?) : JComboBox<BGSuggestion>(DefaultComboBoxModel<BGSuggestion>()) {

    private val comboBoxModel: DefaultComboBoxModel<BGSuggestion>

    var selectedUri: String? = null // This should be set when an item is selected.

    private val searchSuggestion = SearchSuggestion()
    private val searchBoxEditorComponent: JTextComponent

    var text: String get() {
       return searchSuggestion.searchString
    }
    set(value) {
        searchSuggestion.searchString = value
        searchBoxEditorComponent.text = value
        //this.selectedIndex = 0
    }

    init {
        this.comboBoxModel = this.model as DefaultComboBoxModel<BGSuggestion>
        searchBoxEditorComponent = this.getEditor().editorComponent as JTextComponent
        this.preferredSize = Dimension(250, 20)

        val self = this

        this.addActionListener {
            val selectedSuggestion = self.selectedItem as? BGSuggestion

            if ((self.selectedItem as? String).equals("")) {
                // Empty string is selected!
                print("Empty string is selected instead of SearchSuggestion!")
                searchSuggestion.searchString = ""
                this.selectedIndex = 0
            }

            if (selectedSuggestion != searchSuggestion && selectedSuggestion != null) {
                selectSuggestion(selectedSuggestion)
            } else if (selectedSuggestion != null && this.comboBoxModel.size > 1 && selectedSuggestion.prefLabel.isBlank()){
                this.selectedIndex = 1
                val topSuggestion = this.comboBoxModel.getElementAt(1)
                selectSuggestion(topSuggestion)
            }
        }

        this.setEditable(true)

        val searchKeyListener = object : KeyAdapter() {

            override fun keyReleased(e: KeyEvent) {
                val key = e.keyChar
                if ((Character.isLetterOrDigit(key) && !Character.isSpaceChar(key)) || e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE) {

                    val text = searchBoxEditorComponent.text

                    //self.maximumRowCount = 10
                    updateSearchComboBoxModel(text)
                }
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

        if (text.isEmpty()) {
            this.hidePopup()
        } else {
            this.showPopup()
        }

        searchBoxEditorComponent.select(text.length,text.length)

    }

    fun getNameForSelectedURI() {
        val uri = selectedUri ?: return
        val suggestion =  endpoint.getSuggestionForURI(uri)
        suggestion?.let {
            //selectSuggestion(suggestion)
            searchBoxEditorComponent.text = suggestion.prefLabel
        }
    }

    private fun searchForTerm(term: String): ArrayList<BGSuggestion> {

        val type = typeSource() ?: return ArrayList()

        return when (type.autocompleteType) {
            BGNodeType.BGAutoCompleteType.INFIX -> endpoint.searchForLabel(term, type.id.toLowerCase(), 20)
            BGNodeType.BGAutoCompleteType.PREFIX -> endpoint.searchForPrefix(term, type.id.toLowerCase(), 20)
            null -> throw Exception("Autocomplete is not supported for this type!")
        }
    }

    private fun selectSuggestion(suggestion: BGSuggestion) {
        val prefLabel = suggestion.prefLabel
        val uri = suggestion.uri
        searchSuggestion.searchString = prefLabel
        selectedUri = uri
        this.hidePopup()
        this.selectedIndex = 0
        println("Selected $prefLabel with URI: $uri")
    }


    override fun getSize(): Dimension {

        val dim = super.getSize()

        if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) {
            dim.width = 500
        }
        return dim
    }
}
