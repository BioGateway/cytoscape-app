package eu.biogateway.cytoscape.internal.server

import eu.biogateway.cytoscape.internal.model.BGNode

open class BGSuggestion(val _id: String, val prefLabel: String, val synonyms: Array<String>? = null, val definition: String? = null, val taxon: String? = null, val identifiers: Array<String>? = null, val reviewed: Boolean = false) {
    override fun toString(): String {
        var string = prefLabel
        if (synonyms != null) {
            string += ": "
            for (label in synonyms) {
                string += "$label, "
            }
            string = string.removeSuffix(", ")
        }
        if (taxon != null) {
            string += " - " + taxon.replace("http://purl.bioontology.org/ontology/NCBITAXON/", "TAXID: ") + "\n"
        }
        if (definition != null) {
            string += " - " + definition
        }
       return  string
    }

    constructor(node: BGNode): this(node.uri, node.name, null, node.description, node.taxon)
}
