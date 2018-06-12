package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.libs.JCheckBoxTree
import eu.biogateway.cytoscape.internal.parser.*
import eu.biogateway.cytoscape.internal.query.*
import eu.biogateway.cytoscape.internal.server.BGSettings
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Constants.BG_SHOULD_USE_BG_DICT
import eu.biogateway.cytoscape.internal.util.Utility
import org.cytoscape.model.CyNetwork
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import javax.swing.JOptionPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

class BGRelationTypeTreeNode(val relationType: BGRelationType): DefaultMutableTreeNode(relationType.name)
class BGMetadataTypeTreeNode(val metadataType: BGRelationMetadataType): DefaultMutableTreeNode(metadataType.name)
class BGQueryConstraintTreeNode(val constraint: BGQueryConstraint): DefaultMutableTreeNode(constraint.label)
class BGSourceTreeNode(val source: BGDatasetSource): DefaultMutableTreeNode(source.name)


class BGDataModelController() {

    internal inner class PreferencesManager {

        var prefs = Preferences.userRoot().node("eu.biogateway.cytoscape.PreferencesManager")

        fun setSelected(path: String, identifier: String, selected: Boolean) {
            prefs.putBoolean((path+identifier).hashCode().toString(), selected)
        }

        fun getSelected(path: String, identifier: String): Boolean {
            return prefs.getBoolean((path+identifier).hashCode().toString(), false)
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

        var nodeTypes = HashMap<String, BGNodeTypeNew>()

        var importNodeConversionTypes: Collection<BGNodeConversionType>? = null
        var importEdgeConversionTypes: Collection<BGConversionType>? = null
        var exportNodeConversionTypes: Collection<BGConversionType>? = null
        var exportEdgeConversionTypes: Collection<BGConversionType>? = null


        var queryConstraints = HashMap<String, BGQueryConstraint>()
        var metadataTypes = HashMap<String, BGRelationMetadataType>()
        var datasetSources = HashMap<BGRelationType, HashSet<BGDatasetSource>>()

        var relationMetadataTypesNode = DefaultMutableTreeNode("Metadata Types")
        var queryConstraintsRootNode = DefaultMutableTreeNode("Query Constraints")
        var sourcesRootNode = DefaultMutableTreeNode("Sources")
        var relationTypesRootNode = DefaultMutableTreeNode("Datasets")
        var configPanelRootNode = DefaultMutableTreeNode("Root")
        var configPanelTreeModel: DefaultTreeModel = DefaultTreeModel(configPanelRootNode)


        // A cache of BGNodes, which are a local representation of the node found on the dataModelController.
        // Note that this cache is independent of the CyNodes and CyNetworks.
        var nodeCache = HashMap<String, BGNode>()

        var relationTypeMap = HashMap<String, BGRelationType>()
        var relationTypesForGraphs = HashMap<BGGraph, HashMap<String, BGRelationType>>()


        //var relationTypesRootNode = arrayOf("intact", "tf-tg", "goa", "refprot").toHashSet()

        var activeRelationTypes = HashSet<BGRelationType>()
        var activeMetadataTypes = HashSet<BGRelationMetadataType>()
        var activeConstraints = HashSet<BGQueryConstraint>()
        var activeSources = HashSet<BGDatasetSource>()

        val filteredRelationTypeMap: Map<String, BGRelationType> get() {
            return relationTypeMap.filter { activeRelationTypes.contains(it.value) }
        }

        init {
            configPanelRootNode.add(relationTypesRootNode)
            configPanelRootNode.add(relationMetadataTypesNode)
            configPanelRootNode.add(queryConstraintsRootNode)
            configPanelRootNode.add(sourcesRootNode)
        }

        @Deprecated("Too specific.")
        fun addGraphToModel(graph: DefaultMutableTreeNode) {
            val root = configPanelTreeModel.root as DefaultMutableTreeNode
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
        var visualStyleConfig: BGVisualStyleConfig = BGVisualStyleConfig()
        var datasetGraphs = HashMap<String, String>()

        fun addNode(node: BGNode) {
            // Only add a node if it's not already been added.
            // TODO: Merge the new node data into the previously cached node.
            if (nodeCache.contains(node.uri)) return
            nodeCache.set(node.uri, node)
        }

        fun addRelationType(relationType: BGRelationType) {
            relationTypeMap.put(relationType.identifier, relationType)
            relationType.defaultGraph?.let {
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
    val parser = BGParser()
    val networkBuilder = BGNetworkBuilder()

    init {
        cache = BGCache()
        loadXMLFileFromServer()
        createGraphTreeRootnode()
    }

    fun createGraphTreeRootnode() {
        for (graph in cache.relationTypesForGraphs.keys) {
            val graphNode = if (graph.name.isNotBlank()) DefaultMutableTreeNode(graph) else DefaultMutableTreeNode("Unspecified")
            cache.relationTypesForGraphs[graph]?.let {
                for (relationType in it.values) {
                    val childNode = BGRelationTypeTreeNode(relationType)
                    graphNode.add(childNode)
                }
            }
            cache.relationTypesRootNode.add(graphNode)
        }

        for (metadata in cache.metadataTypes.values) {
            val node = BGMetadataTypeTreeNode(metadata)
            cache.relationMetadataTypesNode.add(node)
        }
        for (constraint in cache.queryConstraints.values) {
            val node = BGQueryConstraintTreeNode(constraint)
            cache.queryConstraintsRootNode.add(node)
        }
        for (relationType in cache.datasetSources.keys) {
            val graph = relationType.defaultGraphURI ?: "Unspecified"
            val graphNode = getChildNode(graph, cache.sourcesRootNode)
            val relationTypeNode = getChildNode(relationType.name, graphNode)
            cache.datasetSources[relationType]?.let {
                for (source in it) {
                    val sourceNode = BGSourceTreeNode(source)
                    relationTypeNode.add(sourceNode)
                }
            }
        }
    }

    private fun getChildNode(name: String, parentNode: DefaultMutableTreeNode): DefaultMutableTreeNode {
        val nodes = parentNode.children().toList()
                .filter { it is DefaultMutableTreeNode }
                .map { it as DefaultMutableTreeNode }
                .filter { it.userObject.equals(name) }
        if (nodes.count() > 1) throw Exception("Duplicate graph nodes!")
        if (nodes.count() == 1) return nodes[0]
        val node = DefaultMutableTreeNode(name)
        parentNode.add(node)
        return node
    }

    private inline fun <reified T: DefaultMutableTreeNode> setSelectionFromPreferencesForType(tree: JCheckBoxTree, rootNode: DefaultMutableTreeNode, selection: (T) -> Boolean) {
        for (node in rootNode.depthFirstEnumeration()) {
            val typedNode = node as? T ?: continue
            if (selection(typedNode)) {
                tree.checkNode(typedNode, true)
            }}}

    fun setSelectionFromPreferences(tree: JCheckBoxTree) {
        setSelectionFromPreferencesForType<BGRelationTypeTreeNode>(tree, cache.relationTypesRootNode) {
            preferencesManager.getSelected("activeRelationTypes", it.relationType.identifier)
        }
        setSelectionFromPreferencesForType<BGMetadataTypeTreeNode>(tree, cache.relationMetadataTypesNode) {
            preferencesManager.getSelected("activeMetadataTypes", it.metadataType.id)
        }
        setSelectionFromPreferencesForType<BGQueryConstraintTreeNode>(tree, cache.queryConstraintsRootNode) {
            preferencesManager.getSelected("activeConstraints", it.constraint.id)
        }
        setSelectionFromPreferencesForType<BGSourceTreeNode>(tree, cache.sourcesRootNode) {
            preferencesManager.getSelected("activeSources", it.source.toString())
        }
        tree.repaint()
    }

    private fun updateSelectedConfigTreePreferences() {
        for (relationType in cache.relationTypeMap.values) {
            val active = cache.activeRelationTypes.contains(relationType)
            preferencesManager.setSelected("activeRelationTypes", relationType.identifier, active)
        }
        for (metadataType in cache.metadataTypes.values) {
            val active = cache.activeMetadataTypes.contains(metadataType)
            preferencesManager.setSelected("activeMetadataTypes", metadataType.id, active)
        }
        for (constraint in cache.queryConstraints.values) {
            val active = cache.activeConstraints.contains(constraint)
            preferencesManager.setSelected("activeConstraints", constraint.id, active)
        }
        val sources = cache.datasetSources.values.fold(HashSet<BGDatasetSource>()) {acc, hashSet -> acc.union(hashSet).toHashSet() }
        for (source in sources) {
            val active = cache.activeSources.contains(source)
            preferencesManager.setSelected("activeSources", source.toString(), active)
        }
        preferencesManager.prefs.flush()
    }

    fun setActiveNodesForPaths(paths: Array<TreePath>) {
        val relationSet = HashSet<BGRelationType>()
        val metadataSet = HashSet<BGRelationMetadataType>()
        val constraintSet = HashSet<BGQueryConstraint>()
        val sourceSet = HashSet<BGDatasetSource>()

        for (path in paths) {
            (path.lastPathComponent as? BGRelationTypeTreeNode)?.let {
                relationSet.add(it.relationType)
            }
            (path.lastPathComponent as? BGMetadataTypeTreeNode)?.let {
                metadataSet.add(it.metadataType)
            }
            (path.lastPathComponent as? BGQueryConstraintTreeNode)?.let {
                constraintSet.add(it.constraint)
            }
            (path.lastPathComponent as? BGSourceTreeNode)?.let {
                sourceSet.add(it.source)
            }
        }
        cache.activeRelationTypes = relationSet
        cache.activeMetadataTypes = metadataSet
        cache.activeConstraints = constraintSet
        cache.activeSources = sourceSet

        updateSelectedConfigTreePreferences()
        //return set
    }

    fun setActivationForRelationType(graph: BGGraph, relationTypeName: String, isActive: Boolean) {
        val relationTypes = cache.relationTypesForGraphs.get(graph)?.map { it.value }?.filter { it.name.equals(relationTypeName) }
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
        val networks = BGServiceManager.networkManager?.networkSet ?: return null
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
        if (nodeUris.isEmpty()) return HashMap()
        val query = BGMultiNodeFetchMongoQuery(nodeUris, "fetch")
        query.run()
        val data = query.futureReturnData.get() as BGReturnNodeData

        return data.nodeData
    }

    private fun  getNodeFromServer(uri: String): BGNode? {
        if (!uri.startsWith("http://")) {
            return null
        }

        val nodeType = BGNode.static.nodeTypeForUri(uri)


        val query = if (BG_SHOULD_USE_BG_DICT && (nodeType.autocompleteType != null)) {
            BGNodeFetchMongoQuery(uri)
        } else {
            BGNodeFetchQuery(uri)
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

            val node = BGNode(uri)
            node.name = nodeName
            node.description = description
            return node
            // This old code did not work with imported networks which didn't have the "description" field.
//            if (nodeName != null && description != null) {
//                val node = BGNode(uri, nodeName, description)
//                return node
//            }
        }
        return null
    }






    fun loadRelationMetadataForRelation(relation: BGRelation, metadataType: BGRelationMetadataType) {
        if (!metadataType.supportedRelations.contains(relation.relationType)) return
        val graph = relation.relationType.defaultGraphURI ?: return

        val query = BGFetchMetadataQuery(relation.fromNode.uri, relation.relationType.uri, relation.toNode.uri, graph, metadataType.relationUri)
        query.run()
        val result = query.returnFuture.get() as BGReturnMetadata // <- This is also synchronous. Should not halt at this point though.

    }


    fun getConfidenceScoreForRelation(relation: BGRelation): Double? {
        // Synchronous code! Will halt execution (Well, it's supposed to now...)
        val graph = relation.relationType.defaultGraphURI
        if (graph.equals("intact")) {
            val query = BGFetchMetadataQuery(relation.fromNode.uri, relation.relationType.uri, relation.toNode.uri, graph!!, BGMetadataTypeEnum.CONFIDENCE_VALUE.uri)
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
            val error = BGConfigParser.parseXMLConfigFile(inputStream, cache)
            error?.let {
                JOptionPane.showMessageDialog(null, "Unable to load the config file! \n $it")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
