package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.parser.BGReturnType
import java.util.ArrayList

class BGQueryTemplate(var name: String, var description: String, var sparqlString: String, var returnType: BGReturnType) {

    var parameters = ArrayList<BGQueryParameter>()

    fun addParameter(p: BGQueryParameter) {
        parameters.add(p)
    }
}