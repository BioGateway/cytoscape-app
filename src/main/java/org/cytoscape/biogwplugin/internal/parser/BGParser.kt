package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.query.BGReturnNodeData
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGParserField(val fieldName: String) {
    URI("identifier uri"),
    COMMON_NAME("common name"),
    RELATION_TYPE("type")
}

enum class BGQueryType(val paremeterCount: Int) {
    NODE_QUERY(3),
    RELATION_TRIPLE(3),
    RELATION_TRIPLE_NAMED(5)
}


class BGParser() {

    fun parseNodes(stream: InputStream, completion: (BGReturnNodeData?) -> Unit) {
        // TODO: Add exception handling.
        val reader = BufferedReader(InputStreamReader(stream))

        val columnNames = reader.readLine().split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
        val returnData = BGReturnNodeData(BGQueryType.NODE_QUERY, columnNames)

        reader.forEachLine {
            val lineColumns = it.split("\t").dropLastWhile({it.isEmpty()}).toTypedArray()
            returnData.addEntry(lineColumns)
        }
        completion(returnData)
    }
}