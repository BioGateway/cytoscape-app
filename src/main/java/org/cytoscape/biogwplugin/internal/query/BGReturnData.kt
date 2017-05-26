package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGQueryType

/**
 * Created by sholmas on 26/05/2017.
 */

abstract class BGReturnData(queryType: BGQueryType) {
    fun removeIllegalCharacters(input: String): String {
        return input.replace("\"", "")
    }
}

class BGReturnNodeData(val queryType: BGQueryType): BGReturnData(queryType) {

    val nodeData = HashMap<String, BGNode>()

    // TODO: Is this overkill? Where should the abstraction of different return types lie?


    fun addEntry(line: Array<String>) {

        if (line.size != queryType.paremeterCount) throw Exception("Invalid parameter count!")

        val node =  when (queryType) {
            BGQueryType.NODE_QUERY -> {
                val nodeUri = removeIllegalCharacters(line.get(0))
                val nodeName = removeIllegalCharacters(line.get(1))
                val description = removeIllegalCharacters(line.get(2))
                BGNode(nodeUri, nodeName, description)
            }
            else -> {
                throw Exception("Invalid queryType!")
            }
        }
        nodeData.put(node.uri, node)
    }
}
class BGReturnRelationsData(type: BGQueryType) : BGReturnData(type) {
    val relationsData = ArrayList<BGRelation>()
}