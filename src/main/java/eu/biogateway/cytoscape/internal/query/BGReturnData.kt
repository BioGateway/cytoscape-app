package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.model.BGNode
import eu.biogateway.cytoscape.internal.model.BGRelation
import eu.biogateway.cytoscape.internal.parser.BGReturnType
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Created by sholmas on 26/05/2017.
 */

abstract class BGReturnData {
    // The column names for the result table
    var columnNames: Array<String>

    // An optional title for the result window. If not null, the title of the window displaying the result can be set to this.
    var resultTitle: String? = null

    constructor(returnType: BGReturnType, columnNames: Array<String>) {
        this.columnNames = columnNames
    }

    fun removeIllegalCharacters(input: String): String {
        return input.replace("\"", "")
    }
}

class BGReturnMetadata(columnName: String, val values: ArrayList<String>): BGReturnData(BGReturnType.METADATA_FIELD, arrayOf(columnName))


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
            BGReturnType.NODE_LIST_DESCRIPTION_STATUS -> {
                val nodeUri = removeIllegalCharacters(line.get(0))
                val nodeName = removeIllegalCharacters(line.get(1))
                val description = removeIllegalCharacters(line.get(2))
                val reviewed = removeIllegalCharacters(line.get(3)).toBoolean()
                BGNode(nodeUri, nodeName, description, reviewed)
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

class BGReturnCompoundData(type: BGReturnType, columnNames: Array<String>) : BGReturnData(type, columnNames) {
    var relationsData = HashSet<BGRelation>()
    var unloadedNodes = HashSet<BGNode>()
    var nodes = HashMap<String, BGNode>()
    var metadata = ArrayList<String>()
}