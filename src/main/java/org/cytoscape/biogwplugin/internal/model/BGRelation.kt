package org.cytoscape.biogwplugin.internal.model

class BGRelation(var fromNode: BGNode, val relationType: BGRelationType, var toNode: BGNode) {

    val metadata = BGRelationMetadata(relationType.uri)

    // Extra data field for helping sorting the relations in the result view.
    var extraTableData = ArrayList<Any>()

//    fun stringArray(): Array<String> {
//        return arrayOf(fromNode.uri, relationType.uri, toNode.uri)
//    }

    fun asArray(): Array<Any> {
        val fromNodeName = fromNode.name ?: fromNode.uri
        val toNodeName = toNode.name ?: toNode.uri

        val array = mutableListOf<Any>(fromNodeName, relationType.name, toNodeName)
        array.addAll(extraTableData)
        return array.toTypedArray()
    }

    val edgeIdentifier: String
        get() = fromNode.uri+"_"+relationType.identifier+"_"+toNode.uri

    val reverseEdgeIdentifier: String
        get() = toNode.uri+"_"+relationType.identifier+"_"+fromNode.uri


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
        val hashCode = this.edgeIdentifier.hashCode()
        return this.edgeIdentifier.hashCode()
    }
}

class BGRelationMetadata(val relationTypeUri: String) {
    var sourceGraph: String? = null
    var pubmedUris = HashSet<String>()
    val confidence: Double? = null
}