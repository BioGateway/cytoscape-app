package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelation
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.query.BGReturnNodeData
import org.cytoscape.biogwplugin.internal.query.BGReturnPubmedIds
import org.cytoscape.biogwplugin.internal.query.BGReturnRelationsData
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader

/**
 * Created by sholmas on 26/05/2017.
 */


enum class BGReturnType(val paremeterCount: Int) {
    NODE_LIST(2),              // nodeUri, common_name
    NODE_LIST_DESCRIPTION(3),  // nodeUri, common_name, name
    NODE_LIST_DESCRIPTION_TAXON(4),  // nodeUri, common_name, name, taxon
    RELATION_TRIPLE(4),         // nodeUri, relationUri, nodeUri
    RELATION_TRIPLE_NAMED(6),    // nodeUri, common_name, relationUri, nodeUri, common_name
    RELATION_TRIPLE_PUBMED(6),  // nodeUri, common_name, relationUri, nodeUri, common_name, pubmedUri
    RELATION_MULTIPART(0), // Arbitrary length. Only to be used with parsing that supports it.
    RELATION_MULTIPART_NAMED_DESCRIBED(0), // Same as above, but has names and description data for all returned nodes.
    RELATION_MULTIPART_FROM_NODE_NAMED_DESCRIBED(0), // Only has name and description data for the FROM nodes.
    RELATION_MULTIPART_TO_NODE_NAMED_DESCRIBED(0), // Only has name and description data for the TO nodes.
    PUBMED_ID(1)
}


class BGParser(private val serviceManager: BGServiceManager) {

    var cancelled = false
    var nodeFetchThread: Thread? = null

    fun cancel() {
        cancelled = true
        nodeFetchThread?.stop()
        throw Exception("Cancelled.")
    }

    fun parseNodesToTextArray(reader: BufferedReader, returnType: BGReturnType, completion: (BGReturnNodeData?) -> Unit) {
        cancelled = false

        val columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()
        val returnData = BGReturnNodeData(returnType, columnNames)

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")

            val lineColumns = it.split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
            returnData.addEntry(lineColumns)
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

    fun parseRelations(reader: BufferedReader, returnType: BGReturnType, taskMonitor: TaskMonitor?, completion: (BGReturnRelationsData?) -> Unit) {
        cancelled = false

        val unloadedNodes = HashSet<BGNode>()
        val unloadedUris = HashSet<String>()
        taskMonitor?.setTitle("Parsing relations...")
        val server = serviceManager.server

        val columnNames = when (returnType) {
            BGReturnType.RELATION_MULTIPART -> {
                reader.readLine() // Read the first line and throw it away, so it won't get muddled in with the relations.
                arrayOf("From node", "Relation Uri", "To node")
            }
            else -> reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()
        }

        val returnData = BGReturnRelationsData(returnType, columnNames)
        val relationSet = HashSet<BGRelation>()
        val relationMap = HashMap<Int, BGRelation>()
        val relationArray = ArrayList<BGRelation>()

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")
            val lineColumns = it.split("\t").dropLastWhile { it.isEmpty() }.toTypedArray()
            if (lineColumns.size != returnType.paremeterCount && returnType != BGReturnType.RELATION_MULTIPART) throw Exception("Number of columns in data array must match the parameter count of the query type!")

            if (returnType == BGReturnType.RELATION_TRIPLE) {
                val fromNodeUri = lineColumns[0].replace("\"", "")
                val graphName = lineColumns[1].replace("\"", "")
                val relationUri = lineColumns[2].replace("\"", "")
                val toNodeUri = lineColumns[3].replace("\"", "")

                val fromNode = server.getNodeFromCacheOrNetworks(BGNode(fromNodeUri))
                if (!fromNode.isLoaded) unloadedNodes.add(fromNode)
                val toNode = server.getNodeFromCacheOrNetworks(BGNode(toNodeUri))
                if (!toNode.isLoaded) unloadedNodes.add(toNode)

                val relationType = server.cache.getRelationTypeForURIandGraph(relationUri, graphName) ?: BGRelationType(relationUri, relationUri, 0)
                val relation = BGRelation(fromNode, relationType, toNode)
                relationType.defaultGraphName?.let {
                    relation.metadata.sourceGraph = it
                }
                val hash = relation.hashCode()
                if (relationMap[hash] == null) {
                    relationMap[hash] = relation
                }
                relationArray.add(relation)

            } else if (returnType == BGReturnType.RELATION_TRIPLE_NAMED) {
                val fromNodeUri = lineColumns[0].replace("\"", "")
                val fromNodeName = lineColumns[1].replace("\"", "")
                val graphName = lineColumns[2].replace("\"", "")
                val relationUri = lineColumns[3].replace("\"", "")
                val toNodeUri = lineColumns[4].replace("\"", "")
                val toNodeName = lineColumns[5].replace("\"", "")

                val fromNode = server.getNodeFromCacheOrNetworks(BGNode(fromNodeUri))
                if (!fromNode.isLoaded) unloadedNodes.add(fromNode)
                val toNode = server.getNodeFromCacheOrNetworks(BGNode(toNodeUri))
                if (!toNode.isLoaded) unloadedNodes.add(toNode)
                val relationType = server.cache.getRelationTypeForURIandGraph(relationUri, graphName)

                // Note: Will ignore relation types it doesn't already know of.
                if (relationType != null) {
                    val relation = BGRelation(fromNode, relationType, toNode)
                    relationType.defaultGraphName?.let {
                        relation.metadata.sourceGraph = it
                    }
                    val hash = relation.hashCode()
                    if (relationMap[hash] == null) {
                        relationMap[hash] = relation
                    }
                    relationArray.add(relation)
                }
            } else if (returnType == BGReturnType.RELATION_TRIPLE_PUBMED) {
                val fromNodeUri = lineColumns[0].replace("\"", "")
                val fromNodeName = lineColumns[1].replace("\"", "")
                val relationUri = lineColumns[2].replace("\"", "")
                val toNodeUri = lineColumns[3].replace("\"", "")
                val toNodeName = lineColumns[4].replace("\"", "")
                val pubmedUri = lineColumns[5].replace("\"", "")

                val fromNode = server.getNodeFromCacheOrNetworks(BGNode(fromNodeUri))
                if (!fromNode.isLoaded) unloadedNodes.add(fromNode)
                val toNode = server.getNodeFromCacheOrNetworks(BGNode(toNodeUri))
                if (!toNode.isLoaded) unloadedNodes.add(toNode)
                fromNode.name = fromNodeName
                toNode.name = toNodeName
                val relationType = server.cache.relationTypeMap.get(relationUri)

                // Note: Will ignore relation types it doesn't already know of.
                if (relationType != null) {
                    val relation = BGRelation(fromNode, relationType, toNode)
                    relation.metadata.pubmedUris.add(pubmedUri)
                    relationType.defaultGraphName?.let {
                        relation.metadata.sourceGraph = it
                    }
                    val hash = relation.hashCode()
                    if (relationMap[hash] == null) {
                        relationMap[hash] = relation
                    }
                    relationArray.add(relation)
                }
            } else if (returnType == BGReturnType.RELATION_MULTIPART) {
                var fromNodeIndex = 0
                while (fromNodeIndex < lineColumns.size-3) {
                    val fromNodeUri = lineColumns[fromNodeIndex+0].replace("\"", "")
                    val graphName = lineColumns[fromNodeIndex+1].replace("\"", "")
                    val relationUri = lineColumns[fromNodeIndex+2].replace("\"", "")
                    val toNodeUri = lineColumns[fromNodeIndex+3].replace("\"", "")
                    fromNodeIndex += 4

                    val fromNode = server.getNodeFromCacheOrNetworks(BGNode(fromNodeUri))
                    if (!fromNode.isLoaded) {
                        unloadedNodes.add(fromNode)
                        unloadedUris.add(fromNodeUri)
                    }
                    val toNode = server.getNodeFromCacheOrNetworks(BGNode(toNodeUri))
                    if (!toNode.isLoaded) {
                        unloadedNodes.add(toNode)
                        unloadedUris.add(toNodeUri)
                    }
                    val relationType = server.cache.getRelationTypeForURIandGraph(relationUri, graphName)

                    // Note: Will ignore relation types it doesn't already know of.
                    if (relationType != null) {
                        val relation = BGRelation(fromNode, relationType, toNode)
                        relationType.defaultGraphName?.let {
                            relation.metadata.sourceGraph = it
                        }
                        val hash = relation.hashCode()
                        if (relationMap[hash] == null) {
                            relationMap[hash] = relation
                        }
                        relationArray.add(relation)
                    }
                }
            }
        }

        returnData.relationsData.addAll(relationMap.values)

        /*
        // Printouts for debugging:
        println(relationArray.size.toString() + " relations parsed.")
        println(relationSet.size.toString() + " unique relations found.")

        println(unloadedNodes.size.toString() + " nodes missing.")
        println(unloadedUris.size.toString() + " uris missing. (should be the same as above.)")
        */
        returnData.unloadedNodes = unloadedNodes.toList()
        completion(returnData)
    }
}

