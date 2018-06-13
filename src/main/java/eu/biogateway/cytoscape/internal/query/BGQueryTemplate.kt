package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.parser.BGReturnType
import java.util.ArrayList

class BGQueryTemplate(var name: String, var description: String, var sparqlString: String, var returnType: BGReturnType) {

    var parameters = ArrayList<BGQueryParameter>()

    fun addParameter(p: BGQueryParameter) {
        parameters.add(p)
    }
}