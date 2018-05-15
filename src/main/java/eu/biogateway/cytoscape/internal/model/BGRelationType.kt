package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.gui.BGColorableText
import java.awt.Color

/**
 * Created by sholmas on 26/05/2017.
 */


class BGExternalRelationType(name: String): BGRelationType("External", name, 0)


open class BGRelationType(val uri: String, val name: String, val number: Int, override val textColor: Color = Color.BLACK, val defaultGraphURI: String? = null, val defaultGraphLabel: String? = null, val arbitraryLength: Boolean = false, val directed: Boolean = true, val expandable: Boolean = false, val fromType: BGNodeType? = null, val toType: BGNodeType? = null): BGColorableText {


    val description: String get() {
        if (defaultGraphLabel != null && defaultGraphLabel.isNotEmpty()) {
            return "$defaultGraphLabel: $name"
        }
        return when (defaultGraphURI != null && defaultGraphURI.isNotEmpty()) {
            true -> defaultGraphURI!!.toUpperCase() + ": "+name
            false -> name
        }
    }

    val interaction: String get() {
        return when (defaultGraphURI.isNullOrEmpty()) {
            true -> name
            false -> defaultGraphURI+":"+name
        }
    }

    val identifier: String get() {
        return when (defaultGraphURI.isNullOrEmpty()) {
            true -> uri
            false -> defaultGraphURI+":"+uri
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