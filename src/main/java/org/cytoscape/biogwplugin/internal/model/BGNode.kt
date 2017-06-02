package org.cytoscape.biogwplugin.internal.model

import org.cytoscape.model.CyNode

/**
 * Created by sholmas on 26/05/2017.
 */

class BGNode(val uri: String) {
    var name: String? = null
    var description: String? = null

    // This is used to keep track of the CyNodes using data from this BGNode, so they can be updated if needed.
    var cyNodes = ArrayList<CyNode>()

    constructor(uri: String, name: String): this(uri) {
        this.name = name
    }

    constructor(uri: String, name: String, description: String): this(uri, name) {
        this.description = description
    }

}