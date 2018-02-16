
package org.cytoscape.biogwplugin;


import org.cytoscape.model.CyNetwork;



/**
 * APIs in Cytoscape 3.x are defined by their interfaces. Java interfaces
 * are used by OSGi to define services.  Any Java interface can define a
 * service, meaning any class that implements an interface, can be registered
 * as a service.  Using objects as services through their interfaces rather
 * than through their implementation class directly helps ensure that we
 * write modular and extensible code.
 */
public interface BiogwPlugin {

	/**
	 * An example interface that takes a {@link CyNetwork} argument, 
	 * and "analyzes" the nodes in the network, returning a collection
	 * of nodes.
	 *
	 * @param n The network to be analyzed.
	 * @return A collection of "analyzed" nodes.
	 */
}
