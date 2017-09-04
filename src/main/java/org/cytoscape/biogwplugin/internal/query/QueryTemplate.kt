package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.parser.BGReturnType
import java.util.ArrayList

class QueryTemplate(var name: String, var description: String, var sparqlString: String, var returnType: BGReturnType) {

    var parameters = ArrayList<BGQueryParameter>()

    fun addParameter(p: BGQueryParameter) {
        parameters.add(p)
    }
}