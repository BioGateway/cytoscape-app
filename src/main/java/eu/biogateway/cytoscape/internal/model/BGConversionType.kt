package eu.biogateway.cytoscape.internal.model



open class BGConversionType(val direction: ConversionDirection, val id: String, val name: String, val dataType: BGTableDataType, val biogwId: String, val lookupMethod: LookupMethod, val template: String? = null, val sparqlTemplate: String? = null) {
    enum class ConversionDirection {
        IMPORT, EXPORT
    }
    enum class LookupMethod {
        REPLACE, COPY, EXTRACT, DICT_EXACT_LOOKUP
    }

    override fun toString(): String {
        return name
    }
}

class BGNodeConversionType(val nodeType: BGNodeType, direction: ConversionDirection, id: String, name: String, dataType: BGTableDataType, biogwId: String, lookupMethod: LookupMethod, template: String? = null, sparqlTemplate: String? = null): BGConversionType(direction, id, name, dataType, biogwId, lookupMethod, template, sparqlTemplate) {
    constructor(nodeType: BGNodeType, conversionType: BGConversionType) : this(nodeType, conversionType.direction, conversionType.id, conversionType.name, conversionType.dataType, conversionType.biogwId, conversionType.lookupMethod, conversionType.template, conversionType.sparqlTemplate)
}
class BGEdgeConversionType(direction: ConversionDirection, id: String, name: String, dataType: BGTableDataType, biogwId: String, lookupMethod: LookupMethod, template: String? = null, sparqlTemplate: String? = null): BGConversionType(direction, id, name, dataType, biogwId, lookupMethod, template, sparqlTemplate) {
    constructor(conversionType: BGConversionType) : this(conversionType.direction, conversionType.id, conversionType.name, conversionType.dataType, conversionType.biogwId, conversionType.lookupMethod, conversionType.template, conversionType.sparqlTemplate)
}