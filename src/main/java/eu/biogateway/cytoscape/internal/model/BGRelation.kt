package eu.biogateway.cytoscape.internal.model


open class BGPrimitiveRelation(val fromNodeUri: String, val relationType: BGRelationType, val toNodeUri: String) {
    var sourceGraph: String = ""
    val metadata = HashMap<BGRelationMetadataType, BGRelationMetadata>()
}

class BGRelation(val fromNode: BGNode, relationType: BGRelationType, val toNode: BGNode): BGPrimitiveRelation(fromNode.uri, relationType, toNode.uri) {


    val edgeIdentifier: String
        get() = fromNode.uri+"::"+relationType.identifier+"::"+toNode.uri
    val reverseEdgeIdentifier: String
        get() = toNode.uri+"::"+relationType.identifier+"::"+fromNode.uri

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

