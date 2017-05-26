package org.cytoscape.biogwplugin.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

class BGNode(val uri: String) {
    var name: String? = null
    var description: String? = null

    constructor(uri: String, name: String): this(uri) {
        this.name = name
    }

    constructor(uri: String, name: String, description: String): this(uri, name) {
        this.description = description
    }

}