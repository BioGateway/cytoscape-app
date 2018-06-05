package eu.biogateway.cytoscape.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */


class BGNodeTypeNew(val id: String, val name: String, val uriPattern: String?, val typeClass: BGNodeTypeClass, val metadataGraph: String? = null, val autocompleteType: BGAutoCompleteType? = null) {

    enum class BGAutoCompleteType {
        PREFIX,
        INFIX;

        companion object {
            fun forId(id: String): BGAutoCompleteType? {
                return when (id.toLowerCase()) {
                    "prefix" -> PREFIX
                    "infix" -> INFIX
                    else -> null
                }}
        }
    }

    enum class BGNodeTypeClass(val id: String) {
        ENTITY("entity"),
        STATEMENT("statement"),
        PPI_STATEMENT("ppi_statement"),
        TFTG_STATEMENT("tftg_statement");

        companion object {
            fun forId(id: String): BGNodeTypeClass? {
                return when (id.toLowerCase()) {
                    "entity" -> ENTITY
                    "statement" -> STATEMENT
                    "ppi_statement" -> PPI_STATEMENT
                    "tftg_statement" -> TFTG_STATEMENT
                    else -> null
                }}
        }
    }

    override fun hashCode(): Int {
        return this.id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this.hashCode() == other?.hashCode()
    }

    override fun toString(): String {
        return this.name
    }

    companion object {
        val UNDEFINED = BGNodeTypeNew("undefined", "Undefined", null, BGNodeTypeClass.ENTITY)
    }
}

