package eu.biogateway.cytoscape.internal.parser;

import eu.biogateway.cytoscape.internal.model.BGTableDataType;
import eu.biogateway.cytoscape.internal.util.Constants;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BGNetworkTableHelper {

    static void checkForMissingColumns(CyTable edgeTable, CyTable nodeTable) {

        // Node table
        if (nodeTable != null) {
            if (nodeTable.getColumn(Constants.INSTANCE.getBG_FIELD_IDENTIFIER_URI()) == null)
                nodeTable.createColumn(Constants.INSTANCE.getBG_FIELD_IDENTIFIER_URI(), String.class, false);
            if (nodeTable.getColumn(Constants.INSTANCE.getBG_FIELD_NODE_TYPE()) == null)
                nodeTable.createColumn(Constants.INSTANCE.getBG_FIELD_NODE_TYPE(), String.class, false);
            if (nodeTable.getColumn(Constants.INSTANCE.getBG_FIELD_NODE_TAXON()) == null)
                nodeTable.createColumn(Constants.INSTANCE.getBG_FIELD_NODE_TAXON(), String.class, false);
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

    /// Returns false if the column already exists with another data type.
    static Boolean assureThatColumnExists(CyTable table, String identifier, BGTableDataType dataType, Boolean immutable) {

        Class classType = null;
        switch (dataType) {
            case DOUBLE:
                classType = Double.class;
                break;
            case STRING:
                classType = String.class;
                break;
            case INT:
                classType = Integer.class;
                break;
            case STRINGARRAY:
            case INTARRAY:
            case DOUBLEARRAY:
                classType = List.class;
                break;
            case BOOLEAN:
                classType = Boolean.class;
                break;
        }
        if (table.getColumn(identifier) != null) {
            Class tableType = table.getColumn(identifier).getType();
            if (tableType == classType) {
                return true;
            }
            return false;
        }
        switch (dataType) {
            case STRINGARRAY:
                table.createListColumn(identifier, String.class, immutable);
                return true;
            case DOUBLEARRAY:
                table.createListColumn(identifier, Double.class, immutable);
                return true;
            case INTARRAY:
                table.createListColumn(identifier, Integer.class, immutable);
                return true;
        }

        table.createColumn(identifier, classType, immutable);
        return true;
    }

    @Nullable
    public static Object getValueForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network, Class classType) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, classType);
    }
    @Nullable
    public static Object getValueForNodeColumnName(CyNode node, String columnName, CyNetwork network, Class classType) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, classType);
    }

    @Nullable
    public static String getStringForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, String.class);
    }
    @Nullable
    public static String getStringForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, String.class);
    }
    @Nullable
    public static Double getDoubleForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, Double.class);
    }
    @Nullable
    public static Double getDoubleForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, Double.class);
    }
    @Nullable
    public static Integer getIntegerForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, Integer.class);
    }
    @Nullable
    public static Integer getIntegerForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, Integer.class);
    }
    @Nullable
    public static List getListForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, List.class);
    }
    @Nullable
    public static List getListForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, List.class);
    }
    @Nullable
    public static Boolean getBoolForEdgeColumnName(CyEdge edge, String columnName, CyNetwork network) {
        return network.getDefaultEdgeTable().getRow(edge.getSUID()).get(columnName, Boolean.class);
    }
    @Nullable
    public static Boolean getBoolForNodeColumnName(CyNode node, String columnName, CyNetwork network) {
        return network.getDefaultNodeTable().getRow(node.getSUID()).get(columnName, Boolean.class);
    }
}
