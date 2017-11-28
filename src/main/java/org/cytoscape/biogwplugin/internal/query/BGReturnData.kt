package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.parser.BGReturnType

/**
 * Created by sholmas on 26/05/2017.
 */

abstract class BGReturnData {
    // The column names for the result table
    var columnNames: Array<String>

    // An optional title for the result window. If not null, the title of the window displaying the result can be set to this.
    var resultTitle: String? = null

    constructor(returnType: BGReturnType, columnNames: Array<String>) {

        //if (returnType != BGReturnType.RELATION_MULTIPART && columnNames.size != returnType.paremeterCount) throw Exception("Parameter count must match column name count!")
        this.columnNames = columnNames
    }

    fun removeIllegalCharacters(input: String): String {
        return input.replace("\"", "")
    }
}

class BGReturnPubmedIds(columnNames: Array<String>): BGReturnData(BGReturnType.PUBMED_ID, columnNames) {
    var pubmedIDlist = ArrayList<String>()
}

class BGReturnNodeData(val returnType: BGReturnType, columnNames: Array<String>): BGReturnData(returnType, columnNames) {

    val nodeData = HashMap<String, BGNode>()


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
            BGReturnType.NODE_LIST_DESCRIPTION_TAXON -> {
                val nodeUri = removeIllegalCharacters(line.get(0))
                val nodeName = removeIllegalCharacters(line.get(1))
                val description = removeIllegalCharacters(line.get(2))
                val taxon = removeIllegalCharacters(line.get(3))
                BGNode(nodeUri, nodeName, description, taxon)
            }
            else -> {
                throw Exception("Invalid returnType!")
            }
        }
        nodeData.put(node.uri, node)
    }
}
class BGReturnRelationsData(type: BGReturnType, columnNames: Array<String>) : BGReturnData(type, columnNames) {
    var relationsData = ArrayList<BGRelation>()
    var unloadedNodes: List<BGNode>? = null
}
