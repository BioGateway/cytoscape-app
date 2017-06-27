package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

/**
 * Created by sholmas on 26/05/2017.
 */

abstract class BGReturnData {
    val columnNames: Array<String>

    constructor(returnType: BGReturnType, columnNames: Array<String>) {
        if (columnNames.size != returnType.paremeterCount) throw Exception("Parameter count must match column name count!")
        this.columnNames = columnNames
    }

    fun removeIllegalCharacters(input: String): String {
        return input.replace("\"", "")
    }
}

class BGReturnNodeData(val returnType: BGReturnType, columnNames: Array<String>): BGReturnData(returnType, columnNames) {

    val nodeData = HashMap<String, BGNode>()

    // TODO: Is this overkill? Where should the abstraction of different return types lie?


    fun addEntry(line: Array<String>) {

        if (line.size != returnType.paremeterCount) throw Exception("Invalid parameter count!")

        val node =  when (returnType) {
            BGReturnType.NODE_LIST_DESCRIPTION -> {
                val nodeUri = removeIllegalCharacters(line.get(0))
                val nodeName = removeIllegalCharacters(line.get(1))
                val description = removeIllegalCharacters(line.get(2))
                BGNode(nodeUri, nodeName, description)
            }
            BGReturnType.NODE_LIST -> {
                val nodeUri = removeIllegalCharacters(line.get(0))
                val nodeName = removeIllegalCharacters(line.get(1))
                BGNode(nodeUri, nodeName)
            }
            else -> {
                throw Exception("Invalid returnType!")
            }
        }
        nodeData.put(node.uri, node)
    }
}
class BGReturnRelationsData(type: BGReturnType, columnNames: Array<String>) : BGReturnData(type, columnNames) {
    val relationsData = ArrayList<BGRelation>()
}