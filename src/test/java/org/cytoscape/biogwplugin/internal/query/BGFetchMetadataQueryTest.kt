package org.cytoscape.biogwplugin.internal.query

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BGFetchMetadataQueryTest {
    val serviceManager = BGServiceManager()

    @Test
    internal fun fetchGOAEvidenceCodeTest() {
        val fromUri = "http://identifiers.org/uniprot/H0W984";
        val toUri = "http://purl.obolibrary.org/obo/GO_0016758";
        val relationUri = "http://purl.obolibrary.org/obo/RO_0002327";

        val query = BGFetchMetadataQuery(serviceManager, fromUri, relationUri, toUri, "goa", BGMetadataType.EVIDENCE_CODE);
        Thread(query).start()
        val data = query.returnFuture.get()
        assertNotNull(data)
        val metadata = data as? BGReturnMetadata
        assertNotNull(metadata)
        assert(metadata!!.values.contains("ECO:0000256"))
    }

    @Test
    internal fun fetchTFTGPubmedIDTest() {
        val fromUri = "http://identifiers.org/uniprot/O15525";
        val toUri = "http://identifiers.org/ncbigene/3162";
        val relationUri = "http://purl.obolibrary.org/obo/RO_0002448";

        val query = BGFetchMetadataQuery(serviceManager, fromUri, relationUri, toUri, "tf-tg", BGMetadataType.PUBMED_ID)
        Thread(query).start()
        val data = query.returnFuture.get()
        assertNotNull(data)
        val metadata = data as? BGReturnMetadata
        assertNotNull(metadata)
        assert(metadata!!.values.contains("http://identifiers.org/pubmed/12453873"))
    }
}