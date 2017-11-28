package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.query.BGQueryParameter
import org.cytoscape.biogwplugin.internal.query.QueryTemplate
import org.cytoscape.biogwplugin.internal.server.BGServer
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object BGConfigParser {
    fun parseXMLConfigFile(stream: InputStream, cache: BGServer.BGCache) {

        val queryTemplateHashMap = java.util.HashMap<String, QueryTemplate>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        try {
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(stream)
            doc.documentElement.normalize()
            val relationTypesNode = (doc.getElementsByTagName("relationTypes").item(0) as? Element) ?: throw Exception("relationTypes element not found in XML file!")
            val rList = relationTypesNode.getElementsByTagName("relationType") ?: throw Exception()

            var relationTypes = HashMap<String, BGRelationType>()

            for (index in 0..rList.length -1) {
                val element = rList.item(index) as? Element
                val name = element?.getAttribute("name")
                val defaultGraph = element?.getAttribute("defaultGraph")
                val arbitraryLength = element?.getAttribute("arbitraryLength").equals("true")
                val directed = !element?.getAttribute("directed").equals("false")
                val uri = element?.textContent

                if (name != null && uri != null) {
                    val relationType = BGRelationType(uri, name, index, defaultGraph, arbitraryLength, directed)
                    relationTypes.put(relationType.identifier, relationType)
                }
            }

            cache.relationTypeMap = relationTypes

            // Will crash if the queryList tag isn't present.
            val queryList = (doc.getElementsByTagName("queryList").item(0) as? Element) ?: throw Exception("queryList element not found in XML file!")
            val nList = queryList.getElementsByTagName("query")

            for (temp in 0..nList.length - 1) {
                val nNode = nList.item(temp)

                if (nNode.nodeType == Node.ELEMENT_NODE) {
                    val qElement = nNode as Element
                    val queryName = qElement.getAttribute("name")
                    val returnTypeString = qElement.getAttribute("returnType")
                    val queryDescription = qElement.getElementsByTagName("name").item(0).textContent
                    val sparqlString = qElement.getElementsByTagName("sparql").item(0).textContent.replace("\t", "") // Remove tabs from the XML file. (They might be added "for show").

                    val returnType = when (returnTypeString) {
                        "nodeList" -> BGReturnType.NODE_LIST
                        "nodeListDescription" -> BGReturnType.NODE_LIST_DESCRIPTION
                        "relationTriple" -> BGReturnType.RELATION_TRIPLE
                        "relationTripleNamed" -> BGReturnType.RELATION_TRIPLE_NAMED
                        else -> {
                            throw Exception("Unknown return type!")
                        }
                    }

                    val query = QueryTemplate(queryName, queryDescription, sparqlString, returnType)
                    val parameterList = qElement.getElementsByTagName("parameter")

                    for (pIndex in 0..parameterList.length - 1) {

                        if (parameterList.item(pIndex).nodeType == Node.ELEMENT_NODE) {
                            val parameter = parameterList.item(pIndex) as Element
                            val pId = parameter.getAttribute("id")
                            val pTypeString = parameter.getAttribute("type")
                            val pName = parameter.getElementsByTagName("name").item(0).textContent

                            val pEnabledDependency = parameter.getElementsByTagName("enabled-dependency")

                            val enabledDependency = pEnabledDependency.item(0) as? Element



                            val pType = when (pTypeString) {
                                "text" -> BGQueryParameter.ParameterType.TEXT
                                "checkbox" -> BGQueryParameter.ParameterType.CHECKBOX
                                "combobox" -> BGQueryParameter.ParameterType.COMBOBOX
                                "uniprot_id" -> BGQueryParameter.ParameterType.UNIPROT_ID
                                "ontology" -> BGQueryParameter.ParameterType.ONTOLOGY
                                "optionalUri" -> BGQueryParameter.ParameterType.OPTIONAL_URI
                                else -> BGQueryParameter.ParameterType.TEXT
                            }
                            val qParameter = BGQueryParameter(pId, pName, pType)
                            val optionsList = parameter.getElementsByTagName("option")

                            for (oIndex in 0..optionsList.length - 1) {
                                if (optionsList.item(oIndex).nodeType == Node.ELEMENT_NODE) {
                                    val oElement = optionsList.item(oIndex) as Element
                                    val oName = oElement.getAttribute("name")
                                    val oValue = oElement.textContent
                                    qParameter.addOption(oName, oValue)
                                } }

                            if (enabledDependency != null) {
                                val dependingId = enabledDependency.getAttribute("parameterId")
                                val isEnabled = when (enabledDependency.getAttribute("isEnabled")) {
                                    "true" -> true
                                    "false" -> false
                                    else -> {
                                        println("XML Config parse error: enabled-dependency's isEnabled attribute can only be true or false!")
                                        null
                                    }
                                }
                                val parameterValue = enabledDependency.getAttribute("forParameterValue")
                                if (dependingId != null && parameterValue != null && isEnabled != null) {
                                    qParameter.dependency = BGQueryParameter.EnabledDependency(dependingId, isEnabled, parameterValue)

                                }
                            }

                            query.addParameter(qParameter)
                        } }
                    queryTemplateHashMap.put(queryName, query)
                } }
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        cache.queryTemplates = queryTemplateHashMap
    }
}