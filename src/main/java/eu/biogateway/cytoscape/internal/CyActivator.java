package eu.biogateway.cytoscape.internal;

import java.util.Properties;

import eu.biogateway.cytoscape.internal.gui.conversion.BGImportExportController;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CytoPanelComponent;
import eu.biogateway.cytoscape.BiogwPlugin;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import eu.biogateway.cytoscape.internal.gui.*;
import eu.biogateway.cytoscape.internal.gui.cmfs.*;
import eu.biogateway.cytoscape.internal.model.BGDataModelController;
import eu.biogateway.cytoscape.internal.util.Utility;
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
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.BundleContext;

import static org.cytoscape.work.ServiceProperties.PREFERRED_ACTION;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

@SuppressWarnings("Convert2Lambda")
public class CyActivator extends AbstractCyActivator {

	@Override
	public void start(BundleContext context) {

	    setupServiceManager(context);
        // After this point, we should be assured that the XML config file is loaded. Otherwise, there is a network problem.

        BiogwPlugin biogwPlugin = new BiogwPluginImpl();

        Properties properties = new Properties();
		registerService(context, biogwPlugin, BiogwPlugin.class, properties);

        BGCreateAction createQueryAction = new BGCreateAction("Query Builder", "always", new BGAction() {
            @Override
            public void action() {
                BGQueryBuilderController queryBuilderController = new BGQueryBuilderController();
            }
        });
        registerService(context, createQueryAction, CyAction.class, new Properties());

        BGCreateAction reloadCurrentStyleAction = new BGCreateAction("Refresh visual style", "always", new BGAction() {
            @Override
            public void action() {
                Utility.INSTANCE.reloadCurrentVisualStyleCurrentNetworkView();
            }
        });
        // This action is disabled in the current build.
        //registerService(context, reloadCurrentStyleAction, CyAction.class, new Properties());

        BGCreateAction openImportExport = new BGCreateAction("Import/Export", "always", new BGAction() {
            @Override
            public void action() {
                new BGImportExportController();
            }
        });
        registerService(context, openImportExport, CyAction.class, new Properties());

        BGCreateAction reloadDataModelAction = new BGCreateAction("DEBUG: Reload Config", "always", new BGAction() {
            @Override
            public void action() {
                BGServiceManager.INSTANCE.setDataModelController(new BGDataModelController());
                BGServiceManager.INSTANCE.getControlPanel().setupTreePanel();
            }
        });
        registerService(context, reloadDataModelAction, CyAction.class, new Properties());

        BGControlPanel controlPanel = new BGControlPanel();
        BGServiceManager.INSTANCE.setControlPanel(controlPanel);
        registerService(context, controlPanel, CytoPanelComponent.class, new Properties());

        registerContextMenuItems(context);
    }


	private void registerContextMenuItems(BundleContext bundleContext) {

        BGNetworkViewCMF networkViewCMF = new BGNetworkViewCMF(0F);
        registerAllServices(bundleContext, networkViewCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        BGNodeViewCMF nodeViewCMF = new BGNodeViewCMF(0F);
        registerAllServices(bundleContext, nodeViewCMF, ezProps(PREFERRED_MENU, "BioGateway"));

//        BGChangeEdgeTypeCMF changeEdgeTypeCMF = new BGChangeEdgeTypeCMF(0F);
//        registerAllServices(bundleContext, changeEdgeTypeCMF, ezProps(PREFERRED_MENU, "BioGateway"));

//        BGOpenEdgeSourceViewCMF openPumedIdCMF = new BGOpenEdgeSourceViewCMF(1F);
//        registerAllServices(bundleContext, openPumedIdCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        //BGExpandEdgeCMF expandEdgeCMF = new BGExpandEdgeCMF(0F, serviceManager);
        //registerAllServices(bundleContext, expandEdgeCMF, ezProps(PREFERRED_MENU, "BioGateway"));

        BGNodeDoubleClickNVTF doubleClickNVTF = new BGNodeDoubleClickNVTF();
        //registerAllServices(bundleContext, doubleClickNVTF, ezProps("PREFERRED_ACTION", "OPEN", "TITLE", "Expand/Collapse"));

        Properties doubleClickProperties = new Properties();
        doubleClickProperties.setProperty(PREFERRED_ACTION, "OPEN");
        doubleClickProperties.setProperty(TITLE, "Expand/Collapse Group");
        registerService(bundleContext, doubleClickNVTF, NodeViewTaskFactory.class, doubleClickProperties);

        BGExpandEdgeDoubleClickEVTF edgeDoubleClickEVTF = new BGExpandEdgeDoubleClickEVTF();
        registerAllServices(bundleContext, edgeDoubleClickEVTF, ezProps(PREFERRED_ACTION, "OPEN"));

    }




	private void setupServiceManager(BundleContext bundleContext) {
        CyNetworkManager networkManager = getService(bundleContext, CyNetworkManager.class);

        // This will also create a BGDataModelController object, which creates a BGConfig object and loads the XML file from the dataModelController.
        // The XML file is loaded SYNCHRONOUSLY, because we actually want to wait for it to load before loading the plugin.

        CySwingAppAdapter adapter = getService(bundleContext, CySwingAppAdapter.class);


        BGServiceManager.INSTANCE.setBundleContext(bundleContext);
        BGServiceManager.INSTANCE.setActivator(this);
        BGServiceManager.INSTANCE.setAdapter(adapter);
        BGServiceManager.INSTANCE.setApplicationManager(getService(bundleContext, CyApplicationManager.class));
        BGServiceManager.INSTANCE.setViewManager(getService(bundleContext, CyNetworkViewManager.class));
        BGServiceManager.INSTANCE.setNetworkManager(networkManager);
        BGServiceManager.INSTANCE.setNetworkFactory(getService(bundleContext, CyNetworkFactory.class));
        BGServiceManager.INSTANCE.setViewFactory(getService(bundleContext, CyNetworkViewFactory.class));
        BGServiceManager.INSTANCE.setVisualMappingManager(getService(bundleContext, VisualMappingManager.class));
        BGServiceManager.INSTANCE.setVisualFunctionFactoryDiscrete(getService(bundleContext,VisualMappingFunctionFactory.class, "(mapping.type=discrete)"));
        BGServiceManager.INSTANCE.setEventHelper(getService(bundleContext, CyEventHelper.class));
        BGServiceManager.INSTANCE.setTaskManager(getService(bundleContext, DialogTaskManager.class));
        BGServiceManager.INSTANCE.setLayoutAlgorithmManager(getService(bundleContext, CyLayoutAlgorithmManager.class));
        BGServiceManager.INSTANCE.setTableFactory(getService(bundleContext, CyTableFactory.class));
        BGServiceManager.INSTANCE.setTableManager(getService(bundleContext, CyTableManager.class));
        BGServiceManager.INSTANCE.setCreateNetworkViewTaskFactory(getService(bundleContext, CreateNetworkViewTaskFactory.class));
	}

    // Copied from "http://wiki.cytoscape.org/Cytoscape_3/AppDeveloper/Cytoscape_3_App_Cookbook":
    private static Properties ezProps(String... vals) {
        final Properties props = new Properties();
        for (int i = 0; i < vals.length; i += 2)
            props.put(vals[i], vals[i + 1]);
        return props;
    }
}
