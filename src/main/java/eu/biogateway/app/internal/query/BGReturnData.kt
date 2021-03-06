package eu.biogateway.app.internal.query

import eu.biogateway.app.internal.model.BGNode
import eu.biogateway.app.internal.model.BGNodeFilter
import eu.biogateway.app.internal.model.BGRelation
import eu.biogateway.app.internal.parser.BGReturnType

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

    abstract fun filterWith(filters: Collection<BGNodeFilter>)

    fun removeIllegalCharacters(input: String): String {
        return input.replace("\"", "")
    }
}

class BGReturnMetadata(columnName: String, val values: ArrayList<String>): BGReturnData(BGReturnType.METADATA_FIELD, arrayOf(columnName)) {
    override fun filterWith(filters: Collection<BGNodeFilter>) {

    }
}


class BGReturnPubmedIds(columnNames: Array<String>): BGReturnData(BGReturnType.PUBMED_ID, columnNames) {
    override fun filterWith(filters: Collection<BGNodeFilter>) {

    }

    var pubmedIDlist = ArrayList<String>()
}

class BGReturnNodeData(val returnType: BGReturnType, columnNames: Array<String>): BGReturnData(returnType, columnNames) {

    val nodeData = HashMap<String, BGNode>()

    override fun filterWith(filters: Collection<BGNodeFilter>) {
        val removedNodes = nodeData.values.toSet().subtract(BGNodeFilter.filterNodes(nodeData.values, filters))
        removedNodes.forEach {
            nodeData.remove(it.uri)
        }
    }

    fun addEntry(line: Array<String>) {

        if (line.size != returnType.paremeterCount && line.size != returnType.optionalParameterCount) {
            throw Exception("Invalid parameter count!")
        }

        val node =  when (returnType) {
            BGReturnType.NODE_LIST_DESCRIPTION -> {
                val nodeUri = removeIllegalCharacters(line.get(0))
                val nodeName = removeIllegalCharacters(line.get(1))
                val description = if (line.size > 2) removeIllegalCharacters(line.get(2)) else ""
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
    var relationsData = ArrayList<BGRelation>()
    var unloadedNodes: List<BGNode>? = null

    override fun filterWith(filters: Collection<BGNodeFilter>) {
        val allNodes = relationsData.map { it.fromNode }.toSet().union(relationsData.map { it.toNode })
        val filteredNodes = BGNodeFilter.filterNodes(allNodes, filters)

        val filteredRelations = relationsData.filter { filteredNodes.contains(it.toNode) && filteredNodes.contains(it.fromNode) }
        relationsData = ArrayList(filteredRelations)
    }
}

class BGReturnCompoundData(type: BGReturnType, columnNames: Array<String>) : BGReturnData(type, columnNames) {
    override fun filterWith(filters: Collection<BGNodeFilter>) {

    }


    var relationsData = HashSet<BGRelation>()
    var unloadedNodes = HashSet<BGNode>()
    var nodes = HashMap<String, BGNode>()
    var metadata = ArrayList<String>()
}