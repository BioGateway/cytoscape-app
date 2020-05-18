package eu.biogateway.app.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */


data class BGGraph(val uri: String, val label: String? = null) {
    val name: String get() {
        if (label != null && label.isNotBlank()) return label
        return uri
    }

    override fun toString(): String {
        return name
    }

    override fun hashCode(): Int {
        return uri.hashCode()
    }
}