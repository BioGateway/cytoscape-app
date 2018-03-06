package org.cytoscape.biogwplugin.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

class BGRelationType(val uri: String, val name: String, val number: Int, val defaultGraphName: String? = null, val arbitraryLength: Boolean = false, val directed: Boolean = true, val expandable: Boolean = false, val fromType: BGNodeType? = null, val toType: BGNodeType? = null ) {

    val description: String get() {
        return when (defaultGraphName != null && defaultGraphName.isNotEmpty()) {
            true -> defaultGraphName!!.toUpperCase() + ": "+name
            false -> name
        }
    }

    val identifier: String get() {
        return when (defaultGraphName.isNullOrEmpty()) {
            true -> uri
            false -> defaultGraphName+":"+uri
        }
    }

    override fun toString(): String {
        return description
    }

    val sparqlIRI: String get() {
        return when (arbitraryLength) {
            true -> "<$uri>*"
            false -> "<$uri>"
        }
    }
}