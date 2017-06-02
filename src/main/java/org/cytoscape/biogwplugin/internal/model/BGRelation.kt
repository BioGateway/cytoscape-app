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

class BGRelation(val fromNode: BGNode, val relationType: BGRelationType, val toNode: BGNode)