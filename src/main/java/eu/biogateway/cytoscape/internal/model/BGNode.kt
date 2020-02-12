package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.parser.getDescription
import eu.biogateway.cytoscape.internal.parser.getName
import eu.biogateway.cytoscape.internal.parser.getUri
import eu.biogateway.cytoscape.internal.server.BGSuggestion
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode

open class BGNode {

    val uri: String
    var isLoaded: Boolean = false
    val type: BGNodeType
    var name: String get() {
        if (field.isNullOrEmpty()) {
            return this.generateName()
        } else {
            return field
        }}

    var reviewed = false

    var description: String? = null
    var taxon: String? = null

    var metadata = HashMap<String, BGNodeMetadata>()

    // This is used to keep track of the CyNodes using data from this BGNode, so they can be updated if needed.
    var cyNodes: ArrayList<CyNode>

    constructor(inputUri: String) {

        var uri = inputUri

        // TODO: THIS IS A HACK!
        if (uri.startsWith("ECO:")) {
            uri = "http://identifiers.org/" + inputUri
        } else if (uri.startsWith("PubMed:")) {
            uri = "http://identifiers.org/pubmed/" + inputUri.removePrefix("PubMed:")
        }
        this.uri = uri
        this.cyNodes = ArrayList<CyNode>()
        if (!uri.startsWith("http")) {
            this.name = uri
        } else {
            this.name = ""
        }
        type = BGNode.static.nodeTypeForUri(uri)
    }

    constructor(suggestion: BGSuggestion) : this(suggestion._id) {
        this.name = suggestion.prefLabel
        this.description = suggestion.definition
        this.taxon = suggestion.taxon
        this.reviewed = suggestion.reviewed
    }

    constructor(uri: String, name: String) : this(uri) {
        this.name = name
    }

    constructor(uri: String, name: String, description: String) : this(uri, name) {
        this.description = description
    }

    constructor(uri: String, name: String, description: String, taxon: String) : this(uri, name, description) {
        this.taxon = taxon
    }

    constructor(uri: String, name: String, description: String, reviewed: Boolean) : this(uri, name, description) {
        this.reviewed = reviewed
    }

    // TODO: Check that these values exist! The URI table might not even be present!
    constructor(cyNode: CyNode, network: CyNetwork): this(cyNode.getUri(network)) {
        this.description = cyNode.getDescription(network) ?: ""
        this.name = cyNode.getName(network) ?: ""
    }

    fun generateName(): String {

        // TODO: Remove this code.
        /*
        if (type == BGNodeType.Pubmed) {
            return "Pubmed ID " + uri.substringAfterLast("/")
        }
        if (type == BGNodeType.TFTG) {
            val suffix = uri.substringAfterLast("/")
            val typeName = suffix.substringAfterLast("#")
            return typeName
        }
        if (type == BGNodeType.GOA) {
            val suffix = uri.substringAfterLast("GOA_")
            val parts = suffix.split("-")
            if (parts.size == 3) {
                return "GO: " + parts[1]
            }
        }*/

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

    object static {
        fun nodeTypeForUri(uri: String): BGNodeType {
            val matchingTypes = BGServiceManager.config.nodeTypes.values.filter { it.uriPattern != null && uri.contains(it.uriPattern) }
            if (matchingTypes.size == 1) return matchingTypes.first()
            val defaultTypes = matchingTypes.filter { it.default }
            if (defaultTypes.size == 1) return defaultTypes.first()
            return BGNodeType.UNDEFINED
        }
    }
}