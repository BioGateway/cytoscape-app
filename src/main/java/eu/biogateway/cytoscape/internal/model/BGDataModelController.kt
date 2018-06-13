package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.libs.JCheckBoxTree
import eu.biogateway.cytoscape.internal.parser.*
import eu.biogateway.cytoscape.internal.query.*
import eu.biogateway.cytoscape.internal.server.BGSettings
import eu.biogateway.cytoscape.internal.util.Constants
import eu.biogateway.cytoscape.internal.util.Constants.BG_SHOULD_USE_BG_DICT
import org.cytoscape.model.CyNetwork
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.prefs.Preferences
import javax.swing.tree.DefaultMutableTreeNode
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



    val config: BGConfig
    private val preferencesManager = PreferencesManager()
    val settings = BGSettings()
    val parser = BGParser()
    val networkBuilder = BGNetworkBuilder()

    var nodeCache = HashMap<String, BGNode>()

    fun addNode(node: BGNode) {
        // Only add a node if it's not already been added.
        // TODO: Merge the new node data into the previously cached node.
        if (nodeCache.contains(node.uri)) return
        nodeCache.set(node.uri, node)
    }

    init {
        config = BGConfig()
        loadXMLFileFromServer()
        createGraphTreeRootnode()
    }

    fun createGraphTreeRootnode() {
        for (graph in config.relationTypesForGraphs.keys) {
            val graphNode = if (graph.name.isNotBlank()) DefaultMutableTreeNode(graph) else DefaultMutableTreeNode("Unspecified")
            config.relationTypesForGraphs[graph]?.let {
                for (relationType in it.values) {
                    val childNode = BGRelationTypeTreeNode(relationType)
                    graphNode.add(childNode)
                }
            }
            config.relationTypesRootNode.add(graphNode)
        }

        if (config.metadataTypes.size > 0) {
            config.configPanelRootNode.add(config.relationMetadataTypesNode)

            for (metadata in config.metadataTypes.values) {
                val node = BGMetadataTypeTreeNode(metadata)
                config.relationMetadataTypesNode.add(node)
            }
        }

        if (config.queryConstraints.size > 0) {
            config.configPanelRootNode.add(config.queryConstraintsRootNode)
            for (constraint in config.queryConstraints.values) {
                val node = BGQueryConstraintTreeNode(constraint)
                config.queryConstraintsRootNode.add(node)
            }
        }

        if (config.datasetSources.size > 0) {
            config.configPanelRootNode.add(config.sourcesRootNode)
            for (relationType in config.datasetSources.keys) {
                val graph = relationType.defaultGraphURI ?: "Unspecified"
                val graphNode = getChildNode(graph, config.sourcesRootNode)
                val relationTypeNode = getChildNode(relationType.name, graphNode)
                config.datasetSources[relationType]?.let {
                    for (source in it) {
                        val sourceNode = BGSourceTreeNode(source)
                        relationTypeNode.add(sourceNode)
                    }
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
        setSelectionFromPreferencesForType<BGRelationTypeTreeNode>(tree, config.relationTypesRootNode) {
            preferencesManager.getSelected("activeRelationTypes", it.relationType.identifier)
        }
        setSelectionFromPreferencesForType<BGMetadataTypeTreeNode>(tree, config.relationMetadataTypesNode) {
            preferencesManager.getSelected("activeMetadataTypes", it.metadataType.id)
        }
        setSelectionFromPreferencesForType<BGQueryConstraintTreeNode>(tree, config.queryConstraintsRootNode) {
            preferencesManager.getSelected("activeConstraints", it.constraint.id)
        }
        setSelectionFromPreferencesForType<BGSourceTreeNode>(tree, config.sourcesRootNode) {
            preferencesManager.getSelected("activeSources", it.source.toString())
        }
        tree.repaint()
    }

    private fun updateSelectedConfigTreePreferences() {
        for (relationType in config.relationTypeMap.values) {
            val active = config.activeRelationTypes.contains(relationType)
            preferencesManager.setSelected("activeRelationTypes", relationType.identifier, active)
        }
        for (metadataType in config.metadataTypes.values) {
            val active = config.activeMetadataTypes.contains(metadataType)
            preferencesManager.setSelected("activeMetadataTypes", metadataType.id, active)
        }
        for (constraint in config.queryConstraints.values) {
            val active = config.activeConstraints.contains(constraint)
            preferencesManager.setSelected("activeConstraints", constraint.id, active)
        }
        val sources = config.datasetSources.values.fold(HashSet<BGDatasetSource>()) { acc, hashSet -> acc.union(hashSet).toHashSet() }
        for (source in sources) {
            val active = config.activeSources.contains(source)
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
        config.activeRelationTypes = relationSet
        config.activeMetadataTypes = metadataSet
        config.activeConstraints = constraintSet
        config.activeSources = sourceSet

        updateSelectedConfigTreePreferences()
        //return set
    }

    fun setActivationForRelationType(graph: BGGraph, relationTypeName: String, isActive: Boolean) {
        val relationTypes = config.relationTypesForGraphs.get(graph)?.map { it.value }?.filter { it.name.equals(relationTypeName) }
        if (relationTypes?.size == 1) {
            if (isActive) {
                activateRelationType(relationTypes.first())
            } else {
                deactivateRelationType(relationTypes.first())
            }
        }
    }

    fun activateRelationType(relationType: BGRelationType) {
        config.activeRelationTypes.add(relationType)
    }

    fun deactivateRelationType(relationType: BGRelationType) {
        config.activeRelationTypes.remove(relationType)
    }

    fun searchForExistingNode(uri: String): BGNode? {
        var node = nodeCache[uri]

        if (node == null) {
            node = getNodeDataFromNetworks(uri)
        }
        return node
    }

    fun getNodeFromCacheOrNetworks(newNode: BGNode): BGNode {
        // Check if the node already exists in the config.
        var node = nodeCache[newNode.uri]

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

        // The node should be added to config even though not loaded, to avoid duplicating config misses.
        addNode(node)
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
                addNode(node)
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
            addNode(node)
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
                addNode(node)
                node.isLoaded = true
                return node
            }
        }
        return null
    }

    
    fun loadNodesFromServerSynchronously(nodes: Collection<BGNode>) {

        val unloadedNodes = nodes.filter { (it.description == null) or (it.name == null) }
        nodes.toHashSet().subtract(unloadedNodes).forEach {
            addNode(it)
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
        addNode(node)
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
            BGConfigParser.parseXMLConfigFile(inputStream, config)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
