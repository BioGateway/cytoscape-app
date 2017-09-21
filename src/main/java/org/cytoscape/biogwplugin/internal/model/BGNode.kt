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
    Any("Undefined type") }

open class BGNode {

    val uri: String
    var isLoaded: Boolean = false

    constructor(uri: String) {
        this.uri = uri
        this.cyNodes = ArrayList<CyNode>()
        if (!uri.startsWith("http")) {
            this.name = uri
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

    fun nameStringArray(): Array<String> {
        return arrayOf(this.uri, this.name ?: "", this.description ?: "", this.taxon ?: "")
    }
}