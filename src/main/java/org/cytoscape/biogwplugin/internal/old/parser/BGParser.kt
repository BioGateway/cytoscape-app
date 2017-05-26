package org.cytoscape.biogwplugin.internal.old.parser

import org.cytoscape.biogwplugin.internal.old.cache.BGCache
import org.cytoscape.biogwplugin.internal.old.query.BGNode
import org.cytoscape.biogwplugin.internal.old.query.BGRelation
import org.cytoscape.biogwplugin.internal.old.query.BGRelationsQuery

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.HashMap

object BGParser {

    fun parseNodes(`is`: InputStream, cache: BGCache): ArrayList<BGNode> {

        val nodes = ArrayList<BGNode>()

        val reader = BufferedReader(InputStreamReader(`is`))

        try {
            reader.readLine() // Read the first line and throw it away for now. Might be of use later?
            reader.forEachLine {
                val line = it
                val lineComponents = line.split("\t".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                // If there's more than two components, something went wrong.
                // TODO: Replace this with an exception, an assert is overly drastic.
                assert(lineComponents.size == 2)

                val uri = removeIllegalCharacters(lineComponents[0])
                val description = removeIllegalCharacters(lineComponents[1])

                val node = BGNode(uri)
                node.commonName = description
                cache.addNode(node)
                nodes.add(node)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return nodes
    }

    fun parseRelations(`is`: InputStream, query: BGRelationsQuery, cache: BGCache): ArrayList<BGRelation> {
        val relations = ArrayList<BGRelation>()
        val reader = BufferedReader(InputStreamReader(`is`))

        try {
            reader.readLine() // Read the first line and throw it away for now. Might be of use later?
            reader.forEachLine {
                val line = it
                val lineComponents = line.split("\t".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                // If there's more than two components, something went wrong.
                // TODO: Replace this with an exception, an assert is overly drastic.
                assert(lineComponents.size == 4)

                if (query.direction == BGRelationsQuery.Direction.POST) {
                    // TODO: Assert that this node exists. It should, but can't be certain.
                    val fromNode = cache.getNodeWithURI(query.nodeURI)
                    if (fromNode == null) {
                        println("WARNING! FROM NODE NOT FOUND IN CACHE! THIS SHOULD NOT BE POSSIBLE!")
                        println("Node OPTIONAL_URI: " + query.nodeURI)
                    }

                    val relationType = removeIllegalCharacters(lineComponents[1])
                    val toNodeUri = removeIllegalCharacters(lineComponents[2])
                    val toNodeName = removeIllegalCharacters(lineComponents[3])

                    var toNode: BGNode? = cache.getNodeWithURI(toNodeUri)
                    if (toNode == null) {
                        toNode = BGNode(toNodeUri)
                        toNode.commonName = toNodeName
                        cache.addNode(toNode)
                    }

                    val relation = BGRelation(fromNode, toNode, relationType)
                    relations.add(relation)

                } else {
                    val toNode = cache.getNodeWithURI(query.nodeURI)
                    if (toNode == null) {
                        println("WARNING! TO NODE NOT FOUND IN CACHE! THIS SHOULD NOT BE POSSIBLE!")
                        println("Node OPTIONAL_URI: " + query.nodeURI)
                    }
                    // TODO: Code repetition. Refactor?
                    val relationType = removeIllegalCharacters(lineComponents[1])
                    val fromNodeUri = removeIllegalCharacters(lineComponents[2])
                    val fromNodeName = removeIllegalCharacters(lineComponents[3])

                    var fromNode: BGNode? = cache.getNodeWithURI(fromNodeUri)
                    if (fromNode == null) {
                        fromNode = BGNode(fromNodeUri)
                        fromNode.commonName = fromNodeName
                        cache.addNode(fromNode)
                    }

                    val relation = BGRelation(fromNode, toNode, relationType)
                    relations.add(relation)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return relations
    }

    fun parseRelationTypes(`is`: InputStream): HashMap<String, String> {
        val relationTypes = HashMap<String, String>()
        val reader = BufferedReader(InputStreamReader(`is`))

        try {
            reader.readLine()
            reader.forEachLine {
                val line = it
                val lineComponents = line.split("\t".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                if (lineComponents.size != 2) {
                    throw Exception("Illegal data format: Expected two columns, but found " + lineComponents.size + ".")
                }
                relationTypes.put(removeIllegalCharacters(lineComponents[0]), removeIllegalCharacters(lineComponents[1]))
            }


        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return relationTypes
    }

    internal fun removeIllegalCharacters(input: String): String {
        val returnString = input.replace("\"", "")
        // TODO: Replace other illegal characters as well.
        return returnString
    }
}
