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
        fun generateSourceConstraint(relationType: BGRelationType, fromUri: String, toUri: String, number: Int = 0): Pair<String, String>? {
            val relevantSources = BGServiceManager.config.activeSources.filter { it.relationType.equals(relationType) }
            if (relevantSources.size == BGServiceManager.config.datasetSources.get(relationType)?.size) return null // Don't filter if all sources are selected.
            if (relevantSources.count() == 0) return null
            val uri = "?sourceConstraint"+number

            val filter = "FILTER("+relevantSources.map { uri+"filter = <"+it.uri+">" }.reduce { acc, s -> acc+"||"+s }+")\n"
            val sparql = "$uri rdf:subject $fromUri .\n" +
                    "$uri rdf:object $toUri .\n" +
                    "?instance"+number+" rdf:type? $uri .\n" +
                    "?instance"+number+" <http://schema.org/evidenceOrigin> ${uri}filter ."
//            val sparql1 = fromUri+"  <http://semanticscience.org/resource/SIO_000062> "+uri+" .\n" +
//                    uri+" <http://semanticscience.org/resource/SIO_000291> "+toUri+" .\n" +
//                    uri+"node rdf:type "+uri+" .\n" +
//                    uri+"node <http://semanticscience.org/resource/SIO_000253> "+uri+"filter ."
            return Pair(filter, sparql)
        }
    }
}