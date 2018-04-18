package eu.biogateway.cytoscape.internal.query

import eu.biogateway.cytoscape.internal.BGServiceManager
import eu.biogateway.cytoscape.internal.model.BGNode
import org.junit.jupiter.api.Test

internal class BGLoadUnloadedNodesTest {

    val nodeUris = arrayListOf(
            "http://identifiers.org/uniprot/Q9UER7",
            "http://identifiers.org/uniprot/Q8WWK9",
            "http://purl.obolibrary.org/obo/GO_0001228",
            "http://identifiers.org/ncbigene/26586",
            "http://purl.obolibrary.org/obo/NCBITaxon_9606" )
    @Test
    fun run() {

        val nodes = nodeUris.map { BGNode(it) }
        val query = BGLoadUnloadedNodes.createAndRun(nodes) {

        }

    }
}