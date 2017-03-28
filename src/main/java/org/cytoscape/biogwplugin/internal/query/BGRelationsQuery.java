package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.work.TaskMonitor;

import java.util.ArrayList;

/**
 * Created by sholmas on 27/03/2017.
 */
public class BGRelationsQuery extends BGQuery {

    public enum BGRelationDirection {
        PRE, POST
    }
    public BGRelationDirection direction;
    public String nodeURI;

    static private String POST_RELATIONS_SPARQL = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "PREFIX term_id: <http://identifiers.org/uniprot/Q8L4H0> \n" +
            "SELECT ?term_name ?out_rel ?head_id ?object_name  \n" +
            "WHERE {  GRAPH  <cco> {{  \n" +
            "\t\tterm_id:       skos:prefLabel      ?term_name .  \n" +
            "\t\tterm_id:       ?out_rel   ?head_id .  \n" +
            "\t\t?head_id       skos:prefLabel      ?object_name .  }}}";

    static private String PRE_RELATIONS_SPARQL = "BASE   <http://www.semantic-systems-biology.org/>  \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "PREFIX term_id: <http://identifiers.org/uniprot/Q8L4H0> \n" +
            "SELECT ?term_name ?in_rel ?tail_id ?subject_name  \n" +
            "WHERE {  GRAPH  <cco> {{  \n" +
            "\t\tterm_id:       skos:prefLabel      ?term_name .  \n" +
            "\t\t?tail_id       ?in_rel    term_id: .  \n" +
            "\t\t?tail_id       skos:prefLabel      ?subject_name . }}}";

    private ArrayList<BGRelation> returnData = new ArrayList<>();

    public BGRelationsQuery(String urlString, String nodeURI, BGRelationDirection direction) {
        super(urlString, "");
        if (direction == BGRelationDirection.POST) {
            queryString = POST_RELATIONS_SPARQL.replaceAll("@identifierURI", nodeURI);
        } else {
            queryString = POST_RELATIONS_SPARQL.replaceAll("@identifierURI", nodeURI);
        }
        this.direction = direction;
        this.nodeURI = nodeURI;
    }

    @Override
    public void run() {

    }


    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        this.run();
    }
}
