package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.query.BGReturnNodeData
import org.cytoscape.biogwplugin.internal.query.BGReturnPubmedIds
import org.cytoscape.biogwplugin.internal.query.BGReturnRelationsData
import java.io.BufferedReader

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGParserField(val fieldName: String) {
    URI("identifier uri"),
    COMMON_NAME("common name"),
    RELATION_TYPE("type")
}

enum class BGReturnType(val paremeterCount: Int) {
    NODE_LIST(2),              // nodeUri, common_name
    NODE_LIST_DESCRIPTION(3),  // nodeUri, common_name, name
    NODE_LIST_DESCRIPTION_TAXON(4),  // nodeUri, common_name, name, taxon
    RELATION_TRIPLE(3),         // nodeUri, relationUri, nodeUri
    RELATION_TRIPLE_NAMED(5),    // nodeUri, common_name, relationUri, nodeUri, common_name
    RELATION_TRIPLE_PUBMED(6),  // nodeUri, common_name, relationUri, nodeUri, common_name, pubmedUri
    RELATION_MULTIPART_NAMED(0), // Arbitrary length. Only to be used with parsing that supports it.
    PUBMED_ID(1)
}


class BGParser(private val serviceManager: BGServiceManager) {

    var cancelled = false

    fun cancel() {
        cancelled = true
    }

    fun parseNodesToTextArray(reader: BufferedReader, returnType: BGReturnType, completion: (BGReturnNodeData?) -> Unit) {
        cancelled = false

        val columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()

        val returnData = BGReturnNodeData(returnType, columnNames)

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")

            val lineColumns = it.split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
            returnData.addEntry(lineColumns)
            //println("new Fetched node: "+it)
        }
        completion(returnData)
    }

    fun parsePubmedIdsToTextArray(reader: BufferedReader, returnType: BGReturnType, completion: (BGReturnPubmedIds?) -> Unit) {
        cancelled = false

        val returnData = BGReturnPubmedIds(arrayOf("pubmedIds"))

        val columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")
            val lineColumns = it.split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
            if (lineColumns.size != returnType.paremeterCount) throw Exception("Number of columns in data array must match the parameter count of the query type!")
            val uri = lineColumns[0].replace("\"", "")
            returnData.pubmedIDlist.add(uri)
        }
        completion(returnData)
    }

    fun parsePathway(reader: BufferedReader, returnType: BGReturnType, completion: (BGReturnRelationsData) -> Unit) {
        cancelled = false

        if (returnType != BGReturnType.RELATION_MULTIPART_NAMED) throw Exception("Return type must be relation multipart!")

        val server = serviceManager.server
        var columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()

        columnNames = arrayOf("From node", "Relation Uri", "To node")

        val returnData = BGReturnRelationsData(returnType, columnNames)

        // This might generate a lot of duplicate relations, so we use a set to eliminate duplicates.

        var relationSet = HashSet<BGRelation>()

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")

            val lineColumns = it.split("\t").dropLastWhile {  it.isEmpty() }.toTypedArray()
            var fromNodeIndex = 0
            while (fromNodeIndex < lineColumns.size-2) {
                val fromNodeUri = lineColumns[fromNodeIndex+0].replace("\"", "")
                //val fromNodeName = lineColumns[fromNodeIndex+1].replace("\"", "")
                val relationUri = lineColumns[fromNodeIndex+1].replace("\"", "")
                val toNodeUri = lineColumns[fromNodeIndex+2].replace("\"", "")
                //val toNodeName = lineColumns[fromNodeIndex+4].replace("\"", "")
                fromNodeIndex += 3

                var fromNode = server.getNodeFromCache(BGNode(fromNodeUri))
                //fromNode.name = fromNodeName
                var toNode = server.getNodeFromCache(BGNode(toNodeUri))
                //toNode.name = toNodeName
                val relationType = server.cache.relationTypeMap.get(relationUri)

                // Note: Will ignore relation types it doesn't already know of.
                if (relationType != null) {
                    val relation = BGRelation(fromNode, relationType, toNode)
                    relationType.defaultGraphName?.let {
                        relation.metadata.sourceGraph = it
                    }
                    relationSet.add(relation)
                }
            }
        }
        returnData.relationsData.addAll(relationSet)
        completion(returnData)
    }


    fun parseRelations(reader: BufferedReader, returnType: BGReturnType, completion: (BGReturnRelationsData?) -> Unit) {
        cancelled = false

        val server = serviceManager.server

        val columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()
        var returnData = BGReturnRelationsData(returnType, columnNames)
        var relationSet = HashSet<BGRelation>()

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")

            val lineColumns = it.split("\t").dropLastWhile {  it.isEmpty() }.toTypedArray()
            if (lineColumns.size != returnType.paremeterCount) throw Exception("Number of columns in data array must match the parameter count of the query type!")

            if (returnType == BGReturnType.RELATION_TRIPLE) {
                val fromNodeUri = lineColumns[0].replace("\"", "")
                val relationUri = lineColumns[1].replace("\"", "")
                val toNodeUri = lineColumns[2].replace("\"", "")

                var fromNode = server.getNodeFromCache(BGNode(fromNodeUri))
                var toNode = server.getNodeFromCache(BGNode(toNodeUri))
                //val relationType = server.cache.relationTypeMap.get(relationUri) ?: throw NullPointerException("RelationType not found for this URI!")
                val relationType = server.cache.relationTypeMap.get(relationUri) ?: BGRelationType(relationUri, relationUri, 0)
                val relation = BGRelation(fromNode, relationType, toNode)
                relationType.defaultGraphName?.let {
                    relation.metadata.sourceGraph = it
                }
                relationSet.add(relation)

            } else if (returnType == BGReturnType.RELATION_TRIPLE_NAMED) {
                val fromNodeUri = lineColumns[0].replace("\"", "")
                val fromNodeName = lineColumns[1].replace("\"", "")
                val relationUri = lineColumns[2].replace("\"", "")
                val toNodeUri = lineColumns[3].replace("\"", "")
                val toNodeName = lineColumns[4].replace("\"", "")

                var fromNode = server.getNodeFromCache(BGNode(fromNodeUri))
                fromNode.name = fromNodeName
                var toNode = server.getNodeFromCache(BGNode(toNodeUri))
                toNode.name = toNodeName
                val relationType = server.cache.relationTypeMap.get(relationUri)

                // Note: Will ignore relation types it doesn't already know of.
                if (relationType != null) {
                    val relation = BGRelation(fromNode, relationType, toNode)
                    relationType.defaultGraphName?.let {
                        relation.metadata.sourceGraph = it
                    }
                    returnData.relationsData.add(relation)
                }
            }
            else if (returnType == BGReturnType.RELATION_TRIPLE_PUBMED) {
                val fromNodeUri = lineColumns[0].replace("\"", "")
                val fromNodeName = lineColumns[1].replace("\"", "")
                val relationUri = lineColumns[2].replace("\"", "")
                val toNodeUri = lineColumns[3].replace("\"", "")
                val toNodeName = lineColumns[4].replace("\"", "")
                val pubmedUri = lineColumns[5].replace("\"", "")

                var fromNode = server.getNodeFromCache(BGNode(fromNodeUri))
                fromNode.name = fromNodeName
                var toNode = server.getNodeFromCache(BGNode(toNodeUri))
                toNode.name = toNodeName
                val relationType = server.cache.relationTypeMap.get(relationUri)

                // Note: Will ignore relation types it doesn't already know of.
                if (relationType != null) {
                    val relation = BGRelation(fromNode, relationType, toNode)
                    relation.metadata.pubmedUrl = pubmedUri
                    relationType.defaultGraphName?.let {
                        relation.metadata.sourceGraph = it
                    }
                    returnData.relationsData.add(relation)
                }
            }
        }
        returnData.relationsData.addAll(relationSet)
        completion(returnData)
    }
}

