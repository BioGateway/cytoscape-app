package org.cytoscape.biogwplugin.internal.model

class BGRelation(val fromNode: BGNode, val relationType: BGRelationType, val toNode: BGNode) {

    val metadata = BGRelationMetadata()

    fun stringArray(): Array<String> {
        return arrayOf(fromNode.uri, relationType.uri, toNode.uri)
    }

    fun nameStringArray(): Array<String> {
        val fromNodeName = fromNode.name ?: fromNode.uri
        val toNodeName = toNode.name ?: toNode.uri
        return arrayOf(fromNodeName, relationType.name, toNodeName)
    }

    val edgeIdentifier: String
        get() = fromNode.uri+"_"+relationType.uri+"_"+toNode.uri

    override fun toString(): String {
        val fromNodeName = fromNode.description ?: fromNode.uri
        val toNodeName = toNode.description ?: toNode.uri
        return fromNodeName + " -> " + relationType.name + " -> " + toNodeName
    }

    override fun equals(other: Any?): Boolean {
        val equal = this.toString().equals(other.toString())
        return equal
    }

    override fun hashCode(): Int {
        return this.toString().hashCode()
    }
}

class BGRelationMetadata() {
    var sourceGraph: String? = null
    var pubmedUrl: String? = null
}