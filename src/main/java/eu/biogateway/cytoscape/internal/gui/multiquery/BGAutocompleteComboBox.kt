package eu.biogateway.cytoscape.internal.gui.multiquery

import eu.biogateway.cytoscape.internal.model.BGNodeType
import eu.biogateway.cytoscape.internal.server.BGDictEndpoint
import eu.biogateway.cytoscape.internal.server.SearchSuggestion
import eu.biogateway.cytoscape.internal.server.BGSuggestion
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.JTextComponent
import java.awt.event.*
import java.util.ArrayList

interface BGAutocompleteTypeProvider {
    fun provideType(): BGNodeType
}

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

        return if (type == BGNodeType.GOTerm || type == BGNodeType.Taxon || type == BGNodeType.Disease) {
            endpoint.searchForLabel(term, type.paremeterType.toLowerCase(), 20)
        } else endpoint.searchForPrefix(term, type.paremeterType.toLowerCase(), 20)
    }

    private fun selectSuggestion(suggestion: BGSuggestion) {
        val prefLabel = suggestion.prefLabel
        val uri = suggestion._id
        searchSuggestion.searchString = prefLabel
        selectedUri = uri
        this.hidePopup()
        this.selectedIndex = 0
        println("Selected $prefLabel with URI: $uri")
    }
}
