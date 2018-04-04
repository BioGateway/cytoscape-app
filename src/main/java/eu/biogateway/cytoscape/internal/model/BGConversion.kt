package eu.biogateway.cytoscape.internal.model



open class BGConversion(val type: ConversionType, val id: String, val name: String, val dataType: DataType, val biogwId: String, val lookupMethod: LookupMethod, val template: String? = null, val sparqlTemplate: String? = null) {
    enum class ConversionType {
        IMPORT, EXPORT
    }
    enum class DataType {
        STRING, NUMBER, STRINGARRAY
    }
    enum class LookupMethod {
        REPLACE, COPY, EXTRACT, DICT_EXACT_LOOKUP
    }
}

class BGNodeConversion(val nodeType: BGNodeType, type: ConversionType, id: String, name: String, dataType: DataType, biogwId: String, lookupMethod: LookupMethod, template: String? = null, sparqlTemplate: String? = null): BGConversion(type, id, name, dataType, biogwId, lookupMethod, template, sparqlTemplate) {
    constructor(nodeType: BGNodeType, conversion: BGConversion) : this(nodeType, conversion.type, conversion.id, conversion.name, conversion.dataType, conversion.biogwId, conversion.lookupMethod, conversion.template, conversion.sparqlTemplate)
}
class BGEdgeConversion(type: ConversionType, id: String, name: String, dataType: DataType, biogwId: String, lookupMethod: LookupMethod, template: String? = null, sparqlTemplate: String? = null): BGConversion(type, id, name, dataType, biogwId, lookupMethod, template, sparqlTemplate) {
    constructor(conversion: BGConversion) : this(conversion.type, conversion.id, conversion.name, conversion.dataType, conversion.biogwId, conversion.lookupMethod, conversion.template, conversion.sparqlTemplate)
}