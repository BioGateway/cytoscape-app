package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGQueryType

/**
 * Created by sholmas on 26/05/2017.
 */

abstract class BGReturnData {
    val columnNames: Array<String>

    constructor(queryType: BGQueryType, columnNames: Array<String>) {
        if (columnNames.size != queryType.paremeterCount) throw Exception("Parameter count must match column name count!")
        this.columnNames = columnNames
    }

    fun removeIllegalCharacters(input: String): String {
        return input.replace("\"", "")
    }
}

class BGReturnNodeData(val queryType: BGQueryType, columnNames: Array<String>): BGReturnData(queryType, columnNames) {

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
class BGReturnRelationsData(type: BGQueryType, columnNames: Array<String>) : BGReturnData(type, columnNames) {
    val relationsData = ArrayList<BGRelation>()
}