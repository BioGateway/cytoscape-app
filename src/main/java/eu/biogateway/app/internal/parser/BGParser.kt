package eu.biogateway.app.internal.parser

import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.model.*
import eu.biogateway.app.internal.query.*
import eu.biogateway.app.internal.util.sanitizeParameter
import org.cytoscape.work.TaskMonitor
import java.io.BufferedReader

/**
 * Created by sholmas on 26/05/2017.
 */


enum class BGReturnType(val paremeterCount: Int, val optionalParameterCount: Int? = null) {
    NODE_LIST(2),              // nodeUri, common_name
    NODE_LIST_DESCRIPTION(2, 3),  // nodeUri, common_name, name
    NODE_LIST_DESCRIPTION_STATUS(4),  // nodeUri, common_name, name
    NODE_LIST_DESCRIPTION_TAXON(4),  // nodeUri, common_name, name, taxon
    RELATION_TRIPLE_GRAPHURI(4),         // nodeUri, graphUri, relationUri, nodeUri
    RELATION_TRIPLE_NAMED(6),    // nodeUri, common_name, relationUri, nodeUri, common_name
    RELATION_TRIPLE_PUBMED(6),  // nodeUri, relationUri, nodeUri, pubmedUri
    RELATION_TRIPLE_CONFIDENCE(5),  // nodeUri, graph, relationUri, nodeUri, confidence score
    RELATION_MULTIPART(0), // Arbitrary length. Only to be used with parsing that supports it.
    RELATION_MULTIPART_NAMED_DESCRIBED(0), // Same as above, but has names and description data for all returned nodes.
    RELATION_MULTIPART_FROM_NODE_NAMED_DESCRIBED(0), // Only has name and description data for the FROM nodes.
    RELATION_MULTIPART_TO_NODE_NAMED_DESCRIBED(0), // Only has name and description data for the TO nodes.
    PUBMED_ID(1),
    METADATA_FIELD(1)
}


class BGParser() {

    var cancelled = false
    var nodeFetchThread: Thread? = null

    fun cancel() {
        cancelled = true
        nodeFetchThread?.stop()
        throw Exception("Cancelled.")
    }

    fun parseData(reader: BufferedReader, queryType: BGQueryType, taskMonitor: TaskMonitor? = null): BGReturnData {
        when (queryType) {
            BGQueryType.NODE -> return parseNodesToTextArray(reader, queryType.returnType)
            BGQueryType.RELATION, BGQueryType.MULTI_RELATION -> return parseRelations(reader, queryType.returnType, taskMonitor)
            BGQueryType.METADATA -> return parseMetadata(reader, queryType.returnType)
        }
    }

    fun parseMetadata(reader: BufferedReader, returnType: BGReturnType): BGReturnMetadata {
        val returnType = BGReturnType.METADATA_FIELD

        val firstLine = reader.readLine().split("\t")
        if (firstLine.size != returnType.paremeterCount) throw Exception("Number of columns in data array must match the parameter count of the query type!")

        val values = ArrayList<String>()
        reader.forEachLine {
            values.add(it.sanitizeParameter())
        }

        return BGReturnMetadata("values", values)
    }

    fun parseNodesToTextArray(reader: BufferedReader, returnType: BGReturnType): BGReturnNodeData {
        cancelled = false

        val columnNames = reader.readLine().split("\t").dropLastWhile { it.isEmpty() }.map { it.replace("\"", "") }.toTypedArray()
        val returnData = BGReturnNodeData(returnType, columnNames)

        reader.forEachLine {
            if (cancelled) throw Exception("Cancelled.")

            val lineColumns = it.split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
            returnData.addEntry(lineColumns)
        }
        return returnData
    }

    fun parsePubmedIdsToTextArray(reader: BufferedReader, returnType: BGReturnType): BGReturnPubmedIds {
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
        return returnData
    }

    fun parseRelations(reader: BufferedReader, returnType: BGReturnType, taskMonitor: TaskMonitor?): BGReturnRelationsData {
        cancelled = false

        val unloadedNodes = HashSet<BGNode>()
        val unloadedUris = HashSet<String>()
        taskMonitor?.setTitle("Parsing relations...")
        val server = BGServiceManager.dataModelController

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
        val pathSet = HashSet<BGPath>()
        var lineNumber = 0

        reader.forEachLine {
            lineNumber++
            if (cancelled) throw Exception("Cancelled.")
            val lineColumns = it.split("\t").dropLastWhile { it.isEmpty() }.toTypedArray()
            val path = BGPath(lineNumber)
            pathSet.add(path)
            if (lineColumns.size != returnType.paremeterCount && returnType != BGReturnType.RELATION_MULTIPART) throw Exception("Number of columns in data array must match the parameter count of the query type!")

            when (returnType) {
                BGReturnType.RELATION_TRIPLE_GRAPHURI -> {
                    val fromNodeUri = lineColumns[0].replace("\"", "")
                    val graphName = lineColumns[1].replace("\"", "")
                    val relationUri = lineColumns[2].replace("\"", "")
                    val toNodeUri = lineColumns[3].replace("\"", "")

                    val fromNode = server.getNodeFromCacheOrNetworks(BGNode(fromNodeUri))
                    if (!fromNode.isLoaded) unloadedNodes.add(fromNode)
                    val toNode = server.getNodeFromCacheOrNetworks(BGNode(toNodeUri))
                    if (!toNode.isLoaded) unloadedNodes.add(toNode)

                    val relationType = server.config.getRelationTypeForURIandGraph(relationUri, graphName) ?: BGRelationType(relationUri, relationUri, 0)
                    val relation = BGRelation(fromNode, relationType, toNode)
                    relationType.defaultGraphURI?.let {
                        relation.sourceGraph = it
                    }
                    val hash = relation.hashCode()
                    if (relationMap[hash] == null) {
                        relationMap[hash] = relation
                    }
                    relationArray.add(relation)

                }
                BGReturnType.RELATION_TRIPLE_CONFIDENCE -> {
                    val fromNodeUri = lineColumns[0].replace("\"", "")
                    val graphName = lineColumns[1].replace("\"", "")
                    val relationUri = lineColumns[2].replace("\"", "")
                    val toNodeUri = lineColumns[3].replace("\"", "")
                    val confidenceValue = lineColumns[4].replace("\"", "")

                    val fromNode = server.getNodeFromCacheOrNetworks(BGNode(fromNodeUri))
                    if (!fromNode.isLoaded) unloadedNodes.add(fromNode)
                    val toNode = server.getNodeFromCacheOrNetworks(BGNode(toNodeUri))
                    if (!toNode.isLoaded) unloadedNodes.add(toNode)

                    val relationType = server.config.getRelationTypeForURIandGraph(relationUri, graphName) ?: BGRelationType(relationUri, relationUri, 0)
                    val relation = BGRelation(fromNode, relationType, toNode)
                    // TODO: Delete this line, see if it works still. It should.
                    relation.metadata[BGRelationMetadata.CONFIDENCE_VALUE.name] = BGRelationMetadata(BGTableDataType.DOUBLE, confidenceValue.toDouble())
                    relationType.defaultGraphURI?.let {
                        relation.sourceGraph = it
                    }
                    val hash = relation.hashCode()
                    if (relationMap[hash] == null) {
                        relationMap[hash] = relation
                    }
                    relationArray.add(relation)

                }
                BGReturnType.RELATION_TRIPLE_NAMED -> {
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
                    var relationType = server.config.getRelationTypeForURIandGraph(relationUri, graphName) ?: BGRelationType(relationUri, relationUri, 0)

                    val relation = BGRelation(fromNode, relationType, toNode)
                    relationType.defaultGraphURI?.let {
                        relation.sourceGraph = it
                    }
                    val hash = relation.hashCode()
                    if (relationMap[hash] == null) {
                        relationMap[hash] = relation
                    }
                    relationArray.add(relation)

                }
                BGReturnType.RELATION_MULTIPART -> {
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
                        var relationType = server.config.getRelationTypeForURIandGraph(relationUri, graphName) ?: BGRelationType(relationUri, relationUri, 0)

                        // TODO: R
                        val relation = BGRelation(fromNode, relationType, toNode)
                        relationType.defaultGraphURI?.let {
                            relation.sourceGraph = it
                        }
                        val hash = relation.hashCode()
                        val existingRelation = relationMap[hash]
                        if (existingRelation == null) {
                            relationMap[hash] = relation
                            path.add(relation)
                        } else {
                            path.add(existingRelation)
                        }
                       // relationArray.add(relation)
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
        return returnData
    }
}

