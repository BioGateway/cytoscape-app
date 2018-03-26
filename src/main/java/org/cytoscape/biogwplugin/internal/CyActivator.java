package org.cytoscape.biogwplugin.internal;

import java.util.Properties;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.biogwplugin.BiogwPlugin;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.biogwplugin.internal.gui.*;
import org.cytoscape.biogwplugin.internal.gui.cmfs.*;
import org.cytoscape.biogwplugin.internal.model.BGDataModelController;
import org.cytoscape.biogwplugin.internal.util.Utility;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.*;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.task.create.CreateNetworkViewTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

import static org.cytoscape.work.ServiceProperties.PREFERRED_ACTION;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

@SuppressWarnings("Convert2Lambda")
public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) {

	    BGServiceManager serviceManager = createServiceManager(context);
        // After this point, we should be assured that the XML config file is loaded. Otherwise, there is a network problem.

        BiogwPlugin biogwPlugin = new BiogwPluginImpl(serviceManager);

        Properties properties = new Properties();
		registerService(context, biogwPlugin, BiogwPlugin.class, properties);

        BGCreateAction createQueryAction = new BGCreateAction("BioGateway Query", "always", serviceManager, new BGAction() {
            @Override
            public void action(BGServiceManager serviceManager) {
                BGQueryBuilderController queryBuilderController = new BGQueryBuilderController(serviceManager);
            }
        });
        registerService(context, createQueryAction, CyAction.class, new Properties());

        BGCreateAction reloadCurrentStyleAction = new BGCreateAction("Refresh visual style", "always", serviceManager, new BGAction() {
            @Override
            public void action(BGServiceManager serviceManager) {
                Utility.INSTANCE.reloadCurrentVisualStyleCurrentNetworkView(serviceManager);
            }
        });
        // This action is disabled in the current build.
        //registerService(context, reloadCurrentStyleAction, CyAction.class, new Properties());

        BGCreateAction openSettingsAction = new BGCreateAction("Settings", "always", serviceManager, new BGAction() {
            @Override
            public void action(BGServiceManager serviceManager) {
                new BGSettingsView(serviceManager);
            }
        });
        // This action is disabled in the current build.
        //registerService(context, openSettingsAction, CyAction.class, new Properties());

        BGCreateAction reloadDataModelAction = new BGCreateAction("BioGateway: Reload Config", "always", serviceManager, new BGAction() {
            @Override
            public void action(BGServiceManager serviceManager) {
                serviceManager.setDataModelController(new BGDataModelController(serviceManager));
                serviceManager.getControlPanel().setupTreePanel();
            }
        });
        registerService(context, reloadDataModelAction, CyAction.class, new Properties());

        BGCreateAction importStyleAction = new BGCreateAction("Import the BioGateway visual style", "always", serviceManager, new BGAction() {
            @Override
            public void action(BGServiceManager serviceManager) {
                VisualStyle style = serviceManager.getVisualStyleBuilder().generateStyle();
                serviceManager.getVisualManager().addVisualStyle(style);
            }
        });
        // This is disabled, as the style is imported automatically.
        //registerService(context, importStyleAction, CyAction.class, new Properties());

        BGControlPanel controlPanel = new BGControlPanel(serviceManager);
        serviceManager.setControlPanel(controlPanel);
        registerService(context, controlPanel, CytoPanelComponent.class, new Properties());

        registerContextMenuItems(context, serviceManager);
    }


	private void registerContextMenuItems(BundleContext bundleContext, BGServiceManager serviceManager) {

        BGNetworkViewCMF networkViewCMF = new BGNetworkViewCMF(0F, serviceManager);
        registerAllServices(bundleContext, networkViewCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        BGNodeViewCMF nodeViewCMF = new BGNodeViewCMF(0F, serviceManager);
        registerAllServices(bundleContext, nodeViewCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        BGChangeEdgeTypeCMF changeEdgeTypeCMF = new BGChangeEdgeTypeCMF(0F, serviceManager);
        registerAllServices(bundleContext, changeEdgeTypeCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        BGOpenEdgeSourceViewCMF openPumedIdCMF = new BGOpenEdgeSourceViewCMF(1F, serviceManager);
        registerAllServices(bundleContext, openPumedIdCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        //BGExpandEdgeCMF expandEdgeCMF = new BGExpandEdgeCMF(0F, serviceManager);
        //registerAllServices(bundleContext, expandEdgeCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        BGNodeDoubleClickNVTF doubleClickNVTF = new BGNodeDoubleClickNVTF(serviceManager);
        //registerAllServices(bundleContext, doubleClickNVTF, ezProps("PREFERRED_ACTION", "OPEN", "TITLE", "Expand/Collapse"));

        Properties doubleClickProperties = new Properties();
        doubleClickProperties.setProperty(PREFERRED_ACTION, "OPEN");
        doubleClickProperties.setProperty(TITLE, "Expand/Collapse Group");
        registerService(bundleContext, doubleClickNVTF, NodeViewTaskFactory.class, doubleClickProperties);

        BGExpandEdgeDoubleClickEVTF edgeDoubleClickEVTF = new BGExpandEdgeDoubleClickEVTF(serviceManager);
        registerAllServices(bundleContext, edgeDoubleClickEVTF, ezProps(PREFERRED_ACTION, "OPEN"));

    }




	private BGServiceManager createServiceManager(BundleContext bundleContext) {
        CyNetworkManager networkManager = getService(bundleContext, CyNetworkManager.class);

        // This will also create a BGDataModelController object, which creates a BGCache object and loads the XML file from the dataModelController.
        // The XML file is loaded SYNCHRONOUSLY, because we actually want to wait for it to load before loading the plugin.

        CySwingAppAdapter adapter = getService(bundleContext, CySwingAppAdapter.class);

        BGServiceManager bgServiceManager = new BGServiceManager(this, adapter, bundleContext);
        bgServiceManager.setApplicationManager(getService(bundleContext, CyApplicationManager.class));
        bgServiceManager.setViewManager(getService(bundleContext, CyNetworkViewManager.class));
        bgServiceManager.setNetworkManager(networkManager);
        bgServiceManager.setNetworkFactory(getService(bundleContext, CyNetworkFactory.class));
        bgServiceManager.setViewFactory(getService(bundleContext, CyNetworkViewFactory.class));
        bgServiceManager.setVisualMappingManager(getService(bundleContext, VisualMappingManager.class));
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
