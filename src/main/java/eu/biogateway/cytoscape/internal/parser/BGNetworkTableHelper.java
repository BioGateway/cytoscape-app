package eu.biogateway.cytoscape.internal.parser;

import com.sun.org.apache.xpath.internal.operations.Bool;
import eu.biogateway.cytoscape.internal.model.BGConversionType;
import eu.biogateway.cytoscape.internal.model.BGRelationMetadata;
import eu.biogateway.cytoscape.internal.model.BGRelationMetadataType;
import eu.biogateway.cytoscape.internal.query.BGMetadataTypeEnum;
import eu.biogateway.cytoscape.internal.util.Constants;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.jetbrains.annotations.Nullable;

public class BGNetworkTableHelper {

    static void checkForMissingColumns(CyTable edgeTable, CyTable nodeTable) {

        // Node table
        if (nodeTable != null) {
            if (nodeTable.getColumn(Constants.INSTANCE.getBG_FIELD_IDENTIFIER_URI()) == null)
                nodeTable.createColumn(Constants.INSTANCE.getBG_FIELD_IDENTIFIER_URI(), String.class, false);
            if (nodeTable.getColumn(Constants.INSTANCE.getBG_FIELD_NODE_TYPE()) == null)
                nodeTable.createColumn(Constants.INSTANCE.getBG_FIELD_NODE_TYPE(), String.class, false);
            if (nodeTable.getColumn(Constants.INSTANCE.getBG_FIELD_NODE_PARENT_EDGE_ID()) == null)
                nodeTable.createColumn(Constants.INSTANCE.getBG_FIELD_NODE_PARENT_EDGE_ID(), String.class, false);
        }

        if (edgeTable != null) {
            // Edge table
            if (edgeTable.getColumn(Constants.INSTANCE.getBG_FIELD_IDENTIFIER_URI()) == null)
                edgeTable.createColumn(Constants.INSTANCE.getBG_FIELD_IDENTIFIER_URI(), String.class, false);
            if (edgeTable.getColumn(Constants.INSTANCE.getBG_FIELD_SOURCE_GRAPH()) == null)
                edgeTable.createColumn(Constants.INSTANCE.getBG_FIELD_SOURCE_GRAPH(), String.class, false);
            //if (edgeTable.getColumn(Constants.INSTANCE.getBG_FIELD_CONFIDENCE()) == null)
            //    edgeTable.createColumn(Constants.INSTANCE.getBG_FIELD_CONFIDENCE(), Double.class, false);
            if (edgeTable.getColumn(Constants.INSTANCE.getBG_FIELD_EDGE_ID()) == null)
                edgeTable.createColumn(Constants.INSTANCE.getBG_FIELD_EDGE_ID(), String.class, false);
            if (edgeTable.getColumn(Constants.INSTANCE.getBG_FIELD_EDGE_EXPANDABLE()) == null)
                edgeTable.createColumn(Constants.INSTANCE.getBG_FIELD_EDGE_EXPANDABLE(), String.class, false);
        }
    }

    /// Returns false if the column already exists with another data dataType.
    static Boolean assureThatEdgeColumnExists(CyTable edgeTable, String identifier, BGRelationMetadata.DataType dataType, Boolean immutable) {

        Class classType = null;
        switch (dataType) {
            case NUMBER:
                classType = Double.class;
                break;
            case STRING:
                classType = String.class;
                break;
        }
        if (edgeTable.getColumn(identifier) != null) {
            Class tableType = edgeTable.getColumn(identifier).getType();
            if (tableType == classType) {
                return true;
            }
            return false;
        }
        edgeTable.createColumn(identifier, classType, immutable);
        return true;
    }

    /// Returns false if the column already exists with another data type.
    public static Boolean assureThatNodeColumnExists(CyTable nodeTable, String identifier, BGConversionType.DataType dataType, Boolean immutable) {

        Class classType = null;
        switch (dataType) {
            case DOUBLE:
                classType = Double.class;
                break;
            case STRING:
                classType = String.class;
                break;
        }
        if (nodeTable.getColumn(identifier) != null) {
            Class tableType = nodeTable.getColumn(identifier).getType();
            if (tableType == classType) {
                return true;
            }
            return false;
        }
        nodeTable.createColumn(identifier, classType, immutable);
        return true;
    }

    @Nullable
    public static String getStringForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, String.class);
    }

    @Nullable
    public static Double getDoubleForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, Double.class);
    }

    @Nullable
    public static String getStringForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, String.class);
    }

    @Nullable
    public static Double getDoubleForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, Double.class);
    }}
