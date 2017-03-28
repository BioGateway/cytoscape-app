package org.cytoscape.biogwplugin.internal.parser;

import org.cytoscape.biogwplugin.internal.cache.BGCache;
import org.cytoscape.biogwplugin.internal.query.BGNode;
import org.cytoscape.biogwplugin.internal.query.BGRelation;
import org.cytoscape.biogwplugin.internal.query.BGRelationsQuery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class BGParser {
	
	public static ArrayList<BGNode> parseNodes(InputStream is, BGCache cache) {
		
		ArrayList<BGNode> nodes = new ArrayList<BGNode>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		try {
			String line = reader.readLine(); // Read the first line and throw it away for now. Might be of use later?
			while ((line = reader.readLine()) != null) {
				String[] lineComponents = line.split("\t");
				
				// If there's more than two components, something went wrong.
				// TODO: Replace this with an exception, an assert is overly drastic.
				assert lineComponents.length == 2;
				
				String uri = removeIllegalCharacters(lineComponents[0]);
				String description = removeIllegalCharacters(lineComponents[1]);
				
				BGNode node = new BGNode(uri);
				node.commonName = description;
				cache.addNode(node);
				nodes.add(node);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return nodes;
	}

	public static ArrayList<BGRelation> parseRelations(InputStream is, BGRelationsQuery query, BGCache cache) {
	    ArrayList<BGRelation> relations = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        try {
            String line = reader.readLine(); // Read the first line and throw it away for now. Might be of use later?
            while ((line = reader.readLine()) != null) {
                String[] lineComponents = line.split("\t");

                // If there's more than two components, something went wrong.
                // TODO: Replace this with an exception, an assert is overly drastic.
                assert lineComponents.length == 4;

                if (query.direction == BGRelationsQuery.BGRelationDirection.POST) {
                    // TODO: Assert that this node exists. It should, but can't be certain.
                    BGNode fromNode = cache.getNodeWithURI(query.nodeURI);
                    assert fromNode != null;

                    String relationType = removeIllegalCharacters(lineComponents[1]);
                    String toNodeUri = removeIllegalCharacters(lineComponents[2]);
                    String toNodeName = removeIllegalCharacters(lineComponents[3]);

                    BGNode toNode = cache.getNodeWithURI(toNodeUri);
                    if (toNode == null) {
                        toNode = new BGNode(toNodeUri);
                        toNode.commonName = toNodeName;
                        cache.addNode(toNode);
                    }

                    BGRelation relation = new BGRelation(fromNode, toNode, relationType);
                    relations.add(relation);

                } else {
                    BGNode toNode = cache.getNodeWithURI(query.nodeURI);
                    assert toNode != null;

                    // TODO: Code repetition. Refactor?
                    String relationType = removeIllegalCharacters(lineComponents[1]);
                    String fromNodeUri = removeIllegalCharacters(lineComponents[2]);
                    String fromNodeName = removeIllegalCharacters(lineComponents[3]);

                    BGNode fromNode = cache.getNodeWithURI(fromNodeUri);
                    if (fromNode == null) {
                        fromNode = new BGNode(fromNodeUri);
                        fromNode.commonName = fromNodeName;
                        cache.addNode(fromNode);
                    }

                    BGRelation relation = new BGRelation(fromNode, toNode, relationType);
                    relations.add(relation);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	    return relations;
    }
	
	static String removeIllegalCharacters(String input) {
		String returnString = input.replace("\"", "");
		// TODO: Replace other illegal characters as well.
		return returnString;
	}
}
