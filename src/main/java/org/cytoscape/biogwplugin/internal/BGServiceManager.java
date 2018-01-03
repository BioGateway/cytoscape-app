package org.cytoscape.biogwplugin.internal;

import jdk.nashorn.internal.runtime.regexp.joni.Config;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.biogwplugin.internal.server.BGServer;
import org.cytoscape.biogwplugin.internal.util.BGVisualStyleBuilder;
import org.cytoscape.biogwplugin.internal.util.Constants;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.app.swing.*;
import org.cytoscape.task.create.CreateNetworkViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

import javax.swing.*;
import java.net.URL;

/**
 * Created by sholmas on 23/03/2017.
 */
public class BGServiceManager {

    // This seems like a good place to store static properties.
    private final BundleContext bundleContext;
    private final CyActivator activator;
    private final CyAppAdapter adapter;

    private CyApplicationManager applicationManager;
    private CyNetworkViewManager viewManager;
    private CyNetworkViewFactory viewFactory;
    private CreateNetworkViewTaskFactory createNetworkViewTaskFactory;
    private CyNetworkFactory networkFactory;
    private CyNetworkManager networkManager;
    private CyEventHelper eventHelper;
    private DialogTaskManager taskManager;

    // References to important objects used for GUI and task creation:
    private VisualMappingManager visualManager;
    private VisualMappingFunctionFactory visualFunctionFactoryDiscrete;
    private CyLayoutAlgorithmManager layoutAlgorithmManager;
    private CyTableFactory tableFactory;
    private CyTableManager tableManager;
    private CloseableHttpClient httpClient;

    private BGServer.BGCache cache;
    private BGServer server;
    public CloseableHttpClient httpclient = HttpClients.createDefault();

    private BGVisualStyleBuilder visualStyleBuilder = new BGVisualStyleBuilder(this);


    public BGServiceManager(CyActivator cyActivator, CyAppAdapter adapter, BundleContext bundleContext) {
        server = new BGServer(this);
        cache = server.getCache();
        this.bundleContext = bundleContext;
        this.activator = cyActivator;
        this.adapter = adapter;
    }

    public CyApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public void setApplicationManager(CyApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public CyNetworkViewManager getViewManager() {
        return viewManager;
    }

    public void setViewManager(CyNetworkViewManager viewManager) {
        this.viewManager = viewManager;
    }

    public CyNetworkViewFactory getViewFactory() {
        return viewFactory;
    }

    public void setViewFactory(CyNetworkViewFactory viewFactory) {
        this.viewFactory = viewFactory;
    }

    public CyNetworkFactory getNetworkFactory() {
        return networkFactory;
    }

    public void setNetworkFactory(CyNetworkFactory networkFactory) {
        this.networkFactory = networkFactory;
    }

    public CyNetworkManager getNetworkManager() {
        return networkManager;
    }

    public void setNetworkManager(CyNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public CyEventHelper getEventHelper() {
        return eventHelper;
    }

    public void setEventHelper(CyEventHelper eventHelper) {
        this.eventHelper = eventHelper;
    }

    public DialogTaskManager getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(DialogTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public VisualMappingManager getVisualMappingManager() {
        return visualManager;
    }

    public void setVisualMappingManager(VisualMappingManager visualManager) {
        this.visualManager = visualManager;
    }

    public VisualMappingFunctionFactory getVisualFunctionFactoryDiscrete() {
        return visualFunctionFactoryDiscrete;
    }

    public void setVisualFunctionFactoryDiscrete(VisualMappingFunctionFactory visualFunctionFactoryDiscrete) {
        this.visualFunctionFactoryDiscrete = visualFunctionFactoryDiscrete;
    }

    public CyLayoutAlgorithmManager getLayoutAlgorithmManager() {
        return layoutAlgorithmManager;
    }

    public void setLayoutAlgorithmManager(CyLayoutAlgorithmManager layoutAlgorithmManager) {
        this.layoutAlgorithmManager = layoutAlgorithmManager;
    }

    public CyTableFactory getTableFactory() {
        return tableFactory;
    }

    public void setTableFactory(CyTableFactory tableFactory) {
        this.tableFactory = tableFactory;
    }

    public CyTableManager getTableManager() {
        return tableManager;
    }

    public void setTableManager(CyTableManager tableManager) {
        this.tableManager = tableManager;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public BGServer.BGCache getCache() {
        return cache;
    }

    public void setCache(BGServer.BGCache cache) {
        this.cache = cache;
    }

    public CreateNetworkViewTaskFactory getCreateNetworkViewTaskFactory() {
        return createNetworkViewTaskFactory;
    }

    public void setCreateNetworkViewTaskFactory(CreateNetworkViewTaskFactory createNetworkViewTaskFactory) {
        this.createNetworkViewTaskFactory = createNetworkViewTaskFactory;
    }

    public String getServerPath() {
        return Constants.INSTANCE.getSERVER_PATH();
    }

    public BGServer getServer() {
        return server;
    }

    public void setServer(BGServer server) {
        this.server = server;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public CyActivator getActivator() {
        return activator;
    }

    public CloseableHttpClient getHttpclient() {
        return httpclient;
    }

    public CyAppAdapter getAdapter() {
        return adapter;
    }

    public BGVisualStyleBuilder getVisualStyleBuilder() {
        return visualStyleBuilder;
    }

    public VisualMappingManager getVisualManager() {
        return visualManager;
    }
}
