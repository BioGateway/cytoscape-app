package org.cytoscape.biogwplugin.internal

import org.cytoscape.biogwplugin.internal.server.BGDictEndpoint
import org.apache.http.impl.client.HttpClients
import org.cytoscape.app.CyAppAdapter
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.biogwplugin.internal.gui.BGControlPanel
import org.cytoscape.biogwplugin.internal.server.BGDataModelController
import org.cytoscape.biogwplugin.internal.util.BGVisualStyleBuilder
import org.cytoscape.biogwplugin.internal.util.Constants
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.task.create.CreateNetworkViewTaskFactory
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.swing.DialogTaskManager
import org.osgi.framework.BundleContext


/**
 * Created by sholmas on 23/03/2017.
 */
class BGServiceManager {

    // This seems like a good place to store static properties.
    var bundleContext: BundleContext? = null
    var activator: CyActivator? = null
    var adapter: CyAppAdapter? = null

    var applicationManager: CyApplicationManager? = null
    var viewManager: CyNetworkViewManager? = null
    var viewFactory: CyNetworkViewFactory? = null
    var createNetworkViewTaskFactory: CreateNetworkViewTaskFactory? = null
    var networkFactory: CyNetworkFactory? = null
    var networkManager: CyNetworkManager? = null
    var eventHelper: CyEventHelper? = null
    var taskManager: DialogTaskManager? = null

    // References to important objects used for GUI and task creation:
    var visualMappingManager: VisualMappingManager? = null
    var visualFunctionFactoryDiscrete: VisualMappingFunctionFactory? = null
    var layoutAlgorithmManager: CyLayoutAlgorithmManager? = null
    var tableFactory: CyTableFactory? = null
    var tableManager: CyTableManager? = null


    var dataModelController: BGDataModelController
    val cache: BGDataModelController.BGCache get() {
        return dataModelController.cache
    }

    var httpClient = HttpClients.createDefault()
    val endpoint = BGDictEndpoint("http://localhost:3002/")

    var controlPanel: BGControlPanel? = null

    val visualStyleBuilder = BGVisualStyleBuilder(this)

    // These values will be updated by the settings in the config XML.
    var serverPath: String = Constants.SERVER_PATH
    var dictionaryServerPath: String = Constants.DICTIONARY_SERVER_PATH

    init {
        this.dataModelController = BGDataModelController(this)
    }

    constructor()

    constructor(cyActivator: CyActivator, adapter: CyAppAdapter, bundleContext: BundleContext) {
        this.bundleContext = bundleContext
        this.activator = cyActivator
        this.adapter = adapter
    }

    fun getVisualManager(): VisualMappingManager? {
        return visualMappingManager
    }
}
