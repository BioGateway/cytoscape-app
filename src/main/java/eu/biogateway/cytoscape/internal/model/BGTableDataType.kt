package eu.biogateway.cytoscape.internal.model

enum class BGTableDataType(val javaCanonicalName: String) {
    STRING("java.lang.String"),
    DOUBLE("java.lang.Double"),
    INT("java.lang.Int"),
    BOOLEAN("java.lang.Boolean"),
    UNSUPPORTED(""),
    STRINGARRAY(""),
    INTARRAY(""),
    DOUBLEARRAY("");
    companion object {
        fun getTypeFromString(dataTypeString: String): BGTableDataType? {

            return when (dataTypeString) {
                "string" -> BGTableDataType.STRING
                "stringArray" -> BGTableDataType.STRINGARRAY
                "doubleArray" -> BGTableDataType.DOUBLEARRAY
                "intArray" -> BGTableDataType.INTARRAY
                "double" -> BGTableDataType.DOUBLE
                "boolean" -> BGTableDataType.BOOLEAN
                "integer" -> BGTableDataType.INT
                else -> null
            }
        }
    }
}

