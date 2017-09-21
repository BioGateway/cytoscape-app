package org.cytoscape.biogwplugin.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

class BGRelationType(val uri: String, val name: String, val number: Int, val defaultGraphName: String? = null) {

    val description: String get() {
        return when (defaultGraphName != null && defaultGraphName.isNotEmpty()) {
            true -> defaultGraphName!!.toUpperCase() + ": "+name
            false -> name
        }
    }

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