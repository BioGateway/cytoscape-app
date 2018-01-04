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
    Undefined("Undefined type") }

open class BGNode {

    val uri: String
    var isLoaded: Boolean = false
    val type: BGNodeType

    constructor(uri: String) {
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

            else -> {
                BGNodeType.Undefined
            }
        }
    }


    var name: String? = null
    var description: String? = null
    var taxon: String? = null


    // This is used to keep track of the CyNodes using data from this BGNode, so they can be updated if needed.
    var cyNodes: ArrayList<CyNode>

    fun syncWithCyNodes() {

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