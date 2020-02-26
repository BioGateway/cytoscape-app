package eu.biogateway.cytoscape.internal.model

import eu.biogateway.cytoscape.internal.BGServiceManager

class BGTaxon(val id: String, val name: String, val uri: String) {
    var enabledByDefault = false

    companion object {
       fun generateTaxonConstraint(): String? {
           val taxa = BGServiceManager.config.activeTaxa
           if (BGServiceManager.config.availableTaxa.count() == taxa.count()) return null
           if (taxa.count() == 0) return null
           val taxaString = taxa.map { "<${it.uri}>" }.reduce { acc, s -> acc+","+s }
           return taxaString
        }
    }
}