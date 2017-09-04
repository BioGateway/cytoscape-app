package org.cytoscape.biogwplugin.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

class BGRelationType(val uri: String, val description: String) {

    companion object {
        fun createRelationTypeHashMapFromArrayList(list: ArrayList<BGRelationType>): HashMap<String, BGRelationType> {
            var map = list.fold(HashMap<String, BGRelationType>(), {
                acc, bgRelationType ->
                acc.put(bgRelationType.uri, bgRelationType)
                return acc
            })
            return map
        }
    }
}

class BGRelation(val fromNode: BGNode, val relationType: BGRelationType, val toNode: BGNode) {

    fun stringArray(): Array<String> {
        return arrayOf(fromNode.uri, relationType.uri, toNode.uri)
    }

    fun nameStringArray(): Array<String> {
        val fromNodeName = fromNode.name ?: fromNode.uri
        val toNodeName = toNode.name ?: toNode.uri
        return arrayOf(fromNodeName, relationType.description, toNodeName)
    }

    val edgeIdentifier: String
        get() = fromNode.uri+"_"+relationType.uri+"_"+toNode.uri

    override fun toString(): String {
        val fromNodeName = fromNode.description ?: fromNode.uri
        val toNodeName = toNode.description ?: toNode.uri
        return fromNodeName + " -> " + relationType.description + " -> " + toNodeName
    }

    override fun equals(other: Any?): Boolean {
        val equal = this.toString().equals(other.toString())
        return equal
    }

    override fun hashCode(): Int {
        return this.toString().hashCode()
    }
}