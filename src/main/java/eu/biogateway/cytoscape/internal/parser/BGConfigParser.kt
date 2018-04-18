package eu.biogateway.cytoscape.internal.parser

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.*
import eu.biogateway.cytoscape.internal.query.BGQueryParameter
import eu.biogateway.cytoscape.internal.query.QueryTemplate
import eu.biogateway.cytoscape.internal.model.BGDataModelController
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.awt.Color
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

object BGConfigParser {
    fun parseXMLConfigFile(stream: InputStream, cache: BGDataModelController.BGCache) {

        val queryTemplateHashMap = java.util.HashMap<String, QueryTemplate>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        try {
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(stream)
            doc.documentElement.normalize()

            val endpointNode = doc.getElementsByTagName("endpoint").item(0) as? Element
            endpointNode?.let {
                val sparqlEndpoint =  it.getAttribute("sparql")
                val dictEndpoint = it.getAttribute("dictionary")

                sparqlEndpoint?.let {
                    BGServiceManager.serverPath = it
                }

                dictEndpoint?.let {
                    BGServiceManager.dictionaryServerPath = it
                }
            }


            // Parse RelationTypes
            val relationTypesNode = (doc.getElementsByTagName("relationTypes").item(0) as? Element) ?: throw Exception("relationTypes element not found in XML file!")
            val rList = relationTypesNode.getElementsByTagName("relationType") ?: throw Exception()

            for (index in 0..rList.length -1) {
                val element = rList.item(index) as? Element ?: continue
                val name = element.getAttribute("name")
                val defaultGraph = element.getAttribute("defaultGraph")
                val arbitraryLength = element.getAttribute("arbitraryLength").equals("true")
                val directed = !element.getAttribute("directed").equals("false")
                val expandable = element.getAttribute("expandable").equals("true")
                val uri = element.textContent
                val fromType = BGNodeType.forName(element.getAttribute("fromType"))
                val toType = BGNodeType.forName(element.getAttribute("toType"))
                val colorString = element.getAttribute("color")
                val color = if (colorString.length > 0) Color.decode(colorString) else Color.BLACK

                if (name != null && uri != null) {
                    val relationType = BGRelationType(uri, name, index, color ,defaultGraph, arbitraryLength, directed, expandable, fromType, toType)
                    cache.addRelationType(relationType)
                }
            }

            // Parsing datasetsources
            val sourcesTypeNode = (doc.getElementsByTagName("sources").item(0) as? Element) ?: throw Exception("sources element not found in XML file!")

            val relationTypeList = sourcesTypeNode.getElementsByTagName("relationType")
            for (j in 0..relationTypeList.length-1) {
                val rtElement = relationTypeList.item(j) as? Element ?: continue
                val rtGraph = rtElement.getAttribute("graph") ?: continue
                val rtUri = rtElement.getAttribute("uri") ?: continue
                val relationType = cache.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue

                val sourcesList = rtElement.getElementsByTagName("source") ?: throw Exception()

                for (index in 0..sourcesList.length - 1) {
                    val element = sourcesList.item(index) as? Element ?: continue
                    val name = element.getAttribute("name") ?: continue
                    val uri = element.getAttribute("uri") ?: continue
                    val source = BGDatasetSource(uri, name, relationType)
                    if (!cache.datasetSources.containsKey(relationType)) {
                        cache.datasetSources[relationType] = HashSet()
                    }
                    cache.datasetSources[relationType]?.add(source)
                }
            }


            // Parsing RelationMetadataTypes

            val relationMetadataNode = (doc.getElementsByTagName("relationMetadata").item(0) as? Element) ?: throw Exception("relationMetadata element not found in XML file!")
            val relationMetadataList = relationMetadataNode.getElementsByTagName("metadataType")

            for (index in 0..relationMetadataList.length-1) {
                val metadataElement = relationMetadataList.item(index) as? Element ?: continue
                val id = metadataElement.getAttribute("id") ?: continue
                val label = metadataElement.getAttribute("label") ?: continue
                val relationUri = metadataElement.getAttribute("relationUri") ?: continue
                val typeName = metadataElement.getAttribute("dataType") ?: continue
                val sparql = metadataElement.getAttribute("sparql")

                val dataType = when (typeName) {
                    "text" -> BGTableDataType.STRING
                    "number" -> BGTableDataType.DOUBLE
                    else -> null } ?: continue

                val relationTypes = ArrayList<BGRelationType>()

                val relationTypeList = metadataElement.getElementsByTagName("relationType")
                for (j in 0..relationTypeList.length-1) {
                    val rtElement = relationTypeList.item(j) as? Element ?: continue
                    val rtGraph = rtElement.getAttribute("graph") ?: continue
                    val rtUri = rtElement.textContent
                    val relationType = cache.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue
                    relationTypes.add(relationType)
                }

                val metadataType = BGRelationMetadataType(id, label, dataType, relationUri, relationTypes, sparql)

                cache.metadataTypes.put(metadataType.id, metadataType)
            }


            // Parse conversionTypes

            fun parseConversionElement(direction: BGConversionType.ConversionDirection, element: Element): BGConversionType? {
                val id = element.getAttribute("id") ?: return null
                val name = element.getAttribute("name") ?: return null
                val dataTypeString = element.getAttribute("dataType") ?: return null
                val biogwId = element.getAttribute("biogwId") ?: return null
                val lookupTypeString = element.getAttribute("lookup") ?: return null

                val dataType = when (dataTypeString) {
                    "string" -> BGTableDataType.STRING
                    "stringArray" -> BGTableDataType.STRINGARRAY
                    "doubleArray" -> BGTableDataType.DOUBLEARRAY
                    "intArray" -> BGTableDataType.INTARRAY
                    "double" -> BGTableDataType.DOUBLE
                    "boolean" -> BGTableDataType.BOOLEAN
                    "integer" -> BGTableDataType.INT
                    else -> null
                } ?: return null

                val lookupMethod = when (lookupTypeString) {
                    "replace" -> BGConversionType.LookupMethod.REPLACE
                    "extract" -> BGConversionType.LookupMethod.EXTRACT
                    "copy" -> BGConversionType.LookupMethod.COPY
                    "dictExactLookup" -> BGConversionType.LookupMethod.DICT_EXACT_LOOKUP
                    else -> null
                } ?: return null

                val template = element.getAttribute("template")
                val sparql = element.getAttribute("sparql")

                val conversion = BGConversionType(direction, id, name, dataType, biogwId, lookupMethod, template, sparql)
                return conversion
            }

            val importNodeConversions = HashSet<BGNodeConversionType>()
            val importEdgeConversions = HashSet<BGConversionType>()
            val exportEdgeConversions = HashSet<BGConversionType>()
            val exportNodeConversions = HashSet<BGConversionType>()

            val conversionsNode = doc.getElementsByTagName("conversionTypes").item(0) as? Element
            val importNode = conversionsNode?.getElementsByTagName("import")?.item(0) as? Element
            if (importNode != null) {
                val edges = importNode.getElementsByTagName("edge")
                for (index in 0..edges.length -1 ) {
                    val edgeElement = edges.item(index) as? Element ?: continue

                    val conversion = parseConversionElement(BGConversionType.ConversionDirection.EXPORT, edgeElement) ?: continue

                    importEdgeConversions.add(conversion)
                }

                val nodes = importNode.getElementsByTagName("node")
                for (index in 0..nodes.length -1 ) {
                    val node = nodes.item(index) as? Element ?: continue

                    val conversion = parseConversionElement(BGConversionType.ConversionDirection.IMPORT, node) ?: continue

                    val typeString = node.getAttribute("type") ?: continue
                    val nodeType = BGNodeType.forName(typeString) ?: continue

                    val nodeConversion = BGNodeConversionType(nodeType, conversion) ?: continue

                    importNodeConversions.add(nodeConversion)
                }
            }

            val exportNode = conversionsNode?.getElementsByTagName("export")?.item(0) as? Element
            if (exportNode != null) {
                val edges = exportNode.getElementsByTagName("edge")
                for (index in 0..edges.length -1 ) {
                    val edgeElement = edges.item(index) as? Element ?: continue

                    val conversion = parseConversionElement(BGConversionType.ConversionDirection.EXPORT, edgeElement) ?: continue

                    exportEdgeConversions.add(conversion)
                }

                val nodes = exportNode.getElementsByTagName("node")
                for (index in 0..nodes.length -1 ) {
                    val node = nodes.item(index) as? Element ?: continue

                    val conversion = parseConversionElement(BGConversionType.ConversionDirection.EXPORT, node) ?: continue

                    val typeString = node.getAttribute("type") ?: continue
                    val nodeType = BGNodeType.forName(typeString) ?: continue

                    val nodeConversion = BGNodeConversionType(nodeType, conversion) ?: continue

                    exportNodeConversions.add(nodeConversion)
                }
            }

            cache.importEdgeConversionTypes = importEdgeConversions
            cache.importNodeConversionTypes = importNodeConversions
            cache.exportEdgeConversionTypes = exportEdgeConversions
            cache.exportNodeConversionTypes = exportNodeConversions

            // Parse QueryConstraints. Must be after parsing RelationTypes, as it relies on finding relation types in cache.

            val queryConstraintNode = (doc.getElementsByTagName("queryConstraints").item(0) as? Element) ?: throw Exception("queryConstraints element not found in XML file!")
            val constraintList = queryConstraintNode.getElementsByTagName("constraint")

            for (index in 0..constraintList.length-1) {
                val constraint = constraintList.item(index) as? Element ?: continue
                val id = constraint.getAttribute("id") ?: continue
                val label = constraint.getAttribute("label") ?: continue
                val columns = constraint.getAttribute("columns").toIntOrNull()
                val typeName = constraint.getAttribute("type") ?: continue

                val type = when (typeName) {
                    "combobox" -> BGQueryConstraint.InputType.COMBOBOX
                    "text" -> BGQueryConstraint.InputType.TEXT
                    "number" -> BGQueryConstraint.InputType.NUMBER
                    else -> null } ?: continue

                val queryConstraint = BGQueryConstraint(id, label, type, columns)

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
                cache.queryConstraints.put(queryConstraint.id, queryConstraint)
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
                    val queryDescription = qElement.getElementsByTagName("description").item(0).textContent
                    val sparqlString = qElement.getElementsByTagName("sparql").item(0).textContent.replace("\t", "") // Remove tabs from the XML file. (They might be added "for show").

                    val returnType = when (returnTypeString) {
                        "nodeList" -> BGReturnType.NODE_LIST
                        "nodeListDescription" -> BGReturnType.NODE_LIST_DESCRIPTION
                        "relationTriple" -> BGReturnType.RELATION_TRIPLE_GRAPHURI
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
                                "uniprot_id" -> BGQueryParameter.ParameterType.PROTEIN
                                "ontology" -> BGQueryParameter.ParameterType.GO_TERM
                                "gene_id" -> BGQueryParameter.ParameterType.GENE
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