package eu.biogateway.app.internal.model

abstract class BGNodeFilterType {
    abstract fun filter(node: BGNode, nodeTypes: Collection<BGNodeType>): Boolean


    enum class FilterPosition {
        PREFIX,
        SUFFIX
    }
}

class BGUriFilter(val filterString: String, val position: FilterPosition): BGNodeFilterType() {
    override fun filter(node: BGNode, nodeTypes: Collection<BGNodeType>): Boolean {
        if (!nodeTypes.contains(node.type)) {
            return true
        }
        return when (position) {
            FilterPosition.PREFIX -> {
                node.uri.startsWith(filterString)
            }
            FilterPosition.SUFFIX -> {
                node.uri.endsWith(filterString)
            }
        }
    }
}

class BGNameFilter(val filterString: String, val position: FilterPosition): BGNodeFilterType() {
    override fun filter(node: BGNode, nodeTypes: Collection<BGNodeType>): Boolean {
        if (!nodeTypes.contains(node.type)) {
            return true
        }
        return when (position) {
            FilterPosition.PREFIX -> {
                node.name.startsWith(filterString)
            }
            FilterPosition.SUFFIX -> {
                node.name.endsWith(filterString)
            }
        }
    }
}


class BGTaxonFilter(val taxon: String): BGNodeFilterType() {

    override fun filter(node: BGNode, nodeTypes: Collection<BGNodeType>): Boolean {
        if (!nodeTypes.contains(node.type)) {
            return true
        }
        return (taxon == node.taxon?.uri)
    }
}

class BGNodeFilter(val id: String, val label: String, val inputType: InputType, val filterType: BGNodeFilterType, val nodeTypes: Collection<BGNodeType>, val enabledByDefault: Boolean) {

    enum class InputType {
        COMBOBOX, TEXT, NUMBER, STATIC
    }

    fun filterNodes(nodes: Collection<BGNode>): Collection<BGNode> {
        return nodes.filter { filterType.filter(it, nodeTypes) }
    }


    companion object {
        fun filterNodes(nodes: Collection<BGNode>, filters: Collection<BGNodeFilter>): Collection<BGNode> {
            var nodeList = nodes.toMutableList()
            for (filter in filters) {
                nodeList = filter.filterNodes(nodeList).toMutableList()
            }
            return nodeList
        }
    }
}