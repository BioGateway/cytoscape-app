package eu.biogateway.cytoscape.internal.model

class BGRelationMetadataType(val id: String, val name: String, val dataType: BGTableDataType, val relationUri: String, val supportedRelations: Collection<BGRelationType>, val sparql: String? = null, val conversions: Map<String, String>? = null) {
    var scalingFactor: Double = 1.0
}
