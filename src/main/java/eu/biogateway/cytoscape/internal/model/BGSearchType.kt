package eu.biogateway.cytoscape.internal.model

class BGSearchType(val id: String, val title: String, val nodeType: BGNodeTypeNew, val returnType: String = "json", val restPath: String, val arraySearch: Boolean = false, val httpMethod: HTTPOperation, var parameters: String? = null) {
    enum class HTTPOperation {
        GET,
        POST
    }

}


/*

<searchTypes>
    <multiple id="geneSymbol" title="Gene Symbol" nodeType="gene" returnType="json" restPath="findNodesWithSynonyms" httpMethod="POST"/>
    <single id="geneSymbol" title="Gene Symbol" nodeType="gene" returnType="json" restPath="findNodesWithFieldValue" httpMethod="GET" parameters="field=prefLabel" />
  </searchTypes>

class BGVisualStyleConfig() {
    val edgeColors = HashMap<String, Color>()
    val nodeColors = HashMap<String, Color>()
    val nodeShapes = HashMap<String, NodeShape>()
    val nodeWidths = HashMap<String, Double>()
    val nodeHeights = HashMap<String, Double>()
    val edgeLineTypes = HashMap<String, LineType>()
    val edgeLineWidths = HashMap<String, Double>()

    val lineTypeMapping = hashMapOf<String, LineType>(
            "dash_dot" to LineTypeVisualProperty.DASH_DOT,
            "long_dash" to LineTypeVisualProperty.LONG_DASH,
            "solid" to LineTypeVisualProperty.SOLID,
            "equal_dash" to LineTypeVisualProperty.EQUAL_DASH,
            "dot" to LineTypeVisualProperty.DOT
    )

    val nodeShapeMapping = hashMapOf<String, NodeShape>(
            "round_rectangle" to NodeShapeVisualProperty.ROUND_RECTANGLE,
            "ellipse" to NodeShapeVisualProperty.ELLIPSE,
            "diamond" to NodeShapeVisualProperty.DIAMOND,
            "hexagon" to NodeShapeVisualProperty.HEXAGON,
            "triangle" to NodeShapeVisualProperty.TRIANGLE
    )
}
 */