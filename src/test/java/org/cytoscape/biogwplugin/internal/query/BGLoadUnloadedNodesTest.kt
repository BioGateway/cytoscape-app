package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.biogwplugin.internal.model.BGNode
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class BGLoadUnloadedNodesTest {

    val serviceManager = BGServiceManager()
    val nodeUris = arrayListOf(
            "http://identifiers.org/uniprot/Q9UER7",
            "http://identifiers.org/uniprot/Q8WWK9",
            "http://purl.obolibrary.org/obo/GO_0001228",
            "http://identifiers.org/ncbigene/26586",
            "http://purl.obolibrary.org/obo/NCBITaxon_9606" )
    @Test
    fun run() {

        val nodes = nodeUris.map { BGNode(it) }
        val query = BGLoadUnloadedNodes.createAndRun(serviceManager, nodes) {

        }

    }
}