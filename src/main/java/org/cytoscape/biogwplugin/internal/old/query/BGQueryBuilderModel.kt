package org.cytoscape.biogwplugin.internal.old.query

import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.*

import javax.xml.parsers.*

import org.cytoscape.biogwplugin.internal.BGServiceManager
import org.w3c.dom.*
import kotlin.collections.ArrayList


class BGQueryBuilderModel(private val serviceManager: BGServiceManager) {

    var queries = HashMap<String, QueryTemplate>()

    fun createQueryString(query: QueryTemplate): String {
        var queryString = query.sparqlString

        // Loop through all the checkboxes first, as they alter the code significantly and might contain other parameters that must be altered afterwards.
        for (parameter in query.parameters) {
            if (parameter.type == QueryParameter.ParameterType.CHECKBOX) {
                var value = when (parameter.value) {
                    "true" -> parameter.options["true"]
                    "false" -> parameter.options["false"]
                    else -> ""
                }
                val searchString = "@" + parameter.id

                if (value != null) {
                    queryString = queryString.replace(searchString.toRegex(), value)
                }
            }
        }
        for (parameter in query.parameters) {
            if (parameter.type != QueryParameter.ParameterType.CHECKBOX) {
                val value = parameter.value ?: throw NullPointerException("Parameter value can not be null!")

                val searchString = "@" + parameter.id
                queryString = queryString.replace(searchString.toRegex(), value)
            }
        }

        return queryString
    }

    fun runQuery(query: BGNodeSearchQuery, sanitize: Boolean) {

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


    fun parseXMLFile(stream: InputStream) {
        // TODO: Remove this, it's legacy code.
        queries = BGQueryBuilderModel.parseXMLFile(stream)
    }

    companion object {

        fun parseXMLFile(stream: InputStream): HashMap<String, QueryTemplate> {

            // TODO: Use "optionals" (and exceptions?) to let this crash more gracefully.

            val queryTemplateHashMap = HashMap<String, QueryTemplate>()

            val dbFactory = DocumentBuilderFactory.newInstance()
            try {
                val dBuilder = dbFactory.newDocumentBuilder()
                val doc = dBuilder.parse(stream)
                doc.documentElement.normalize()

                println("Root element :" + doc.documentElement.nodeName)

                val relationTypesNode = (doc.getElementsByTagName("relationTypes").item(0) as? Element) ?: throw Exception("relationTypes element not found in XML file!")
                val rList = relationTypesNode.getElementsByTagName("relationType") ?: throw Exception()


                var relationTypes = ArrayList<RelationType>()

                for (index in 0..rList.length -1) {
                    val element = rList.item(index) as? Element
                    val name = element?.getAttribute("name")
                    val uri = element?.textContent

                    if (name != null && uri != null) {
                        relationTypes.add(RelationType(uri, name))
                    }
                }


                // Will crash if the queryList tag isn't present.
                val queryList = (doc.getElementsByTagName("queryList").item(0) as? Element) ?: throw Exception("queryList element not found in XML file!")

                val nList = queryList.getElementsByTagName("query")

                println("----------------------------")

                for (temp in 0..nList.length - 1) {

                    val nNode = nList.item(temp)

                    if (nNode.nodeType == Node.ELEMENT_NODE) {

                        val qElement = nNode as Element

                        val queryName = qElement.getAttribute("name")
                        val queryDescription = qElement.getElementsByTagName("description").item(0).textContent
                        val sparqlString = qElement.getElementsByTagName("sparql").item(0).textContent.replace("\t", "") // Remove tabs from the XML file. (They might be added "for show").


                        println("\nQuery Name: " + queryName)
                        println("Description: " + queryDescription)
                        println("Query: " + sparqlString)

                        val query = QueryTemplate(queryName, queryDescription, sparqlString)

                        val parameterList = qElement.getElementsByTagName("parameter")

                        for (pIndex in 0..parameterList.length - 1) {

                            println(pIndex)

                            if (parameterList.item(pIndex).nodeType == Node.ELEMENT_NODE) {
                                val parameter = parameterList.item(pIndex) as Element
                                //							System.out.println("\nParameter: "+parameter.getElementsByTagName("name").item(0).getTextContent());
                                //							System.out.println("ID: "+parameter.getAttribute("id")+", type: "+parameter.getAttribute("type"));

                                val pId = parameter.getAttribute("id")
                                val pTypeString = parameter.getAttribute("type")
                                val pName = parameter.getElementsByTagName("name").item(0).textContent

                                val pType = when (pTypeString) {
                                    "text" -> QueryParameter.ParameterType.TEXT
                                    "checkbox" -> QueryParameter.ParameterType.CHECKBOX
                                    "combobox" -> QueryParameter.ParameterType.COMBOBOX
                                    "uniprot_id" -> QueryParameter.ParameterType.UNIPROT_ID
                                    "ontology" -> QueryParameter.ParameterType.ONTOLOGY
                                    "optionalUri" -> QueryParameter.ParameterType.OPTIONAL_URI
                                    else -> QueryParameter.ParameterType.TEXT
                                }

                                val qParameter = QueryParameter(pId, pName, pType)

                                val optionsList = parameter.getElementsByTagName("option")
                                run {
                                    for (oIndex in 0..optionsList.length - 1) {
                                        if (optionsList.item(oIndex).nodeType == Node.ELEMENT_NODE) {
                                            val oElement = optionsList.item(oIndex) as Element

                                            val oName = oElement.getAttribute("name")
                                            val oValue = oElement.textContent

                                            println("Option: $oName - $oValue")
                                            //QueryParameter.Option option = qParameter.Option(oName, oValue);
                                            qParameter.addOption(oName, oValue)
                                        }
                                    }
                                }
                                query.addParameter(qParameter)
                            }
                        }
                        queryTemplateHashMap.put(queryName, query)
                    }
                }
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            return queryTemplateHashMap
        }

        /* Deprecated
	public static void main(String[] args) {
		BGQueryBuilderModel model = new BGQueryBuilderModel();
		BGQueryBuilderUIWindowBuilder gui = new BGQueryBuilderUIWindowBuilder(model);
	}*/


        fun createBiopaxURL(serverPath: String, queryData: String, returnType: String, options: String): URL? {
            val queryURL: URL
            try {
                queryURL = URL(serverPath + "?query=" + URLEncoder.encode(queryData, "UTF-8") + "&format=" + URLEncoder.encode(returnType, "UTF-8") + "&" + options)
            } catch (e: MalformedURLException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
                return null
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return null
            }

            return queryURL
        }
    }

}
