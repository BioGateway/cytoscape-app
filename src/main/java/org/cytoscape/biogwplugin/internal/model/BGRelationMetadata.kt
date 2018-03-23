package org.cytoscape.biogwplugin.internal.model



class BGRelationMetadata(val dataType: DataType, value: Any, val type: BGRelationMetadataType? = null) {

    // TODO: Get rid of this. It's just here to make some parts of the code happy.
    @Deprecated("Get rid of this. It's just here to make some parts of the code happy.")
    companion object {
        val CONFIDENCE_VALUE = BGRelationMetadataType("confidenceValue", "Confidence Value", BGRelationMetadata.DataType.NUMBER, "rdfs:label", arrayListOf())
    }

    enum class DataType {
        STRING, NUMBER
    }

    constructor(type: BGRelationMetadataType, value: Any) : this(type.dataType, value, type)

    var numericValue: Double? = null
    var stringValue: String? = null

    init {
        when (dataType) {
            BGRelationMetadata.DataType.STRING -> {
                stringValue = value as String
            }
            BGRelationMetadata.DataType.NUMBER -> {
                numericValue = value as Double
            }
        }
    }
}