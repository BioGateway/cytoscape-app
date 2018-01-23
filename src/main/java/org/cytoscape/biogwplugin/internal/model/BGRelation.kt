package org.cytoscape.biogwplugin.internal.model

class BGRelation(var fromNode: BGNode, val relationType: BGRelationType, var toNode: BGNode) {

    val metadata = BGRelationMetadata(relationType.uri)
    val edgeIdentifier: String
        get() = fromNode.uri+";"+relationType.identifier+";"+toNode.uri
    val reverseEdgeIdentifier: String
        get() = toNode.uri+";"+relationType.identifier+";"+fromNode.uri

    // Extra data field for helping sorting the relations in the result view.
    var extraTableData = ArrayList<Any>()

    fun asArray(): Array<Any> {
        val fromNodeName = fromNode.name ?: fromNode.uri
        val toNodeName = toNode.name ?: toNode.uri

        val array = mutableListOf<Any>(fromNodeName, relationType.name, toNodeName)
        array.addAll(extraTableData)
        return array.toTypedArray()
    }



    override fun toString(): String {
        val fromNodeName = fromNode.description ?: fromNode.uri
        val toNodeName = toNode.description ?: toNode.uri
        return fromNodeName + " -> " + relationType.name + " -> " + toNodeName
    }

    override fun equals(other: Any?): Boolean {
        return this.edgeIdentifier.equals(other.toString())
    }

    override fun hashCode(): Int {
        return this.edgeIdentifier.hashCode()
    }
}

class BGRelationMetadata(val relationTypeUri: String) {
    var sourceGraph: String? = null
    var source: String? = null
    var pubmedUris = HashSet<String>()
    var evidenceCode: String? = null
    val confidence: Double? = null
}