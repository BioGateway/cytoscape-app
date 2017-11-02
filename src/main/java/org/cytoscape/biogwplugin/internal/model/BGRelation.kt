package org.cytoscape.biogwplugin.internal.model

class BGRelation(var fromNode: BGNode, val relationType: BGRelationType, var toNode: BGNode) {

    val metadata = BGRelationMetadata(relationType.uri)

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

    val reverseEdgeIdentifier: String
        get() = toNode.uri+"_"+relationType.uri+"_"+fromNode.uri


    override fun toString(): String {
        val fromNodeName = fromNode.description ?: fromNode.uri
        val toNodeName = toNode.description ?: toNode.uri
        return fromNodeName + " -> " + relationType.name + " -> " + toNodeName
    }

    override fun equals(other: Any?): Boolean {
        val equal = this.edgeIdentifier.equals(other.toString())
        return equal
    }

    override fun hashCode(): Int {
        return this.edgeIdentifier.hashCode()
    }
}

class BGRelationMetadata(val relationTypeUri: String) {
    var sourceGraph: String? = null
    var pubmedUris = HashSet<String>()
    val confidence: Double? = null
}