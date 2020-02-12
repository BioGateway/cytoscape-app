package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager

/**
 * Created by sholmas on 26/05/2017.
 */


class BGNodeType(val id: String, val name: String, val uriPattern: String?, val typeClass: BGNodeTypeClass, val metadataGraph: String? = null, val autocompleteType: BGAutoCompleteType? = null, val default: Boolean = false) {

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
        UNDIRECTED_STATEMENT("undirected_statement");

        companion object {
            fun forId(id: String): BGNodeTypeClass? {
                return when (id.toLowerCase()) {
                    "entity" -> ENTITY
                    "statement" -> STATEMENT
                    "undirected_statement" -> UNDIRECTED_STATEMENT
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
        val UNDEFINED = BGNodeType("undefined", "Undefined", null, BGNodeTypeClass.ENTITY)

        fun getNodeTypeForUri(uri: String): BGNodeType? {
            val matchingTypes = BGServiceManager.config.nodeTypes.values.filter { it.uriPattern != null}.filter {
                uri.contains(it.uriPattern!!)
            }
            return matchingTypes.firstOrNull()
        }
    }
}

