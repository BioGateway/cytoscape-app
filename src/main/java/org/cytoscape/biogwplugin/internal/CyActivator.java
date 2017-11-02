package org.cytoscape.biogwplugin.internal;

import java.util.Properties;

import org.cytoscape.biogwplugin.BiogwPlugin;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.biogwplugin.internal.gui.*;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.create.CreateNetworkViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) throws Exception {

	    BGServiceManager serviceManager = createServiceManager(context);
        // After this point, we should be assured that the XML config file is loaded. Otherwise, there is a network problem.


        BiogwPlugin biogwPlugin = new BiogwPluginImpl(serviceManager);


        Properties properties = new Properties();
		registerService(context, biogwPlugin, BiogwPlugin.class, properties);

        CreateQueryAction createQueryAction = new CreateQueryAction("Create query", "always", serviceManager);
        registerService(context, createQueryAction, CyAction.class, new Properties());
        registerContextMenuItems(context, serviceManager);
    }


	private void registerContextMenuItems(BundleContext bundleContext, BGServiceManager serviceManager) {

        //BGRelationPostSearchCMF postSearchCMF = new BGRelationPostSearchCMF(serviceManager);
        //BGRelationPreSearchCMF preSearchCMF = new BGRelationPreSearchCMF(serviceManager);

//        BGNodeMenuActionsCMF relationSearchFromCMF = new BGNodeMenuActionsCMF(0F, serviceManager);
//        registerAllServices(bundleContext, relationSearchFromCMF, ezProps("preferredMenu", "BioGateway"));

        BGNetworkViewCMF networkViewCMF = new BGNetworkViewCMF(0F, serviceManager);
        registerAllServices(bundleContext, networkViewCMF, ezProps("preferredMenu", "BioGateway"));

        BGNodeViewCMF nodeViewCMF = new BGNodeViewCMF(0F, serviceManager);
        registerAllServices(bundleContext, nodeViewCMF, ezProps("preferredMenu", "BioGateway"));

        BGChangeEdgeTypeCMF changeEdgeTypeCMF = new BGChangeEdgeTypeCMF(0F, serviceManager);
        registerAllServices(bundleContext, changeEdgeTypeCMF, ezProps("preferredMenu", "BioGateway"));

        BGOpenEdgeSourceViewCMF openPumedIdCMF = new BGOpenEdgeSourceViewCMF(1F, serviceManager);
        registerAllServices(bundleContext, openPumedIdCMF, ezProps("preferredMenu", "BioGateway"));

//        BGMultiNodeQueryCMF multiNodeFromQueryCMF = new BGMultiNodeQueryCMF(0F, "Fetch relations FROM selected", BGRelationDirection.FROM, serviceManager);
//        BGMultiNodeQueryCMF multiNodeToQueryCMF = new BGMultiNodeQueryCMF(1F, "Fetch relations TO selected", BGRelationDirection.TO, serviceManager);
//        registerAllServices(bundleContext, multiNodeFromQueryCMF, ezProps("preferredMenu", "BioGateway"));
//        registerAllServices(bundleContext, multiNodeToQueryCMF, ezProps("preferredMenu", "BioGateway"));

        /*
        BGNodeMenuActionsCMF postSearchCMF = new BGNodeMenuActionsCMF(serviceManager, BGRelationsQuery.Direction.POST, "Fetch relations from this node");
        BGNodeMenuActionsCMF preSearchCMF = new BGNodeMenuActionsCMF(serviceManager, BGRelationsQuery.Direction.PRE, "Fetch relations to this node");
        BGMultiRelationSearchCMF multiPostSearchCMF = new BGMultiRelationSearchCMF(serviceManager, BGRelationsQuery.Direction.POST, "Get relations from selected nodes");
        BGMultiRelationSearchCMF multiPreSearchCMF = new BGMultiRelationSearchCMF(serviceManager, BGRelationsQuery.Direction.PRE, "Get relations to selected nodes");

        registerAllServices(bundleContext, postSearchCMF, ezProps("preferredMenu", "Apps"));
        registerAllServices(bundleContext, preSearchCMF, ezProps("preferredMenu", "Apps"));
        registerAllServices(bundleContext, multiPostSearchCMF, ezProps("preferredMenu", "Apps"));
        registerAllServices(bundleContext, multiPreSearchCMF, ezProps("preferredMenu", "Apps"));*/
    }


	private BGServiceManager createServiceManager(BundleContext bundleContext) {
        CyNetworkManager networkManager = getService(bundleContext, CyNetworkManager.class);

        // This will also create a BGServer object, which creates a BGCache object and loads the XML file from the server.
        // The XML file is loaded SYNCHRONOUSLY, because we actually want to wait for it to load before loading the plugin.
        BGServiceManager bgServiceManager = new BGServiceManager(this, bundleContext);

        bgServiceManager.setApplicationManager(getService(bundleContext, CyApplicationManager.class));
        bgServiceManager.setViewManager(getService(bundleContext, CyNetworkViewManager.class));
        bgServiceManager.setNetworkManager(networkManager);
        bgServiceManager.setNetworkFactory(getService(bundleContext, CyNetworkFactory.class));
        bgServiceManager.setViewFactory(getService(bundleContext, CyNetworkViewFactory.class));
        bgServiceManager.setVisualManager(getService(bundleContext, VisualMappingManager.class));
        bgServiceManager.setVisualFunctionFactoryDiscrete(getService(bundleContext,VisualMappingFunctionFactory.class, "(mapping.type=discrete)"));
        bgServiceManager.setEventHelper(getService(bundleContext, CyEventHelper.class));
        bgServiceManager.setTaskManager(getService(bundleContext, DialogTaskManager.class));
        bgServiceManager.setLayoutAlgorithmManager(getService(bundleContext, CyLayoutAlgorithmManager.class));
        bgServiceManager.setTableFactory(getService(bundleContext, CyTableFactory.class));
        bgServiceManager.setTableManager(getService(bundleContext, CyTableManager.class));
        bgServiceManager.setCreateNetworkViewTaskFactory(getService(bundleContext, CreateNetworkViewTaskFactory.class));

        return bgServiceManager;
    }

    // Copied from "http://wiki.cytoscape.org/Cytoscape_3/AppDeveloper/Cytoscape_3_App_Cookbook":
    private static Properties ezProps(String... vals) {
        final Properties props = new Properties();
        for (int i = 0; i < vals.length; i += 2)
            props.put(vals[i], vals[i + 1]);
        return props;
    }
}
