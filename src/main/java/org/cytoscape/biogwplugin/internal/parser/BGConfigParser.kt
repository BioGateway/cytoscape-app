package org.cytoscape.biogwplugin.internal.parser

import org.cytoscape.biogwplugin.internal.model.BGQueryConstraint
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



            // Parse RelationTypes
            val relationTypesNode = (doc.getElementsByTagName("relationTypes").item(0) as? Element) ?: throw Exception("relationTypes element not found in XML file!")
            val rList = relationTypesNode.getElementsByTagName("relationType") ?: throw Exception()

            //var relationTypes = HashMap<String, BGRelationType>()

            for (index in 0..rList.length -1) {
                val element = rList.item(index) as? Element
                val name = element?.getAttribute("name")
                val defaultGraph = element?.getAttribute("defaultGraph")
                val arbitraryLength = element?.getAttribute("arbitraryLength").equals("true")
                val directed = !element?.getAttribute("directed").equals("false")
                val expandable = element?.getAttribute("expandable").equals("true")
                val uri = element?.textContent

                if (name != null && uri != null) {
                    val relationType = BGRelationType(uri, name, index, defaultGraph, arbitraryLength, directed, expandable)
                    //relationTypes.put(relationType.identifier, relationType)
                    cache.addRelationType(relationType)
                }
            }



            // Parse QueryConstraints. Must be after parsing RelationTypes, as it relies on finding relation types in cache.

            val queryConstraintNode = (doc.getElementsByTagName("queryConstraints").item(0) as? Element) ?: throw Exception("queryConstraints element not found in XML file!")
            val constraintList = queryConstraintNode.getElementsByTagName("constraint")

            for (index in 0..constraintList.length-1) {
                val constraint = constraintList.item(index) as? Element ?: continue
                val id = constraint.getAttribute("id") ?: continue
                val label = constraint.getAttribute("label") ?: continue
                val typeName = constraint.getAttribute("type") ?: continue

                val type = when (typeName) {
                    "combobox" -> BGQueryConstraint.InputType.COMBOBOX
                    "text" -> BGQueryConstraint.InputType.TEXT
                    "number" -> BGQueryConstraint.InputType.NUMBER
                    else -> null } ?: continue

                val queryConstraint = BGQueryConstraint(id, label, type)

                val actionsList = constraint.getElementsByTagName("action")

                for (i in 0..actionsList.length-1) {
                    val actionElement = actionsList.item(i) as? Element ?: continue
                    val parameterString = actionElement.getAttribute("parameter") ?: continue
                    val actionParameter = when (parameterString) {
                        "first" -> BGQueryConstraint.ActionParameter.FIRST
                        "last" -> BGQueryConstraint.ActionParameter.LAST
                        "both" -> BGQueryConstraint.ActionParameter.BOTH
                        else -> null } ?: continue
                    val actionGraph = actionElement.getAttribute("graph") ?: continue
                    val sparqlElement = actionElement.getElementsByTagName("sparql").item(0) as?  Element ?: continue
                    val sparqlString = sparqlElement.textContent

                    val relationTypes = ArrayList<BGRelationType>()

                    val relationTypeList = actionElement.getElementsByTagName("relationType")
                    for (j in 0..relationTypeList.length-1) {
                        val rtElement = relationTypeList.item(j) as? Element ?: continue
                        val rtGraph = rtElement.getAttribute("graph") ?: continue
                        val rtUri = rtElement.textContent
                        val relationType = cache.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue
                        relationTypes.add(relationType)
                    }
                    val action = BGQueryConstraint.ConstraintAction(actionParameter, actionGraph, relationTypes, sparqlString)
                    queryConstraint.actions.add(action)
                }

                val optionsList = constraint.getElementsByTagName("option")
                for (oIndex in 0..optionsList.length - 1) {
                    if (optionsList.item(oIndex).nodeType == Node.ELEMENT_NODE) {
                        val oElement = optionsList.item(oIndex) as Element
                        val optionLabel = oElement.getAttribute("name")
                        val optionValue = oElement.textContent
                        val option = BGQueryConstraint.ComboBoxOption(optionLabel, optionValue)
                        queryConstraint.options.add(option)
                    }
                }
                cache.queryConstraints.add(queryConstraint)
            }


            //cache.relationTypeMap = relationTypes

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