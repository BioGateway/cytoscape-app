package org.cytoscape.biogwplugin.internal.model

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.libs.JCheckBoxTree
import org.cytoscape.biogwplugin.internal.parser.*
import org.cytoscape.biogwplugin.internal.query.*
import org.cytoscape.biogwplugin.internal.server.BGSettings
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.biogwplugin.internal.util.Constants.BG_SHOULD_USE_BG_DICT
import org.cytoscape.biogwplugin.internal.util.Utility
import org.cytoscape.model.CyNetwork
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

class RelationTypeTreeNode(val relationType: BGRelationType): DefaultMutableTreeNode(relationType.name)


class BGDataModelController(private val serviceManager: BGServiceManager) {

    internal inner class PreferencesManager {

        var prefs = Preferences.userRoot().node("org.cytoscape.biogwplugin.PreferencesManager")

        fun setSelected(path: String, selected: Boolean) {
            prefs.putBoolean(path, selected)
        }

        fun getSelected(path: String): Boolean {
            return prefs.getBoolean(path, false)
        }
    }

    class BGCache {

        fun findNodeWithName(root: DefaultMutableTreeNode, name: String): MutableTreeNode? {
            val e = root.depthFirstEnumeration()
            while (e.hasMoreElements()) {
                val node = e.nextElement() as DefaultMutableTreeNode
                if (node.toString().equals(name)) {
                    return node
                }
            }
            return null
        }

        var importNodeConversions: Collection<BGConversion>? = null
        var exportNodeConversions: Collection<BGConversion>? = null
        var exportEdgeConversions: Collection<BGConversion>? = null


        var queryConstraints = HashMap<String, BGQueryConstraint>()
        var availableGraphs: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("Graphs"))

        // A cache of BGNodes, which are a local representation of the node found on the dataModelController.
        // Note that this cache is independent of the CyNodes and CyNetworks.
        var nodeCache = HashMap<String, BGNode>()

        var relationTypeMap = HashMap<String, BGRelationType>()
        var relationTypesForGraphs = HashMap<String, HashMap<String, BGRelationType>>()

        //var activeGraphs = arrayOf("intact", "tf-tg", "goa", "refprot").toHashSet()

        var activeRelationTypes = HashSet<BGRelationType>()

        val filteredRelationTypeMap: Map<String, BGRelationType> get() {
            return relationTypeMap.filter { activeRelationTypes.contains(it.value) }
        }

        fun addGraphToModel(graph: DefaultMutableTreeNode) {
            val root = availableGraphs.root as DefaultMutableTreeNode
           root.insert(graph, root.childCount)
        }

        fun getRelationTypesForURI(uri: String): Collection<BGRelationType>? {
            val types = relationTypeMap.values.filter { it.uri == uri }
            return if (types.size > 0) types else null
        }

        fun getRelationTypeForURIandGraph(uri: String, graph: String): BGRelationType? {
            if (graph.startsWith("?")) {
                val types = getRelationTypesForURI(uri)
                return types?.first()
            }
            val relationWithGraph = relationTypeMap.get(Utility.createRelationTypeIdentifier(uri, graph))
            if (relationWithGraph != null) return relationWithGraph
            val types = getRelationTypesForURI(uri)
            return types?.first()
        }



        val relationTypeDescriptions: LinkedHashMap<String, BGRelationType> get() {
            val relationTypes = LinkedHashMap<String, BGRelationType>()
            val relations = filteredRelationTypeMap.values.sortedBy { it.number }
            //val relations = relationTypeMap.values.sortedBy { it.number }
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

        fun addRelationType(relationType: BGRelationType) {
            relationTypeMap.put(relationType.identifier, relationType)
            relationType.defaultGraphName?.let {
                if (relationTypesForGraphs.containsKey(it)) {
                    relationTypesForGraphs[it]!![relationType.identifier] = relationType
                } else {
                    relationTypesForGraphs[it] = hashMapOf(relationType.identifier to relationType)
                }
            }
        }

        fun getRelationsForName(name: String): Collection<BGRelationType> {
            return relationTypeMap.filter { it.value.name == name }.map { it.value }.toList()
        }
    }

    val cache: BGCache
    private val preferencesManager = PreferencesManager()
    val settings = BGSettings()
    val parser = BGParser(serviceManager)
    val networkBuilder = BGNetworkBuilder(serviceManager)

    init {
        cache = BGCache()
        loadXMLFileFromServer()
        createGraphTreeRootnode()
    }

    fun createGraphTreeRootnode() {

        for (graph in cache.relationTypesForGraphs.keys) {
            val graphNode = DefaultMutableTreeNode(graph)
            cache.relationTypesForGraphs[graph]?.let {
                for (child in it.values) {
                    val childNode = RelationTypeTreeNode(child)
                    graphNode.insert(childNode, graphNode.childCount)
                }
            }
            cache.addGraphToModel(graphNode)
        }
    }

    fun setSelectionFromPreferences(tree: JCheckBoxTree) {
        val model = serviceManager.cache.availableGraphs
        val root = model.root as DefaultMutableTreeNode
        val enumeration = root.depthFirstEnumeration()
        for (node in enumeration) {
            if (!(node is RelationTypeTreeNode)) continue
            val type = node.relationType
            val active = preferencesManager.getSelected(type.identifier)
            if (active) {
                tree.checkNode(node, true)
            }
        }
        tree.repaint()
//        val rootPath = TreePath(root.path)
        //tree.fireCheckChangeEvent(JCheckBoxTree.CheckChangeEvent(rootPath))
    }

    private fun updateSelectedRelationPreferences() {
        for (relationType in cache.relationTypeMap.values) {
            val active = cache.activeRelationTypes.contains(relationType)
            preferencesManager.setSelected(relationType.identifier, active)
        }
        preferencesManager.prefs.flush()
    }

    fun setActiveRelationsForPaths(paths: Array<TreePath>) {
        val set = HashSet<BGRelationType>()
        for (path in paths) {
            val leaf = path.lastPathComponent as? RelationTypeTreeNode ?: continue
            set.add(leaf.relationType)
        }
        cache.activeRelationTypes = set
        updateSelectedRelationPreferences()
        //return set
    }

    fun setActivationForRelationType(graphName: String, relationTypeName: String, isActive: Boolean) {
        val relationTypes = cache.relationTypesForGraphs.get(graphName)?.map { it.value }?.filter { it.name.equals(relationTypeName) }
        if (relationTypes?.size == 1) {
            if (isActive) {
                activateRelationType(relationTypes.first())
            } else {
                deactivateRelationType(relationTypes.first())
            }
        }
    }

    fun activateRelationType(relationType: BGRelationType) {
        cache.activeRelationTypes.add(relationType)
    }

    fun deactivateRelationType(relationType: BGRelationType) {
        cache.activeRelationTypes.remove(relationType)
    }

    fun searchForExistingNode(uri: String): BGNode? {
        var node = cache.nodeCache[uri]

        if (node == null) {
            node = getNodeDataFromNetworks(uri)
        }
        return node
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
        }

        // The node should be added to cache even though not loaded, to avoid duplicating cache misses.
        cache.addNode(node)
        return node
    }

    fun loadDataForNode(node: BGNode) {
        if ((node.description == null) or (node.name == null)) {
            // Query the dataModelController for more info about this node.
            if (!node.uri.startsWith("http")) {
                return
            }

            val fetchedNode = getNodeFromServer(node.uri)
            if (fetchedNode != null) {
                node.name = fetchedNode.name
                node.description = fetchedNode.description
                node.isLoaded = true
                cache.addNode(node)
                for (cyNode in node.cyNodes) {
                    fetchedNode.name?.let {
                        cyNode.setName(it, cyNode.networkPointer)
                    }
                    fetchedNode.description?.let {
                        cyNode.setDescription(it, cyNode.networkPointer)
                    }
                }
            }
        } else {
            cache.addNode(node)
            node.isLoaded = true
        }
    }



    private fun getNodeDataFromNetworks(uri: String): BGNode? {
        val networks = serviceManager.networkManager?.networkSet ?: return null
        for (network in networks) {
            if (network.defaultNodeTable.getColumn(Constants.BG_FIELD_IDENTIFIER_URI) == null) {
                continue
            }
            val node = getNodeFromCyNetwork(uri, network)
            if (node != null) {
                cache.addNode(node)
                node.isLoaded = true
                return node
            }
        }
        return null
    }

    
    fun loadNodesFromServerSynchronously(nodes: Collection<BGNode>) {

        val unloadedNodes = nodes.filter { (it.description == null) or (it.name == null) }
        nodes.toHashSet().subtract(unloadedNodes).forEach {
            cache.addNode(it)
            it.isLoaded = true
        }
        val unloadedNodeUris = unloadedNodes.map { it.uri }
        val fetchedNodes = getNodesFromDictionaryServer(unloadedNodeUris)
        unloadedNodes.forEach { node ->
            val fetchedNode = fetchedNodes[node.uri]
            if (fetchedNode != null) {
                updateNodeData(node, fetchedNode)
            }
        }

    }

    private fun updateNodeData(node: BGNode, fetchedNode: BGNode) {
        node.name = fetchedNode.name
        node.description = fetchedNode.description
        node.isLoaded = true
        cache.addNode(node)
        for (cyNode in node.cyNodes) {
            fetchedNode.name?.let {
                cyNode.setName(it, cyNode.networkPointer)
            }
            fetchedNode.description?.let {
                cyNode.setDescription(it, cyNode.networkPointer)
            }
        }
    }

    private fun getNodesFromDictionaryServer(nodeUris: Collection<String>): HashMap<String, BGNode> {
        val query = BGMultiNodeFetchMongoQuery(serviceManager, nodeUris)
        query.run()
        val data = query.futureReturnData.get() as BGReturnNodeData

        return data.nodeData
    }

    private fun  getNodeFromServer(uri: String): BGNode? {
        if (!uri.startsWith("http://")) {
            return null
        }

        val nodeType = BGNode.static.nodeTypeForUri(uri)


        val query = if (BG_SHOULD_USE_BG_DICT && (nodeType == BGNodeType.Protein || nodeType == BGNodeType.Gene || nodeType == BGNodeType.GO)) {
            BGNodeFetchMongoQuery(serviceManager, uri)
        } else {
            BGNodeFetchQuery(serviceManager, uri)
        }

        query.run()
        val data = query.futureReturnData.get(10, TimeUnit.SECONDS ) as? BGReturnNodeData
        val node = data?.nodeData?.get(uri)
        return node

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

    fun getConfidenceScoreForRelation(relation: BGRelation): Double? {
        // Synchronous code! Will halt execution (Well, it's supposed to now...)
        val graph = relation.relationType.defaultGraphName
        if (graph.equals("intact")) {
            val query = BGFetchMetadataQuery(serviceManager, relation.fromNode.uri, relation.relationType.uri, relation.toNode.uri, graph!!, BGMetadataType.CONFIDENCE_VALUE)
            query.run() // <- This is synchronous.
            val result = query.returnFuture.get() as BGReturnMetadata // <- This is also synchronous. Should not halt at this point though.
            return result.values.first().toDouble() // <- Will throw an exception if the data isn't a double!
        }
        return null
    }

    fun loadXMLFileFromServer() {
        try {
            val queryFileUrl = URL(Constants.BG_CONFIG_FILE_URL)
            val connection = queryFileUrl.openConnection()
            val inputStream = connection.getInputStream()
            BGConfigParser.parseXMLConfigFile(inputStream, serviceManager, cache)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
