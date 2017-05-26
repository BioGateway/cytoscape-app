package org.cytoscape.biogwplugin.internal.old.query;

import org.cytoscape.biogwplugin.internal.BGServiceManager;
import org.cytoscape.biogwplugin.internal.old.parser.BGParser;
import org.cytoscape.work.TaskMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class BGNodeSearchQuery extends BGQuery {
	
	public ArrayList<BGNode> returnData = new ArrayList<BGNode>();
	public BGServiceManager serviceManager;
	
	public BGNodeSearchQuery(String urlString, String queryString, BGServiceManager serviceManager) {
		super(urlString, queryString, ResultType.NODE_DATA);
		this.serviceManager = serviceManager;
	}

	@Override
	public void run() {
		URL queryUrl = Companion.createBiopaxURL(this.getUrlString(), this.getQueryString(), Companion.getRETURN_TYPE_TSV(), Companion.getBIOPAX_DEFAULT_OPTIONS());
		try {
			// Simpler way to get a String from an InputStream.
			InputStream stream = queryUrl.openStream();
			returnData = BGParser.INSTANCE.parseNodes(stream, serviceManager.getCache());

		} catch (IOException e) {
			e.printStackTrace();
		}

		//this.runCallbacks();
	}

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
	    taskMonitor.setStatusMessage("Searching for nodes...");
        this.run();
    }
}