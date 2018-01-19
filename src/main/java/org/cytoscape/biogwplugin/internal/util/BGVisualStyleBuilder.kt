package org.cytoscape.biogwplugin.internal.util

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.VisualLexicon
import org.cytoscape.view.presentation.property.*
import org.cytoscape.view.presentation.property.values.ArrowShape
import org.cytoscape.view.presentation.property.values.LineType
import org.cytoscape.view.presentation.property.values.NodeShape
import org.cytoscape.view.vizmap.VisualPropertyDependency
import org.cytoscape.view.vizmap.VisualStyle
import org.cytoscape.view.vizmap.mappings.DiscreteMapping
import org.cytoscape.view.vizmap.mappings.PassthroughMapping
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import java.awt.Color
import java.awt.Font
import java.awt.Paint

class BGVisualStyleBuilder(val serviceManager: BGServiceManager) {

    fun generateStyle(): VisualStyle {

        val adapter = serviceManager.adapter

        // Colors
        val textBlue = Color(0, 153, 204)
        val nodeWhite = Color(255,255,255)
        val nodeLightYellow = Color(255, 252, 211)
        val nodeLightBlue = Color(230, 247, 253)
        val nodeLightGray = Color(245, 245, 245)
        val edgeGreen = Color(51, 204, 0)
        val edgeDarkBlue = Color(0, 51, 204)
        val edgeTurquoise = Color(0, 153, 153)

        // Fonts
        val labelFont = Font("SansSerif", Font.PLAIN, 12)

        val nodeShapes = hashMapOf<String, NodeShape>(
                "Protein" to NodeShapeVisualProperty.ROUND_RECTANGLE,
                "Gene" to NodeShapeVisualProperty.ELLIPSE,
                "GO annotation" to NodeShapeVisualProperty.DIAMOND,
                "Taxon" to NodeShapeVisualProperty.HEXAGON,
                "PPI" to NodeShapeVisualProperty.TRIANGLE
        )
        val nodeColors = hashMapOf<String, Color>(
                "Protein" to nodeLightBlue,
                "Gene" to nodeLightYellow,
                "PPI" to nodeLightGray
        )
        val edgeColors = hashMapOf<String, Color>(
                "enables" to edgeGreen,
                "has agent" to edgeTurquoise,
                "encodes" to edgeDarkBlue,
                "molecularly controls" to edgeTurquoise,
                "molecularly interacts with" to edgeTurquoise
        )
        val edgeLineTypes = hashMapOf<String, LineType>(
                "enables" to LineTypeVisualProperty.EQUAL_DASH,
                "encodes" to LineTypeVisualProperty.DOT,
                "involved in" to LineTypeVisualProperty.EQUAL_DASH,
                "molecularly interacts with" to LineTypeVisualProperty.EQUAL_DASH,
                "has agent" to LineTypeVisualProperty.DOT
        )
        val edgeLineWidths = hashMapOf<String, Double>(
                "molecularly interacts with" to 4.0
        )

        val vizMapManager = adapter.visualMappingManager
        val visualStyleFactory = adapter.visualStyleFactory

        val discreteMappingFactory = adapter.visualMappingFunctionDiscreteFactory
        val passthroughMappingFactory = adapter.visualMappingFunctionPassthroughFactory

        val vs = visualStyleFactory.createVisualStyle("BioGateway")

        // Default values
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, textBlue)
        vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, nodeWhite)
        vs.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 50.0)
        vs.setDefaultValue(BasicVisualLexicon.NODE_WIDTH, 50.0)
        vs.setDefaultValue(BasicVisualLexicon.NODE_HEIGHT, 50.0)
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_FACE, labelFont)
        vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 8)
        vs.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 2.0)
        vs.setDefaultValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW)


        // Node styles
        val nodeShapeMapping = discreteMappingFactory.createVisualMappingFunction("type", String::class.java, BasicVisualLexicon.NODE_SHAPE) as DiscreteMapping<String, NodeShape>
        val nodeColorMapping = discreteMappingFactory.createVisualMappingFunction("type", String::class.java, BasicVisualLexicon.NODE_FILL_COLOR) as DiscreteMapping<String, Paint>
        val nodeTooltipMapping = passthroughMappingFactory.createVisualMappingFunction("description", String::class.java, BasicVisualLexicon.NODE_TOOLTIP) as PassthroughMapping<String, String>
        val nodeLabelMapping = passthroughMappingFactory.createVisualMappingFunction("name", String::class.java, BasicVisualLexicon.NODE_LABEL) as PassthroughMapping<String, String>


        for ((type, shape) in nodeShapes) {
            nodeShapeMapping.putMapValue(type, shape)
        }
        for ((type, color) in nodeColors) {
            nodeColorMapping.putMapValue(type, color)
        }
        vs.addVisualMappingFunction(nodeShapeMapping)
        vs.addVisualMappingFunction(nodeColorMapping)
        vs.addVisualMappingFunction(nodeTooltipMapping)
        vs.addVisualMappingFunction(nodeLabelMapping)

        // Edge styles
        val edgeColorMapping = discreteMappingFactory.createVisualMappingFunction("name", String::class.java, BasicVisualLexicon.EDGE_UNSELECTED_PAINT) as DiscreteMapping<String, Paint>
        val edgeLineTypeMapping = discreteMappingFactory.createVisualMappingFunction("name", String::class.java, BasicVisualLexicon.EDGE_LINE_TYPE) as DiscreteMapping<String, LineType>
        val edgeTooltipMapping = passthroughMappingFactory.createVisualMappingFunction("name", String::class.java, BasicVisualLexicon.EDGE_TOOLTIP) as PassthroughMapping<String, String>
        val edgeWidthMapping = discreteMappingFactory.createVisualMappingFunction("name", String::class.java, BasicVisualLexicon.EDGE_WIDTH) as DiscreteMapping<String, Double>

        val edgeSourceArrowMapping = discreteMappingFactory.createVisualMappingFunction("name", String::class.java, BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE) as DiscreteMapping<String, ArrowShape>
        edgeSourceArrowMapping.putMapValue("molecularly interacts with", ArrowShapeVisualProperty.ARROW)

        for ((name, color) in edgeColors) {
            edgeColorMapping.putMapValue(name, color)
        }
        for ((name, type) in edgeLineTypes) {
            edgeLineTypeMapping.putMapValue(name, type)
        }
        for ((name, width) in edgeLineWidths) {
            edgeWidthMapping.putMapValue(name, width)
        }
        vs.addVisualMappingFunction(edgeColorMapping)
        vs.addVisualMappingFunction(edgeLineTypeMapping)
        vs.addVisualMappingFunction(edgeTooltipMapping)
        vs.addVisualMappingFunction(edgeSourceArrowMapping)

        for (dependency in vs.allVisualPropertyDependencies) {
            if (dependency.idString.equals("arrowColorMatchesEdge")) {
                dependency.setDependency(true)
            }
        }

        return vs
    }
}