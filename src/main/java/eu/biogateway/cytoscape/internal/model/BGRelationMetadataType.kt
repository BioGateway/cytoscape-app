package eu.biogateway.cytoscape.internal.model

class BGRelationMetadataType(val id: String, val name: String, val dataType: BGRelationMetadata.DataType, val relationUri: String, val supportedRelations: Collection<BGRelationType>, val sparql: String? = null) {

}
