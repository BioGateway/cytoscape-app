package org.cytoscape.biogwplugin.internal.model

import org.cytoscape.model.CyNode

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGNodeType(val paremeterType: String) {
    Protein("Protein"),
    Gene("Gene"),
    GO("GO annotation"),
    Taxon("Taxon"),
    PPI("PPI"),
    GOA("GO Annotation"),
    TFTG("TF-TG Statement"),
    PUBMED("Pubmed URI"),
    Undefined("Undefined type") }

open class BGNode {

    val uri: String
    var isLoaded: Boolean = false
    val type: BGNodeType
    var name: String? = null
    var description: String? = null
    var taxon: String? = null

    // This is used to keep track of the CyNodes using data from this BGNode, so they can be updated if needed.
    var cyNodes: ArrayList<CyNode>

    constructor(inputUri: String) {

        var uri = inputUri

        // TODO: THIS IS A HACK!
        if (uri.startsWith("ECO:")) {
            uri = "http://identifiers.org/" + inputUri
        } else if (uri.startsWith("PubMed:")) {
            uri = "http://identifiers.org/pubmed/"+inputUri.removePrefix("PubMed:")
        }
        this.uri = uri
        this.cyNodes = ArrayList<CyNode>()
        if (!uri.startsWith("http")) {
            this.name = uri
        }
        type = when {
            uri.contains("uniprot") -> BGNodeType.Protein
            uri.contains("ncbigene") -> BGNodeType.Gene
            uri.contains("GO_") -> BGNodeType.GO
            uri.contains("NCBITaxon_") -> BGNodeType.Taxon
            uri.contains("intact") -> BGNodeType.PPI
            uri.contains("pubmed") -> BGNodeType.PUBMED
            uri.contains("GOA_") -> BGNodeType.GOA
            uri.contains("semantic-systems-biology.org/") -> BGNodeType.TFTG // TODO: Need a better identifier!
            else -> {
                BGNodeType.Undefined
            }
        }
    }

    constructor(uri: String, name: String): this(uri) {

        this.name = name
    }

    constructor(uri: String, name: String, description: String): this(uri, name) {
        this.description = description
    }
    constructor(uri: String, name: String, description: String, taxon: String): this(uri, name, description) {
        this.taxon = taxon
    }

    fun generateName(): String {

        if (type == BGNodeType.PUBMED) {
            return "Pubmed ID "+uri.substringAfterLast("/")
        }
        if (type == BGNodeType.TFTG) {
            val suffix = uri.substringAfterLast("/")
            val parts = suffix.split("_")
            if (parts.size == 4) {
                val label = parts[2] + " to " + parts[3] +" from PubmedId "+parts[0]
                return label
            }
        }
        if (type == BGNodeType.GOA) {
            val suffix = uri.substringAfterLast("GOA_")
            val parts = suffix.split("-")
            if (parts.size == 3) {
                return "GO: "+parts[1]
            }
        }

        if (uri.startsWith("http://")) {
            val suffix = uri.substringAfterLast("/")
            return suffix
        }

        return "Unnamed"
    }

    fun nameStringArray(): Array<String> {
        return arrayOf(this.uri, this.name ?: "", this.description ?: "", this.taxon ?: "")
    }

    override fun hashCode(): Int {
        return uri.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BGNode

        if (uri.hashCode() != other.uri.hashCode()) return false

        return true
    }
}