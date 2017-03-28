package org.cytoscape.biogwplugin.internal.query;

import org.cytoscape.work.AbstractTask;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

// TODO: This class should implement 'Runnable', I think.
public abstract class BGQuery extends AbstractTask implements Runnable {

    protected final static String RETURN_TYPE_TSV = "text/tab-separated-values";
    protected final static String BIOPAX_DEFAULT_OPTIONS = "timeout=0&debug=on";
	
	public String urlString;
	public String queryString;
	private ArrayList<Runnable> callbacks;
	
	public BGQuery(String urlString, String queryString) {
		super();
		this.urlString = urlString;
		this.queryString = queryString;
		callbacks = new ArrayList<Runnable>();
	}
	
	public void addCallback(Runnable callback) {
		this.callbacks.add(callback);
	}

    public ArrayList<Runnable> getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(ArrayList<Runnable> callbacks) {
        this.callbacks = callbacks;
    }

    public void runCallbacks() {
		for (Runnable callback : callbacks) {
			callback.run();
		}
	}

    protected static URL createBiopaxURL(String serverPath, String queryData, String returnType, String options) {
        URL queryURL;
        try {
            queryURL = new URL(serverPath+"?query="+ URLEncoder.encode(queryData, "UTF-8")+"&format="+URLEncoder.encode(returnType, "UTF-8")+"&"+options);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        return queryURL;
    }
}
