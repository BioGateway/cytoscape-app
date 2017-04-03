package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.parser.BGParser;
import org.cytoscape.work.TaskMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by sholmas on 27/03/2017.
 */
public class BGRelationsQuery extends BGQuery {

    public enum Direction {
        PRE, POST
    }
    public Direction direction;
    public String nodeURI;
    public BGServiceManager serviceManager;

    static protected String POST_RELATIONS_SPARQL = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "PREFIX term_id: <http://identifiers.org/uniprot/Q8L4H0> \n" +
            "SELECT ?term_name ?out_rel ?head_id ?object_name  \n" +
            "WHERE {  GRAPH  <cco> {{  \n" +
            "\t\tterm_id:       skos:prefLabel      ?term_name .  \n" +
            "\t\tterm_id:       ?out_rel   ?head_id .  \n" +
            "\t\t?head_id       skos:prefLabel      ?object_name .  }}}";

    static protected String PRE_RELATIONS_SPARQL = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "PREFIX term_id: <http://identifiers.org/uniprot/Q8L4H0> \n" +
            "SELECT ?term_name ?in_rel ?tail_id ?subject_name  \n" +
            "WHERE {  GRAPH  <cco> {{  \n" +
            "\t\tterm_id:       skos:prefLabel      ?term_name .  \n" +
            "\t\t?tail_id       ?in_rel    term_id: .  \n" +
            "\t\t?tail_id       skos:prefLabel      ?subject_name . }}}";

    private ArrayList<BGRelation> returnData = new ArrayList<>();

    public BGRelationsQuery(String urlString, String nodeURI, Direction direction, BGServiceManager serviceManager) {
        super(urlString, "");
        if (direction == Direction.POST) {
            queryString = POST_RELATIONS_SPARQL.replaceAll("@identifierURI", nodeURI);
        } else {
            queryString = PRE_RELATIONS_SPARQL.replaceAll("@identifierURI", nodeURI);
        }
        this.direction = direction;
        this.nodeURI = nodeURI;
        this.serviceManager = serviceManager;
    }

    @Override
    public void run() {
        URL queryUrl = createBiopaxURL(this.urlString, this.queryString, RETURN_TYPE_TSV, BIOPAX_DEFAULT_OPTIONS);
        try {
            // Simpler way to get a String from an InputStream.
            InputStream stream = queryUrl.openStream();
            //returnData = BGParser.parseNodes(stream, serviceManager.getCache());
            returnData = BGParser.parseRelations(stream, this, serviceManager.getCache());

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.runCallbacks();
    }


    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.run();
    }

    public ArrayList<BGRelation> getReturnData() {
        return returnData;
    }
}
