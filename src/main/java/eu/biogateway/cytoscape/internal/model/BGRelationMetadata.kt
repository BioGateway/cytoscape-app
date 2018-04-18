package eu.biogateway.cytoscape.internal.model


class BGRelationMetadata(val dataType: BGTableDataType, val value: Any, val type: BGRelationMetadataType? = null) {

    // TODO: Get rid of this. It's just here to make some parts of the code happy.
    @Deprecated("Get rid of this. It's just here to make some parts of the code happy.")
    companion object {
        val CONFIDENCE_VALUE = BGRelationMetadataType("confidenceValue", "Confidence Value", BGTableDataType.DOUBLE, "rdfs:label", arrayListOf())
    }

    constructor(type: BGRelationMetadataType, value: Any) : this(type.dataType, value, type)

    var numericValue: Double? = null
    var stringValue: String? = null

    init {
        when (dataType) {
            BGTableDataType.STRING -> {
                stringValue = value as String
            }
            BGTableDataType.DOUBLE -> {
                numericValue = value as Double
            }
            else -> {
            }
        }
    }
}