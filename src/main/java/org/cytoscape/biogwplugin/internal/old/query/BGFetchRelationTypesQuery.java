package org.cytoscape.biogwplugin.internal.old.query;

import org.cytoscape.biogwplugin.internal.old.parser.BGParser;
import org.cytoscape.work.TaskMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
        super(urlString, queryString, ResultType.RELATION_DATA);
    }

    @Override
    public void run() {
        URL queryUrl = Companion.createBiopaxURL(this.getUrlString(), this.queryString, Companion.getRETURN_TYPE_TSV(), Companion.getBIOPAX_DEFAULT_OPTIONS());
        try {
            // Simpler way to get a String from an InputStream.
            InputStream stream = queryUrl.openStream();
            returnData = BGParser.INSTANCE.parseRelationTypes(stream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //this.runCallbacks();
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.run();
    }
}
