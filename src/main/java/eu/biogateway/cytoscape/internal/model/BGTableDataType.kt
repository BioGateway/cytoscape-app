package eu.biogateway.cytoscape.internal.model

enum class BGTableDataType(val javaCanonicalName: String) {
    STRING("java.lang.String"),
    DOUBLE("java.lang.Double"),
    INT("java.lang.Int"),
    BOOLEAN("java.lang.Bool"),
    UNSUPPORTED(""),
    STRINGARRAY("java.lang.Array")
}