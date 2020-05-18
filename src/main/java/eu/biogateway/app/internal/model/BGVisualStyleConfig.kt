package eu.biogateway.app.internal.model

import org.cytoscape.view.presentation.property.LineTypeVisualProperty
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty
import org.cytoscape.view.presentation.property.values.LineType
import org.cytoscape.view.presentation.property.values.NodeShape
import java.awt.Color

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