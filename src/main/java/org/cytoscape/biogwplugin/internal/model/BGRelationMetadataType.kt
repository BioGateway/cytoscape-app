package org.cytoscape.biogwplugin.internal.model

class BGRelationMetadataType(val id: String, val name: String, val dataType: DataType, val relationUri: String, val supportedRelations: Collection<BGRelationType>) {
    enum class DataType {
        STRING, NUMBER
    }
}