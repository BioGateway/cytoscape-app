package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.query.BGQueryTemplate
import eu.biogateway.cytoscape.internal.util.Utility
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class BGConfig {
    var nodeTypes = HashMap<String, BGNodeTypeNew>()

    var importNodeConversionTypes: Collection<BGNodeConversionType>? = null
    var importEdgeConversionTypes: Collection<BGConversionType>? = null
    var exportNodeConversionTypes: Collection<BGConversionType>? = null
    var exportEdgeConversionTypes: Collection<BGConversionType>? = null

    val exampleQueries = ArrayList<BGExampleQuery>()

    var queryConstraints = HashMap<String, BGQueryConstraint>()
    var metadataTypes = HashMap<String, BGRelationMetadataType>()
    var datasetSources = HashMap<BGRelationType, HashSet<BGDatasetSource>>()

    var relationMetadataTypesNode = DefaultMutableTreeNode("Metadata Types")
    var queryConstraintsRootNode = DefaultMutableTreeNode("Query Constraints")
    var sourcesRootNode = DefaultMutableTreeNode("Sources")
    var relationTypesRootNode = DefaultMutableTreeNode("Datasets")
    var configPanelRootNode = DefaultMutableTreeNode("Root")
    var configPanelTreeModel: DefaultTreeModel = DefaultTreeModel(configPanelRootNode)

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

    var queryTemplates = HashMap<String, BGQueryTemplate>()
    var visualStyleConfig: BGVisualStyleConfig = BGVisualStyleConfig()
    var datasetGraphs = HashMap<String, String>()

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

    var defaultFontSize = 12.0
        set(value) {
            if (value > 0) field = value
            BGServiceManager.dataModelController?.writeDefaultPreferences()
        }
}