package eu.biogateway.app.internal.parser

import eu.biogateway.app.internal.BGBundleContext
import eu.biogateway.app.internal.BGServiceManager
import eu.biogateway.app.internal.model.*
import eu.biogateway.app.internal.query.BGQueryTemplate
import eu.biogateway.app.internal.server.BGSettings
import eu.biogateway.app.internal.util.Utility
import org.osgi.framework.Version
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.awt.Color
import java.io.InputStream
import javax.swing.JOptionPane
import javax.xml.parsers.DocumentBuilderFactory


// Extension to get the direct child with name, instead of searching the whole DOM.
fun Element.getChildWithName(name: String): Element? {
    var child: Node? = this.firstChild
    while (child != null) {
        if (child is Element && name == child.nodeName) {
            return child
        }
        child = child.nextSibling
    }
    return null
}

object BGConfigParser {

    var versions = HashMap<Int, Element>()

    fun parseConfigElement(configDoc: Element, config: BGConfig) {
        val endpointNode = configDoc.getElementsByTagName("endpoint").item(0) as? Element
        endpointNode?.let {
            val sparqlEndpoint = it.getAttribute("sparql")
            val dictEndpoint = it.getAttribute("dictionary")

            sparqlEndpoint?.let {
                BGServiceManager.serverPath = it
            }

            dictEndpoint?.let {
                BGServiceManager.dictionaryServerPath = it
            }
        }

        // Parse the dataset graphs:
        configDoc.getChildWithName("graphs")?.let { graphsNode ->
            // ?: throw Exception("graphs element not found in XML file!")

            val graphMap = HashMap<String, String>()

            val graphs = graphsNode.getElementsByTagName("graph")
            for (index in 0..graphs.length - 1) {
                val graphElement = graphs.item(index) as? Element ?: continue
                val uri = graphElement.getAttribute("uri") ?: continue
                if (uri.isEmpty()) continue
                val name = graphElement.getAttribute("name") ?: continue
                graphMap[name] = uri
            }
            config.datasetGraphs = graphMap
        }

        // Parse the nodetypes:

        val nodeTypeNode =
            configDoc.getChildWithName("nodetypes") ?: throw Exception("nodetypes element not found in XML file!")


        val nodeTypes = nodeTypeNode.getElementsByTagName("type")
        for (index in 0..nodeTypes.length - 1) {
            val nodeTypeElement = nodeTypes.item(index) as? Element ?: continue
            val id = nodeTypeElement.getAttribute("id") ?: continue
            if (id.isEmpty()) continue

            // TODO: The null-checks are useless, as non-existing attributes will be returned as empty strings.
            val name = nodeTypeElement.getAttribute("name")
            val uriPattern = nodeTypeElement.getAttribute("uriPattern")
            val default = nodeTypeElement.getAttribute("default").equals("true")
            val nodeTypeClassId = nodeTypeElement.getAttribute("class")
            val nodeTypeClass = BGNodeType.BGNodeTypeClass.forId(nodeTypeClassId) ?: continue
            val metadataGraph = if (nodeTypeElement.getAttribute("metadatagraph")
                    .isNotEmpty()
            ) nodeTypeElement.getAttribute("metadatagraph") else null
            val autocompleteTypeId = nodeTypeElement.getAttribute("autocompleteType")
            val autocompleteType = BGNodeType.BGAutoCompleteType.forId(autocompleteTypeId)

            val nodeType = BGNodeType(id, name, uriPattern, nodeTypeClass, metadataGraph, autocompleteType, default)
            config.nodeTypes[id] = nodeType
        }


        // Parse RelationTypes
        //val relationTypesNode = (configDoc.getElementsByTagName("relationTypes").item(0) as? Element) ?: throw Exception("relationTypes element not found in XML file!")
        val relationTypesNode = configDoc.getChildWithName("relationTypes")
            ?: throw Exception("relationTypes element not found in XML file!")
        val rList = relationTypesNode.getElementsByTagName("relationType") ?: throw Exception()

        for (index in 0..rList.length - 1) {
            val element = rList.item(index) as? Element ?: continue
            val name = element.getAttribute("name")
            val defaultGraph = element.getAttribute("defaultGraph")
            val graphLabel = element.getAttribute("graphLabel")
            val enabledByDefault = element.getAttribute("defaultEnabled").toBoolean()
            val arbitraryLength = element.getAttribute("arbitraryLength").equals("true")
            val directed = !element.getAttribute("directed").equals("false")
            val expandable = element.getAttribute("expandable").equals("true")
            val uri = element.textContent
            val fromType = config.nodeTypes.get(element.getAttribute("fromType"))
            val toType = config.nodeTypes.get(element.getAttribute("toType"))
            val colorString = element.getAttribute("color")
            val color = if (colorString.length > 0) Color.decode(colorString) else Color.BLACK

            var graph: BGGraph? = null
            if (defaultGraph != null) {
                graph = BGGraph(defaultGraph, graphLabel)
            }

            if (name != null && uri != null) {
                val relationType = BGRelationType(
                    uri,
                    name,
                    index,
                    color,
                    graph,
                    arbitraryLength,
                    directed,
                    expandable,
                    fromType,
                    toType
                )
                relationType.enabledByDefault = enabledByDefault
                config.addRelationType(relationType)
            }
        }

        // Parsing taxa
        (configDoc.getChildWithName("taxa"))?.let { taxaNode ->
            // ?: throw Exception("sources element not found in XML file!")

            val taxaList = taxaNode.getElementsByTagName("taxon")
            for (j in 0..taxaList.length - 1) {
                val element = taxaList.item(j) as? Element ?: continue
                val name = element.getAttribute("name") ?: continue
                val id = element.getAttribute("id") ?: continue
                val uri = element.textContent
                val taxon = BGTaxon(id, name, uri)
                config.availableTaxa[uri] = taxon
            }
        }

        // Parsing datasetsources
        (configDoc.getChildWithName("sources"))?.let { sourcesTypeNode ->
            // ?: throw Exception("sources element not found in XML file!")
            val sourceRelationUri = sourcesTypeNode.getAttribute("sourceRelation")
            config.datasetSourceRelationTypeUri = sourceRelationUri

            val relationTypeList = sourcesTypeNode.getElementsByTagName("relationType")
            for (j in 0..relationTypeList.length - 1) {
                val rtElement = relationTypeList.item(j) as? Element ?: continue
                val rtGraph = rtElement.getAttribute("graph") ?: continue
                val rtGraphLabel = rtElement.getAttribute("graphLabel")
                val rtUri = rtElement.getAttribute("uri") ?: continue
                val relationType = config.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue

                val sourcesList = rtElement.getElementsByTagName("source") ?: throw Exception()

                for (index in 0..sourcesList.length - 1) {
                    val element = sourcesList.item(index) as? Element ?: continue
                    val name = element.getAttribute("name") ?: continue
                    val uri = element.getAttribute("uri") ?: continue
                    val source = BGDatasetSource(uri, name, relationType)
                    if (!config.datasetSources.containsKey(relationType)) {
                        config.datasetSources[relationType] = HashSet()
                    }
                    config.datasetSources[relationType]?.add(source)
                }
            }
        }

        // Parsing Statement context menu actions
        (configDoc.getChildWithName("statementContextMenu"))?.let { statementMenuActionNode ->
            fun parseNodes(nodeList: NodeList, type: BGStatementContextMenuAction.ActionType) {
                for (index in 0..nodeList.length - 1) {
                    val menuActionElement = nodeList.item(index) as? Element ?: continue
                    val id = menuActionElement.getAttribute("id") ?: continue
                    val resourceURI = menuActionElement.getAttribute("resourceURI") ?: continue
                    val singleLabel = menuActionElement.getAttribute("singleLabel") ?: continue
                    val multipleLabel = menuActionElement.getAttribute("multipleLabel") ?: continue

                    val relationTypes = ArrayList<BGRelationType>()

                    val relationTypeList = menuActionElement.getElementsByTagName("relationType")
                    for (j in 0..relationTypeList.length - 1) {
                        val rtElement = relationTypeList.item(j) as? Element ?: continue
                        val rtGraph = rtElement.getAttribute("graph") ?: continue
                        val rtUri = rtElement.textContent
                        val relationType = config.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue
                        relationTypes.add(relationType)
                    }
                    val menuAction = BGStatementContextMenuAction(id, resourceURI, singleLabel, multipleLabel, relationTypes, type)
                    config.statementContextMenuActions[id] = menuAction
                }
            }

            val openURIActionList = statementMenuActionNode.getElementsByTagName("openURIAction")
            parseNodes(openURIActionList, BGStatementContextMenuAction.ActionType.OPEN_URI)
            val copyURIActionList = statementMenuActionNode.getElementsByTagName("copyURIAction")
            parseNodes(copyURIActionList, BGStatementContextMenuAction.ActionType.COPY_URI)


        }

        // Parsing RelationMetadataTypes

        (configDoc.getChildWithName("relationMetadata"))?.let { relationMetadataNode -> // ?: throw Exception("relationMetadata element not found in XML file!")
            val relationMetadataList = relationMetadataNode.getElementsByTagName("metadataType")

            for (index in 0..relationMetadataList.length - 1) {
                val metadataElement = relationMetadataList.item(index) as? Element ?: continue
                val id = metadataElement.getAttribute("id") ?: continue
                val label = metadataElement.getAttribute("label") ?: continue
                val relationUri = metadataElement.getAttribute("relationUri") ?: continue
                val typeName = metadataElement.getAttribute("dataType") ?: continue
                val sparql = metadataElement.getAttribute("sparql")

                val dataType = BGTableDataType.getTypeFromString(typeName) ?: continue

                val relationTypes = ArrayList<BGRelationType>()

                val relationTypeList = metadataElement.getElementsByTagName("relationType")
                for (j in 0..relationTypeList.length - 1) {
                    val rtElement = relationTypeList.item(j) as? Element ?: continue
                    val rtGraph = rtElement.getAttribute("graph") ?: continue
                    val rtUri = rtElement.textContent
                    val relationType = config.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue
                    relationTypes.add(relationType)
                }
                val conversions = HashMap<String, String>()

                val conversionList = metadataElement.getElementsByTagName("conversion")
                for (j in 0..conversionList.length - 1) {
                    val cvrtElement = conversionList.item(j) as? Element ?: continue
                    val fromValue = cvrtElement.getAttribute("fromValue") ?: continue
                    val toValue = cvrtElement.getAttribute("toValue") ?: continue
                    conversions[fromValue] = toValue
                }

                val metadataType =
                    BGRelationMetadataType(id, label, dataType, relationUri, relationTypes, sparql, conversions)

                metadataElement.getAttribute("scalingFactor")?.toDoubleOrNull()?.let {
                    metadataType.scalingFactor = it
                }

                config.edgeMetadataTypes[metadataType.id] = metadataType
            }
        }

        // Parsing NodeMetadataTypes

        (configDoc.getChildWithName("nodeMetadata"))?.let { nodeMetadataNode -> // ?: throw Exception("nodeMetadata element not found in XML file!")
            val nodeMetadataList = nodeMetadataNode.getElementsByTagName("metadataType")

            for (index in 0..nodeMetadataList.length - 1) {
                val metadataElement = nodeMetadataList.item(index) as? Element ?: continue
                val id = metadataElement.getAttribute("id") ?: continue
                val label = metadataElement.getAttribute("label") ?: continue
                val nodeTypeString = metadataElement.getAttribute("nodeType") ?: continue
                val typeName = metadataElement.getAttribute("dataType") ?: continue
                val sparql = metadataElement.getAttribute("sparql")
                val template = metadataElement.getAttribute("template")
                val restGet = metadataElement.getAttribute("restGet")
                val jsonField = metadataElement.getAttribute("jsonField")


                val dataType = BGTableDataType.getTypeFromString(typeName) ?: continue
                val nodeType = config.nodeTypes.get(nodeTypeString) ?: continue


                val metadataType =
                    BGNodeMetadataType(id, label, dataType, nodeType, template, sparql, restGet, jsonField)


                config.nodeMetadataTypes[metadataType.id] = metadataType
            }
        }


        // Parse conversionTypes

        fun parseConversionElement(
            direction: BGConversionType.ConversionDirection,
            element: Element
        ): BGConversionType? {
            val id = element.getAttribute("id") ?: return null
            val name = element.getAttribute("name") ?: return null
            val dataTypeString = element.getAttribute("dataType") ?: return null
            val biogwId = element.getAttribute("biogwId") ?: return null
            val lookupTypeString = element.getAttribute("lookup") ?: return null
            val dataType = BGTableDataType.getTypeFromString(dataTypeString) ?: return null

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

        val conversionsNode = configDoc.getChildWithName("conversionTypes")
        val importNode = conversionsNode?.getElementsByTagName("import")?.item(0) as? Element
        if (importNode != null) {
            val edges = importNode.getElementsByTagName("edge")
            for (index in 0..edges.length - 1) {
                val edgeElement = edges.item(index) as? Element ?: continue

                val conversion =
                    parseConversionElement(BGConversionType.ConversionDirection.EXPORT, edgeElement) ?: continue

                importEdgeConversions.add(conversion)
            }

            val nodes = importNode.getElementsByTagName("node")
            loop@ for (index in 0..nodes.length - 1) {
                val node = nodes.item(index) as? Element ?: continue

                val conversion = parseConversionElement(BGConversionType.ConversionDirection.IMPORT, node) ?: continue

                val typeString = node.getAttribute("type") ?: continue

                val nodeType = when (typeString == "undefined") {
                    true -> BGNodeType.UNDEFINED
                    false -> {
                        config.nodeTypes.get(typeString) ?: continue@loop
                    }
                }

                val nodeConversion = BGNodeConversionType(nodeType, conversion)

                importNodeConversions.add(nodeConversion)
            }
        }

        val exportNode = conversionsNode?.getElementsByTagName("export")?.item(0) as? Element
        if (exportNode != null) {
            val edges = exportNode.getElementsByTagName("edge")
            for (index in 0..edges.length - 1) {
                val edgeElement = edges.item(index) as? Element ?: continue

                val conversion =
                    parseConversionElement(BGConversionType.ConversionDirection.EXPORT, edgeElement) ?: continue

                exportEdgeConversions.add(conversion)
            }

            val nodes = exportNode.getElementsByTagName("node")
            for (index in 0..nodes.length - 1) {
                val node = nodes.item(index) as? Element ?: continue

                val conversion = parseConversionElement(BGConversionType.ConversionDirection.EXPORT, node) ?: continue

                val typeString = node.getAttribute("type") ?: continue
                val nodeType = config.nodeTypes.get(typeString) ?: continue

                val nodeConversion = BGNodeConversionType(nodeType, conversion) ?: continue

                exportNodeConversions.add(nodeConversion)
            }
        }

        config.importEdgeConversionTypes = importEdgeConversions
        config.importNodeConversionTypes = importNodeConversions
        config.exportEdgeConversionTypes = exportEdgeConversions
        config.exportNodeConversionTypes = exportNodeConversions


        // Parse Node Filters. Must be after parsing NodeTypes.
        (configDoc.getChildWithName("nodeFilters"))?.let {
            val nodeFilterList = it.getElementsByTagName("filter")

            for (index in 0..nodeFilterList.length - 1) {
                val filterElement = nodeFilterList.item(index) as? Element ?: continue
                val id = filterElement.getAttribute("id") ?: continue
                val enabledByDefault = filterElement.getAttribute("defaultEnabled").toBoolean()
                val label = filterElement.getAttribute("label") ?: continue
                val inputTypeString = filterElement.getAttribute("inputType") ?: continue
                val inputType = when (inputTypeString) {
                    "static" -> BGNodeFilter.InputType.STATIC
                    "combobox" -> BGNodeFilter.InputType.COMBOBOX
                    "text" -> BGNodeFilter.InputType.TEXT
                    "number" -> BGNodeFilter.InputType.NUMBER
                    else -> {
                        null
                    }
                } ?: continue

                val ftId = filterElement.getAttribute("filterType") ?: continue
                val ftFilterString = filterElement.getAttribute("filterString") ?: continue
                val ftPositionString = filterElement.getAttribute("position")

                val position = when (ftPositionString) {
                    "prefix" -> BGNodeFilterType.FilterPosition.PREFIX
                    "suffix" -> BGNodeFilterType.FilterPosition.SUFFIX
                    else -> {
                        null
                    }
                }

                val filterType = when (ftId) {
                    "uriFilter" -> {
                        if (position != null) {
                            BGUriFilter(ftFilterString, position)
                        } else {
                            null
                        }
                    }
                    "nameFilter" -> {
                        if (position != null) {
                            BGNameFilter(ftFilterString, position)
                        } else {
                            null
                        }
                    }
                    else -> {
                        null
                    }
                } ?: continue

                val nodeTypes = ArrayList<BGNodeType>()

                val nodeTypeList = filterElement.getElementsByTagName("nodeType")
                for (j in 0..nodeTypeList.length - 1) {
                    val ntElement = nodeTypeList.item(j) as? Element ?: continue
                    val ntId = ntElement.getAttribute("id") ?: continue
                    val nodeType = config.nodeTypes[ntId] ?: continue
                    nodeTypes.add(nodeType)
                }
                val nodeFilter = BGNodeFilter(id, label, inputType, filterType, nodeTypes, enabledByDefault)
                config.nodeFilters[id] = nodeFilter

            }
        }

        fun parseQueryConstraint(constraint: Element): BGQueryConstraint? {
            val id = constraint.getAttribute("id") ?: return null
            val label = constraint.getAttribute("label") ?: return null
            val columns = constraint.getAttribute("columns").toIntOrNull()
            val typeName = constraint.getAttribute("type") ?: return null

            val type = when (typeName) {
                "combobox" -> BGQueryConstraint.InputType.COMBOBOX
                "text" -> BGQueryConstraint.InputType.TEXT
                "number" -> BGQueryConstraint.InputType.NUMBER
                "boolean" -> BGQueryConstraint.InputType.BOOLEAN
                else -> null
            } ?: return null

            val queryConstraint = BGQueryConstraint(id, label, type, columns)

            val actionsList = constraint.getElementsByTagName("action")

            for (i in 0..actionsList.length - 1) {
                val actionElement = actionsList.item(i) as? Element ?: continue
                val parameterString = actionElement.getAttribute("parameter") ?: continue
                val actionParameter = when (parameterString) {
                    "first" -> BGQueryConstraint.ActionParameter.FIRST
                    "last" -> BGQueryConstraint.ActionParameter.LAST
                    "both" -> BGQueryConstraint.ActionParameter.BOTH
                    else -> null
                } ?: continue
                val actionGraph = actionElement.getAttribute("graph") ?: continue
                val sparqlElement = actionElement.getElementsByTagName("sparql").item(0) as? Element ?: continue
                val sparqlString = sparqlElement.textContent

                val relationTypes = ArrayList<BGRelationType>()

                val relationTypeList = actionElement.getElementsByTagName("relationType")
                for (j in 0..relationTypeList.length - 1) {
                    val rtElement = relationTypeList.item(j) as? Element ?: continue
                    val rtGraph = rtElement.getAttribute("graph") ?: continue
                    val rtUri = rtElement.textContent
                    val relationType = config.getRelationTypeForURIandGraph(rtUri, rtGraph) ?: continue
                    relationTypes.add(relationType)
                }
                val action =
                    BGQueryConstraint.ConstraintAction(actionParameter, actionGraph, relationTypes, sparqlString)
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
            return queryConstraint
        }

        // Parse QueryConstraints. Must be after parsing RelationTypes, as it relies on finding relation types in config.

        (configDoc.getChildWithName("queryConstraints"))?.let { queryConstraintNode -> // ?: throw Exception("queryConstraints element not found in XML file!")
            val constraintList = queryConstraintNode.getElementsByTagName("constraint")

            for (index in 0..constraintList.length - 1) {
                val element = constraintList.item(index) as? Element ?: continue
                val queryConstraint = parseQueryConstraint(element) ?: continue
                config.queryConstraints[queryConstraint.id] = queryConstraint
            }
        }

        (configDoc.getChildWithName("taxonConstraint"))?.let { taxonConstraintNode -> // ?: throw Exception("queryConstraints element not found in XML file!")
            config.taxonConstraint = parseQueryConstraint(taxonConstraintNode) ?: return
        }

        // Parse example queries

        configDoc.getChildWithName("exampleQueries")?.let { exampleQueryNode ->
            val exampleQueryElements = exampleQueryNode.getElementsByTagName("query")

            for (index in 0..exampleQueryElements.length - 1) {
                val queryElement = exampleQueryElements.item(index) as? Element ?: continue
                val name = queryElement.getAttribute("name")
                val sparql = queryElement.textContent.replace("\t", "")

                if (name.isNullOrEmpty()) continue
                if (sparql.isNullOrEmpty()) continue

                val exampleQuery = BGExampleQuery(name, sparql)
                config.exampleQueries.add(exampleQuery)
            }
        }
        // Parse Search Types

        configDoc.getChildWithName("searchTypes")
            ?.let { searchTypesNode ->  //?: throw Exception("searchTypes element not found in XML file!")
                val searchTypesElements = searchTypesNode.getElementsByTagName("searchType")

                for (index in 0..searchTypesElements.length - 1) {
                    val searchTypeElement = searchTypesElements.item(index) as? Element ?: continue
                    val id = searchTypeElement.getAttribute("id")
                    val title = searchTypeElement.getAttribute("title")
                    val returnType = searchTypeElement.getAttribute("returnType")
                    val nodeTypeString = searchTypeElement.getAttribute("nodeType")
                    val arraySearch = searchTypeElement.getAttribute("arraySearch") == "true"
                    val restPath = searchTypeElement.getAttribute("restPath")
                    val httpMethodString = searchTypeElement.getAttribute("httpMethod")
                    val parameters = searchTypeElement.getAttribute("parameters")
                    val searchPrefix = searchTypeElement.getAttribute("searchPrefix")


                    val httpMethod = when (httpMethodString) {
                        "GET" -> BGSearchType.HTTPOperation.GET
                        "POST" -> BGSearchType.HTTPOperation.POST
                        else -> null
                    } ?: continue
                    val nodeType = config.nodeTypes[nodeTypeString] ?: continue

                    val searchType = BGSearchType(
                        id,
                        title,
                        nodeType,
                        returnType,
                        restPath,
                        arraySearch,
                        httpMethod,
                        searchPrefix,
                        parameters
                    )

                    config.searchTypes.add(searchType)
                }
            }

        // Parse the visual style config:
        (configDoc.getChildWithName("visualStyle"))?.let { visualStyleNode ->
            //?: throw Exception("visualStyle element not found in XML file!")

            val visualStyleConfig = BGVisualStyleConfig()

            val edgeColors = visualStyleNode.getElementsByTagName("edgeColor")
            for (index in 0..edgeColors.length - 1) {
                val styleElement = edgeColors.item(index) as? Element ?: continue
                val uri = styleElement.getAttribute("uri") ?: continue
                if (uri.isEmpty()) continue
                val colorString = styleElement.getAttribute("color") ?: continue
                val color = if (!colorString.isEmpty()) Color.decode(colorString) else continue
                visualStyleConfig.edgeColors[uri] = color
            }
            val nodeColors = visualStyleNode.getElementsByTagName("nodeColor")
            for (index in 0..nodeColors.length - 1) {
                val styleElement = nodeColors.item(index) as? Element ?: continue
                val type = styleElement.getAttribute("type") ?: continue
                if (type.isEmpty()) continue
                val colorString = styleElement.getAttribute("color") ?: continue
                val color = if (!colorString.isEmpty()) Color.decode(colorString) else continue
                visualStyleConfig.nodeColors[type] = color
            }

            val edgeLines = visualStyleNode.getElementsByTagName("edgeLine")
            for (index in 0..edgeLines.length - 1) {
                val styleElement = edgeLines.item(index) as? Element ?: continue
                val uri = styleElement.getAttribute("uri") ?: continue
                if (uri.isEmpty()) continue
                val lineTypeName = styleElement.getAttribute("lineType") ?: continue
                val lineType = visualStyleConfig.lineTypeMapping.get(lineTypeName) ?: continue
                visualStyleConfig.edgeLineTypes[uri] = lineType
            }

            val nodeShapes = visualStyleNode.getElementsByTagName("nodeShape")
            for (index in 0..nodeShapes.length - 1) {
                val styleElement = nodeShapes.item(index) as? Element ?: continue
                val type = styleElement.getAttribute("type") ?: continue
                if (type.isEmpty()) continue
                val nodeShapeName = styleElement.getAttribute("shape") ?: continue
                val nodeShape = visualStyleConfig.nodeShapeMapping.get(nodeShapeName) ?: continue
                visualStyleConfig.nodeShapes[type] = nodeShape

                val nodeWidth = styleElement.getAttribute("width").toDoubleOrNull()
                if (nodeWidth != null) {
                    visualStyleConfig.nodeWidths[type] = nodeWidth
                }
                val nodeHeight = styleElement.getAttribute("height").toDoubleOrNull()
                if (nodeHeight != null) {
                    visualStyleConfig.nodeHeights[type] = nodeHeight
                }
            }
            val nodeBorderColors = visualStyleNode.getElementsByTagName("nodeBorderColor")
            for (index in 0..nodeBorderColors.length - 1) {
                val styleElement = nodeBorderColors.item(index) as? Element ?: continue
                val score = styleElement.getAttribute("annotationScore") ?: continue
                if (score.isEmpty()) continue
                val colorString = styleElement.getAttribute("color") ?: continue
                val color = if (!colorString.isEmpty()) Color.decode(colorString) else continue
                visualStyleConfig.nodeBorderColors[score] = color
            }


            config.visualStyleConfig = visualStyleConfig
        }
    }

    fun parseConfigVersion(version: Int, config: BGConfig) {
        val configDoc = versions[version] ?: return
        parseConfigElement(configDoc, config)
    }

    /// Parses the config file from the input stream. Returns an error message if something went wrong, returns null if everything went well.
    fun parseXMLConfigFile(stream: InputStream, config: BGConfig, settings: BGSettings) {

        val queryTemplateHashMap = java.util.HashMap<String, BGQueryTemplate>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        try {
            val dBuilder = dbFactory.newDocumentBuilder()
            val rootDoc = dBuilder.parse(stream)
            rootDoc.documentElement.normalize()

            val currentVersion = BGBundleContext.version ?: throw Exception("OSGi Bundle Version unavailable!")
            val minorVersion = "${currentVersion.major}.${currentVersion.minor}${currentVersion.qualifier}"

            val buildNumberNode = rootDoc.getElementsByTagName("latestVersion").item(0) as? Element
            buildNumberNode?.let {
                val version = Version(it.textContent)
                config.latestVersion = version
            }
            val configElementList = rootDoc.getElementsByTagName("config")

            // Attempt to fetch the config for the current version.
            for (index in 0..configElementList.length-1) {
                val element = configElementList.item(index) as? Element ?: continue
                val configVersion = element.getAttribute("version").toIntOrNull() ?: continue
                versions.put(configVersion, element)
            }
            settings.availableVersions = versions.keys.toTypedArray()
            val version = if (settings.databaseVersion > 0) settings.databaseVersion else versions.keys.max()
            val configDoc = versions[version]

            // TODO: Check if this app version is supported by the selected config.

            /*
            if (configDoc == null) {
                var matchingConfigs = mutableMapOf<String, Element>()
                for (index in 0..configElementList.length-1) {
                    val element = configElementList.item(index) as? Element ?: continue
                    val configVersionString = element.getAttribute("version")
                    val configVersion = Version(configVersionString)
                    if (configVersion.major == currentVersion.major && configVersion.minor < currentVersion.minor) {
                        matchingConfigs[configVersion.qualifier] = element
                    }
                }
                val latestMatching = matchingConfigs.keys.sortedDescending().firstOrNull()
                if (latestMatching != null) {
                    configDoc = matchingConfigs[latestMatching]
                }
            }
            */
            // Didn't find a matching config, give up.

            if (configDoc == null) {
                val message = "Your current app version ($minorVersion) is not recognized by the server as a supported version of BioGateway. \nPress OK to download the latest version from apps.cytoscape.org."
                val response = JOptionPane.showOptionDialog(null, message, "Unsupported app version", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null)
                if (response == JOptionPane.OK_OPTION) {
                    Utility.openBrowser("https://apps.cytoscape.org/apps/biogatewaycytoscapeplugin")
                }
                return
            }

            parseConfigElement(configDoc, config)

        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(null, "Unable to load BioGateway Config File. \nIf you are using a custom configuration XML file, make sure it is BioGateway compliant. \n\nError: \n" + e.localizedMessage, "Unable to load BioGateway Config File", JOptionPane.ERROR_MESSAGE)
        }
        config.queryTemplates = queryTemplateHashMap
    }
}

