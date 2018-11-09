package eu.biogateway.cytoscape.internal.server

import eu.biogateway.cytoscape.internal.model.BGNode

open class BGSuggestion(val _id: String, val prefLabel: String, val altLabel: String? = null, val definition: String? = null, val taxon: String? = null, val ensembl_id: String? = null, val hgnc_id: String? = null, val ncbi_id: String? = null, val reviewed: Boolean = false) {
    override fun toString(): String {
        var string = prefLabel
        if (altLabel != null) {
            string += ": " + altLabel
        }
        if (definition != null) {
            string += " - " + definition
        }
       return  string
    }

    constructor(node: BGNode): this(node.uri, node.name, null, node.description, node.taxon)

}