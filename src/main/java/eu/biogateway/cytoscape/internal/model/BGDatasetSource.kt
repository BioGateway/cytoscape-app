package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager

class BGDatasetSource(val uri: String, val name: String, val relationType: BGRelationType) {

    val identifier: String get() {return relationType.identifier+":"+uri}

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun toString(): String {
        return relationType.toString()+": "+name
    }

    companion object {
        fun generateSourceConstraint(serviceManager: BGServiceManager, relationType: BGRelationType, fromUri: String, toUri: String, number: Int = 0): Pair<String, String>? {
            val relevantSources = serviceManager.cache.activeSources.filter { it.relationType.equals(relationType) }
            if (relevantSources.size == serviceManager.cache.datasetSources.get(relationType)?.size) return null // Don't filter if all sources are selected.
            if (relevantSources.count() == 0) return null
            val uri = "?sourceConstraint"+number

            val filter = "FILTER("+relevantSources.map { uri+"filter = <"+it.uri+">" }.reduce { acc, s -> acc+"||"+s }+")\n"
            val sparql = uri+" rdf:subject "+fromUri+".\n" +
                    uri+" rdf:object "+toUri+" .\n" +
                    uri+" rdf:predicate <"+relationType.uri+"> .\n" +
                    uri+" <http://semanticscience.org/resource/SIO_000253> "+uri+"filter ."
            return Pair(filter, sparql)
        }
    }
}