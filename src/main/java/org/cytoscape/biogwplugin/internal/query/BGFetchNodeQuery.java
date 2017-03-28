package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.parser.BGParser;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.TaskMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;


public class BGFetchNodeQuery extends BGQuery {
	
	public ArrayList<BGNode> returnData = new ArrayList<>();
	public BGServiceManager serviceManager;

	public BGFetchNodeQuery(String urlString, String queryString, BGServiceManager serviceManager) {
        super(urlString, queryString);
	    this.serviceManager = serviceManager;
	}

	@Override
	public void run() {
		URL queryUrl = createBiopaxURL(this.urlString, this.queryString, RETURN_TYPE_TSV, BIOPAX_DEFAULT_OPTIONS);
		try {
			// Simpler way to get a String from an InputStream.

			InputStream stream = queryUrl.openStream();

            returnData = BGParser.parseNodes(stream, serviceManager.getCache());

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
