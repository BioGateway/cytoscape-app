package eu.biogateway.app.internal

import eu.biogateway.app.internal.server.BGDictEndpoint
import org.apache.http.impl.client.HttpClients
import org.cytoscape.application.CyApplicationManager
import eu.biogateway.app.internal.gui.BGControlPanel
import eu.biogateway.app.internal.model.BGConfig
import eu.biogateway.app.internal.model.BGDataModelController
import eu.biogateway.app.internal.model.BGNetworkConverter
import eu.biogateway.app.internal.util.BGVisualStyleBuilder
import org.cytoscape.app.swing.CySwingAppAdapter
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
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.swing.DialogTaskManager
import org.osgi.framework.BundleContext
import org.osgi.framework.Version


/**
 * Created by sholmas on 23/03/2017.
 */

object BGBundleContext {
    var bundleContext: BundleContext? = null
    val version: Version? get() {
        return bundleContext?.bundle?.version
    }
}


object BGServiceManager {

    // This seems like a good place to store static properties.
    var activator: CyActivator? = null
    var adapter: CySwingAppAdapter? = null

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

    val networkConverter = BGNetworkConverter(this)
    var dataModelController: BGDataModelController

    // NOTE! THIS IS NULL BEFORE THE DATAMODELCONTROLLER IS INITIALIZED!
    val config: BGConfig get() {
        return dataModelController.config
    }

    var httpClient = HttpClients.createDefault()

    var controlPanel: BGControlPanel? = null
    val visualStyleBuilder = BGVisualStyleBuilder(this)

    // These values will be updated by the settings in the config XML.
    var serverPath: String = ""
    var dictionaryServerPath: String = ""
        set(value) {
        field = value
        endpoint = BGDictEndpoint(value)
    }

    var endpoint = BGDictEndpoint(dictionaryServerPath)


    init {
        this.dataModelController = BGDataModelController()
    }


    fun getVisualManager(): VisualMappingManager? {
        return visualMappingManager
    }

    fun execute(task: AbstractTask) {
        taskManager?.execute(TaskIterator(task))
    }
}
