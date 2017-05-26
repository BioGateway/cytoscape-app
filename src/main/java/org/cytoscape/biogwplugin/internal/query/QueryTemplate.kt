package org.cytoscape.biogwplugin.internal.query

import java.util.ArrayList

class QueryTemplate(var name: String, var description: String, var sparqlString: String) {

    var parameters = ArrayList<QueryParameter>()

    fun addParameter(p: QueryParameter) {
        parameters.add(p)
    }
}