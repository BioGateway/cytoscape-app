package eu.biogateway.app.internal.model

import eu.biogateway.app.internal.BGServiceManager

class BGDatasetSource(val uri: String, val name: String, val relationType: BGRelationType) {

    val identifier: String get() {return relationType.identifier+":"+uri}
    var enabledByDefault = false

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun toString(): String {
        return relationType.toString()+": "+name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BGDatasetSource

        if (uri != other.uri) return false
        if (name != other.name) return false
        if (relationType != other.relationType) return false
        if (enabledByDefault != other.enabledByDefault) return false

        return true
    }

    companion object {
        fun generateSourceConstraint(relationType: BGRelationType, fromUri: String, toUri: String, number: Int = 0): Pair<String, String>? {
            val relevantSources = BGServiceManager.config.activeSources.filter { it.relationType.equals(relationType) }
            if (relevantSources.size == BGServiceManager.config.datasetSources.get(relationType)?.size) return null // Don't filter if all sources are selected.
            val sourceRelationUri = BGServiceManager.config.datasetSourceRelationTypeUri ?: return null
            if (relevantSources.count() == 0) return null
            val uri = "?sourceConstraint"+number

            val filter = "FILTER("+relevantSources.map { uri+"filter = <"+it.uri+">" }.reduce { acc, s -> acc+"||"+s }+")\n"
            val sparql = "$uri rdf:subject $fromUri .\n" +
                    "$uri rdf:object $toUri .\n" +
                    "?instance"+number+" rdf:type? $uri .\n" +
                    "?instance"+number+" <$sourceRelationUri> ${uri}filter ."
            return Pair(filter, sparql)
        }
    }
}