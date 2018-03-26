package org.cytoscape.biogwplugin.internal.parser;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadata;
import org.cytoscape.biogwplugin.internal.model.BGRelationMetadataType;
import org.cytoscape.biogwplugin.internal.query.BGMetadataTypeEnum;
import org.cytoscape.biogwplugin.internal.util.Constants;
import org.cytoscape.model.CyTable;

public class BGNetworkTableCreator {

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

    /// Returns false if the column already exists with another data type.
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
}
