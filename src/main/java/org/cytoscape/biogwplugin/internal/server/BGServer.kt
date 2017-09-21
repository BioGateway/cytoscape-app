package org.cytoscape.biogwplugin.internal.server

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.cytoscape.biogwplugin.internal.model.BGRelationType
import org.cytoscape.biogwplugin.internal.parser.*
import org.cytoscape.biogwplugin.internal.query.BGNodeFetchQuery
import org.cytoscape.biogwplugin.internal.query.BGQueryParameter
import org.cytoscape.biogwplugin.internal.query.QueryTemplate
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.model.CyNetwork
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

class BGServer(private val serviceManager: BGServiceManager) {

    class BGCache() {
        // A cache of BGNodes, which are a local representation of the node found on the server.
        // Note that this cache is independent of the CyNodes and CyNetworks.
        var nodeCache = HashMap<String, BGNode>()

        var relationTypeMap = HashMap<String, BGRelationType>()

        val relationTypeDescriptions: LinkedHashMap<String, BGRelationType> get() {
            val relationTypes = LinkedHashMap<String, BGRelationType>()
            val relations = relationTypeMap.values.sortedBy { it.number }
            for (relation in relations) {
                relationTypes.put(relation.description, relation)
            }
            return relationTypes
        }

        var queryTemplates = HashMap<String, QueryTemplate>()

        fun addNode(node: BGNode) {
            // Only add a node if it's not already been added.
            // TODO: Merge the new node data into the previously cached node.
            if (nodeCache.contains(node.uri)) return
            nodeCache.set(node.uri, node)
        }

        fun getRelationUriForName(name: String): String? {
            val keys = relationTypeMap.filter { it.value.name == name }.keys.toTypedArray()
            if (keys.size == 1) {
                return keys.first()
            }
            return null
        }
    }

    val cache = BGCache()
    val parser = BGParser(serviceManager)
    val networkBuilder = BGNetworkBuilder(serviceManager)

    init {
        loadXMLFileFromServer()
    }

    fun getNodeFromCacheOrNetworks(newNode: BGNode): BGNode {
        // Check if the node already exists in the cache.
        var node = cache.nodeCache[newNode.uri]

        if (node == null) {
            // If it doesn't exist, add the new node.
            node = newNode

            if ((node.description == null) or (node.name == null)) {
                val nodeFromNetwork = getNodeDataFromNetworks(node.uri)
                if (nodeFromNetwork != null) {
                    node = nodeFromNetwork
                    println("CyNetwork hit: "+node.name)
                }
            }
        } else {
            println("Cache hit: "+node.name)
        }
        return node
    }

    fun loadDataForNode(node: BGNode) {
        if ((node.description == null) or (node.name == null)) {
            // Query the server for more info about this node.
            if (!node.uri.startsWith("http")) {
                return
            }
            getNodeFromServer(node.uri) {
                if (it != null) {
                    println("Cache miss: "+it.name)
                    node.name = it.name
                    node.description = it.description
                    node.isLoaded = true
                    cache.addNode(node)
                    for (cyNode in node.cyNodes) {
                        it.name?.let {
                            cyNode.setName(it, cyNode.networkPointer)
                        }
                        it.description?.let {
                            cyNode.setDescription(it, cyNode.networkPointer)
                        }
                    }
                }
            }
        } else {
            cache.addNode(node)
            node.isLoaded = true
        }
    }


    private fun getNodeDataFromNetworks(uri: String): BGNode? {
        for (network in serviceManager.networkManager.networkSet) {
            val node = getNodeFromCyNetwork(uri, network)
            if (node != null) {
                cache.addNode(node)
                node.isLoaded = true
                return node
            }
        }
        return null
    }

    private fun  getNodeFromServer(uri: String, completion: (BGNode?) -> Unit) {
        if (!uri.startsWith("http://")) {
            completion(null)
            return
        }
        val query = BGNodeFetchQuery(serviceManager, uri, serviceManager.server.parser, BGReturnType.NODE_LIST_DESCRIPTION)
        // TODO: Use the CloseableHttpClient. Because this might cause things to take a looooong time.
        val stream = query.encodeUrl()?.openStream()
        if (stream != null) {
            val reader = BufferedReader(InputStreamReader(stream))
            parser.parseNodesToTextArray(reader, BGReturnType.NODE_LIST_DESCRIPTION) {
                val data = it ?: throw Exception("Invalid return data!")
                val node = data.nodeData.get(uri)
                completion(node)
            }
        }
    }

    private fun getNodeFromCyNetwork(uri: String, network: CyNetwork): BGNode? {
        val nodeTable = network.defaultNodeTable
        val nodes = networkBuilder.getCyNodesWithValue(network, nodeTable, Constants.BG_FIELD_IDENTIFIER_URI, uri)

        for (cyNode in nodes) {
            val nodeName = nodeTable.getRow(cyNode.suid).get(Constants.BG_FIELD_NAME, String::class.java)
            val description = nodeTable.getRow(cyNode.suid).get(Constants.BG_FIELD_DESCRIPTION, String::class.java)
            if (nodeName != null && description != null) {
                val node = BGNode(uri, nodeName, description)
                return node
            }
        }
        return null
    }


    private fun loadXMLFileFromServer() {
        try {
            val queryFileUrl = URL(Constants.BG_CONFIG_FILE_URL)
            val connection = queryFileUrl.openConnection()
            val inputStream = connection.getInputStream()
            parseXMLConfigFile(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun parseXMLConfigFile(stream: InputStream) {

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
                val uri = element?.textContent

                if (name != null && uri != null) {
                    val relationType = BGRelationType(uri, name, index, defaultGraph)
                    relationTypes.put(uri, relationType)
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

    fun createBiopaxURL(queryData: String, returnType: String, options: String): URL? {
        val queryURL: URL
        try {
            queryURL = URL(serviceManager.serverPath + "?query=" + URLEncoder.encode(queryData, "UTF-8") + "&format=" + URLEncoder.encode(returnType, "UTF-8") + "&" + options)

        } catch (e: MalformedURLException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return null

        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            return null
        }

        return queryURL
    }
}
