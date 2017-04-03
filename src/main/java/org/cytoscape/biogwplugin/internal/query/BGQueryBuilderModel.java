package org.cytoscape.biogwplugin.internal.query;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import javax.xml.parsers.*;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.w3c.dom.*;


public class BGQueryBuilderModel {

	private BGServiceManager serviceManager;
	
	public HashMap<String, QueryTemplate> queries = new HashMap<String, QueryTemplate>();


	public BGQueryBuilderModel(BGServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}

	public String createQueryString(QueryTemplate query) {
		String queryString = query.sparqlString;
		
		// Loop through all the checkboxes first, as they alter the code significantly and might contain other parameters that must be altered afterwards.
		for(QueryParameter parameter : query.parameters) {
			if (parameter.type == QueryParameter.ParameterType.CHECKBOX) {
				String value = "";
				switch (parameter.value) {
				case "true":
					value = parameter.options.get("true");
					break;
				case "false":
					value = parameter.options.get("false");
					break;
				default:
					break;
				}
				String searchString = "@"+parameter.id;
				queryString = queryString.replaceAll(searchString, value);
			}
		}
		for (QueryParameter parameter : query.parameters) {
			if (parameter.type != QueryParameter.ParameterType.CHECKBOX) {
				String value = parameter.value;
				String searchString = "@"+parameter.id;
				queryString = queryString.replaceAll(searchString, value);
			}
		}
		
		return queryString;
	}
	
	public static HashMap<String, QueryTemplate> parseXMLFile(InputStream stream) {

		HashMap<String, QueryTemplate> queryTemplateHashMap = new HashMap<>();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(stream);
			doc.getDocumentElement().normalize();
			
			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

			NodeList nList = doc.getElementsByTagName("query");

			System.out.println("----------------------------");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element qElement = (Element) nNode;
					
					String queryName = qElement.getAttribute("name");
					String queryDescription = qElement.getElementsByTagName("description").item(0).getTextContent();
					String sparqlString = qElement.getElementsByTagName("sparql").item(0).getTextContent().replace("\t", ""); // Remove tabs from the XML file. (They might be added "for show").
					
					
					System.out.println("\nQuery Name: "+queryName);
					System.out.println("Description: "+ queryDescription);
					System.out.println("Query: "+ sparqlString);

					QueryTemplate query = new QueryTemplate(queryName, queryDescription, sparqlString);
					
					NodeList parameterList = qElement.getElementsByTagName("parameter");
					
					for (int pIndex = 0; pIndex < parameterList.getLength(); pIndex++) {

						System.out.println(pIndex);
						
						if (parameterList.item(pIndex).getNodeType() == Node.ELEMENT_NODE) {
							Element parameter = (Element) parameterList.item(pIndex);
//							System.out.println("\nParameter: "+parameter.getElementsByTagName("name").item(0).getTextContent());
//							System.out.println("ID: "+parameter.getAttribute("id")+", type: "+parameter.getAttribute("type"));

							String pId = parameter.getAttribute("id");
							String pTypeString = parameter.getAttribute("type");
							String pName = parameter.getElementsByTagName("name").item(0).getTextContent();
							
							QueryParameter.ParameterType pType; 
							switch (pTypeString) {
							case "text":
								pType = QueryParameter.ParameterType.TEXT;
								break;
							case "checkbox":
								pType = QueryParameter.ParameterType.CHECKBOX;
								break;
							case "combobox":
								pType = QueryParameter.ParameterType.COMBOBOX;
								break;
							case "uniprot_id":
								pType = QueryParameter.ParameterType.UNIPROT_ID;
								break;
							case "ontology":
								pType = QueryParameter.ParameterType.ONTOLOGY;
								break;
							default:
								pType = QueryParameter.ParameterType.TEXT;
								break;
							}
							
							QueryParameter qParameter = new QueryParameter(pId, pName, pType);

							NodeList optionsList = parameter.getElementsByTagName("option"); {
								for (int oIndex = 0; oIndex < optionsList.getLength(); oIndex++) {
									if (optionsList.item(oIndex).getNodeType() == Node.ELEMENT_NODE) {
										Element oElement = (Element) optionsList.item(oIndex);
										
										String oName = oElement.getAttribute("name");
										String oValue = oElement.getTextContent();
										
										System.out.println("Option: "+oName+" - "+oValue);
										//QueryParameter.Option option = qParameter.Option(oName, oValue);
										qParameter.addOption(oName, oValue);
									}
								}
						}
							query.addParameter(qParameter);
					}
					}
					queryTemplateHashMap.put(queryName, query);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryTemplateHashMap;
	}
	
	public void runQuery(BGNodeSearchQuery query, boolean sanitize) {

        //		query.run();

		//String rawOutput = "";
		/*
		URL queryUrl = createBiopaxURL(query.urlString, query.queryString, RETURN_TYPE_TSV, BIOPAX_DEFAULT_OPTIONS);
		
		try {
			
			// Simpler way to get a String from an InputStream.
			
			InputStream stream = queryUrl.openStream();		
			
			ArrayList<BGNode> nodes = BGParser.parseNodes(stream, serviceManager.getCache());
			query.returnData = nodes;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		query.runCallbacks();
		//System.out.println(resultStrings);
		//return rawOutput;
		*/
	}
	
	/* Deprecated
	public static void main(String[] args) {
		BGQueryBuilderModel model = new BGQueryBuilderModel();
		BGQueryBuilderUIWindowBuilder gui = new BGQueryBuilderUIWindowBuilder(model);
	}*/
	
	
	public static URL createBiopaxURL(String serverPath, String queryData, String returnType, String options) {
		URL queryURL;
		try {
			queryURL = new URL(serverPath+"?query="+URLEncoder.encode(queryData, "UTF-8")+"&format="+URLEncoder.encode(returnType, "UTF-8")+"&"+options);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return queryURL;
	}
	
}
