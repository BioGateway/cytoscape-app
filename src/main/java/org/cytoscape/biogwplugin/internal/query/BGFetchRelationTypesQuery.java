package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.biogwplugin.internal.parser.BGParser;
import org.cytoscape.work.TaskMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sholmas on 03/04/2017.
 */
public class BGFetchRelationTypesQuery extends BGQuery {


    public HashMap<String, String> returnData = new HashMap<>();

    private static final String queryString = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX graph: <cco>  \n" +
            "SELECT distinct ?propertyURI ?propertyName  \n" +
            "WHERE {  \n" +
            " GRAPH graph: {  \n" +
            "  ?s ?propertyURI ?o .  \n" +
            "  ?propertyURI skos:prefLabel ?propertyName .  \n" +
            " }  \n" +
            "}  \n" +
            "ORDER BY ?propertyURI \n";

    public BGFetchRelationTypesQuery(String urlString) {
        super(urlString, queryString);
    }

    @Override
    public void run() {
        URL queryUrl = createBiopaxURL(this.urlString, this.queryString, RETURN_TYPE_TSV, BIOPAX_DEFAULT_OPTIONS);
        try {
            // Simpler way to get a String from an InputStream.
            InputStream stream = queryUrl.openStream();
            returnData = BGParser.parseRelationTypes(stream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.runCallbacks();
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.run();
    }
}
