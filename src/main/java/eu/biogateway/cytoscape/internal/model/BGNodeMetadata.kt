package eu.biogateway.cytoscape.internal.model

class BGNodeMetadata(val dataType: BGTableDataType, val dataValue: Any, val columnName: String) {

    fun getValue(): Any? {
        return when (dataType) {
            BGTableDataType.STRING -> dataValue as? String
            BGTableDataType.DOUBLE -> dataValue as? Double
            BGTableDataType.INT -> dataValue as? Int
            BGTableDataType.BOOLEAN -> dataValue as? Boolean
            BGTableDataType.UNSUPPORTED -> null
            BGTableDataType.STRINGARRAY -> {
                // Converting an array to a string could be done here.
                dataValue as? List<*>
            }
            BGTableDataType.INTARRAY -> TODO()
            BGTableDataType.DOUBLEARRAY -> TODO()
        }
    }
}