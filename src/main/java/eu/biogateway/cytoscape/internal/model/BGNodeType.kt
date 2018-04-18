package eu.biogateway.cytoscape.internal.model

/**
 * Created by sholmas on 26/05/2017.
 */

enum class BGNodeType(val paremeterType: String) {
    Protein("Protein"),
    Gene("Gene"),
    GO("GO-Term"),
    Taxon("Taxon"),
    Disease("Disease"),
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
                "go-term" -> GO
                "taxon" -> Taxon
                "ppi" -> PPI
                "tf-tg" -> TFTG
                "pubmedId" -> Pubmed
                "disease" -> Disease
                "undefined" -> Undefined
                else -> null
            }
        }
    }
}