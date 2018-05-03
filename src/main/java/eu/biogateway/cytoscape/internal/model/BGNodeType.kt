package eu.biogateway.cytoscape.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGNodeType(val paremeterType: String) {
    Protein("Protein"),
    Gene("Gene"),
    GOTerm("GO-Term"),
    Taxon("Taxon"),
    Disease("Disease"),
    TF("Transcription Factor"),
    PPI("PPI"),
    GOA("GO-Annotation"),
    TFTG("TF-TG Statement"),
    Pubmed("Pubmed URI"),
    Undefined("Undefined");

    override fun toString(): String {
        return this.paremeterType
    }

    companion object {
        fun forName(name: String): BGNodeType? {
            return when (name.toLowerCase()) {
                "protein" -> Protein
                "gene" -> Gene
                "go-term" -> GOTerm
                "taxon" -> Taxon
                "ppi" -> PPI
                "tf-tg" -> TFTG
                "tf" -> TF
                "pubmedId" -> Pubmed
                "disease" -> Disease
                "undefined" -> Undefined
                else -> null
            }
        }
        fun superClassUri(type: BGNodeType): String? {
            return when (type) {
                Protein -> "http://semanticscience.org/resource/SIO_010043"
                Gene -> "http://semanticscience.org/resource/SIO_010035"
                TF -> "http://identifiers.org/ncit/C17207"
                TFTG -> "http://purl.obolibrary.org/obo/GO_0006357"
                Disease -> "http://semanticscience.org/resource/SIO_000014"
                else -> null
            }
        }
    }
}