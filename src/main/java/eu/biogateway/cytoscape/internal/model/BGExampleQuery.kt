package eu.biogateway.cytoscape.internal.model

class BGExampleQuery(val name: String, val sparql: String, val placeholder: Boolean = false) {
    override fun toString(): String {
        return name
    }
}