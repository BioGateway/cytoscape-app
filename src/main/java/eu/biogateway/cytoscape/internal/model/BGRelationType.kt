package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.gui.BGColorableText
import java.awt.Color

class BGExternalRelationType(name: String): BGRelationType("External", name, 0)


open class BGRelationType(val uri: String, val name: String, val number: Int, override val textColor: Color = Color.BLACK, val defaultGraph: BGGraph? = null, val arbitraryLength: Boolean = false, val directed: Boolean = true, val expandable: Boolean = false, val fromType: BGNodeTypeNew? = null, val toType: BGNodeTypeNew? = null, val symmetrical: Boolean = false): BGColorableText {

    val description: String get() {
        if (defaultGraph != null) {
            return "${defaultGraph.name}: $name"
        } else {
            return name
        }
    }

    val interaction: String get() {
        return description
    }


    val identifier: String get() {
        if (defaultGraph != null) {
            return "${defaultGraph.uri}::$uri"
        }
        return uri
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
    val defaultGraphURI: String? get() {
        return defaultGraph?.uri
    }
}