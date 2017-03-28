package org.cytoscape.biogwplugin.internal;

import java.util.Properties;

import org.cytoscape.biogwplugin.BiogwPlugin;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
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

        BiogwPlugin biogwPlugin = new BiogwPluginImpl(serviceManager);


        Properties properties = new Properties();
		registerService(context, biogwPlugin, BiogwPlugin.class, properties);
		System.out.println("HELLO, CYTOSCAPE! I'M ALIVE! WHOOO... yeah...");

        CreateQueryAction createQueryAction = new CreateQueryAction("Create query", "always", serviceManager);
        registerService(context, createQueryAction, CyAction.class, new Properties());

	}


	private BGServiceManager createServiceManager(BundleContext bundleContext) {
        CyNetworkManager networkManager = getService(bundleContext, CyNetworkManager.class);
        BGServiceManager bgServiceManager = new BGServiceManager();
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
