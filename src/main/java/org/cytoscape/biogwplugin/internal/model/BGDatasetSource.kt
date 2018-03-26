package org.cytoscape.biogwplugin.internal.model

import org.cytoscape.biogwplugin.internal.BGServiceManager

class BGDatasetSource(val uri: String, val name: String, val relationTypes: Collection<BGRelationType>) {

    companion object {
        fun generateSourceConstraint(serviceManager: BGServiceManager, relationType: BGRelationType, fromUri: String, toUri: String, number: Int = 0): Pair<String, String>? {
            if (serviceManager.cache.activeSources.count() == serviceManager.cache.datasetSources.count()) return null // Don't filter if all sources are selected.
            val relevantSources = serviceManager.cache.activeSources.filter { it.relationTypes.contains(relationType) }
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