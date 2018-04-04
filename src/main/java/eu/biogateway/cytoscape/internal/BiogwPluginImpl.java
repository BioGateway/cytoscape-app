package eu.biogateway.cytoscape.internal;


import eu.biogateway.cytoscape.BiogwPlugin;

/**
 * This is an implementation of the BiogwPlugin interface.
 * This will be registered as a service in the Spring Dynamic
 * Modules XML configuration files that can be found in
 * the directory src/main/resources/META-INF/spring.
 * <br/>
 * The reason that this class is in a separate "internal" package
 * is so that consumers of this bundle cannot access this code
 * directly. The osgi.bnd file in the top level directory of this
 * project defines this package as "private."  That means that 
 * while any of the packages in this bundle can access this package,
 * OSGi forbids any OTHER bundles for accessing this package.
 * OSGi only allows access to this class as a registered service
 * of the BiogwPlugin interface. This approach ensures that
 * consumers of the BiogwPlugin API will only  ever use the API
 * interface and never the actual implementation. This gives YOU 
 * the flexibility to change this implemenation without breaking 
 * anyone else's code!!!
 */
public class BiogwPluginImpl implements BiogwPlugin {

    private final BGServiceManager serviceManager;

	public BiogwPluginImpl(BGServiceManager serviceManager) {
	    this.serviceManager = serviceManager;
	}

	/**
	 * Basic implemenation of the BiogwPlugin API.
	 *
	 * @param n The network to be analyzed.
	 * @return A collection of analyzed nodes.
	 */


}
