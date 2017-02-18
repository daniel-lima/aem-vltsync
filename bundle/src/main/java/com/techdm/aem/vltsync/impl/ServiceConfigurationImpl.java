package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import aQute.bnd.annotation.component.Component;

/**
 * Service to handle org.apache.jackrabbit.vault.sync.impl.VaultSyncServiceImpl
 * settings.
 * 
 * @author Daniel Henrique Alves Lima
 *
 */
@Service(ServiceConfigurationImpl.class)
@Component
public class ServiceConfigurationImpl {

	private static final String SERVICE_PID = "org.apache.jackrabbit.vault.sync.impl.VaultSyncServiceImpl";

	private static final String ENABLED_PROPERTY = "vault.sync.enabled";

	private static final String SYNCROOTS_PROPERTY = "vault.sync.syncroots";

	/* The service to get OSGi configs */
	@Reference
	private ConfigurationAdmin configAdmin;

	/**
	 * Add the specified directory to the sync roots, enabling the service when
	 * necessary.
	 * 
	 * @param syncRoot
	 *            directory to add
	 */
	public void addSyncRoot(final File syncRoot) throws IllegalStateException {
		final Configuration configuration = getConfiguration();
		final Dictionary<String, Object> properties = getProperties(configuration);

		final Set<String> syncRoots = getSyncRoots(properties);

		syncRoots.add(syncRoot.getAbsolutePath());
		if (syncRoots.size() == 1) {
			enableSync(properties);
		}

		properties.put(SYNCROOTS_PROPERTY, syncRoots.toArray(new String[syncRoots.size()]));
		update(configuration);
	}

	/**
	 * Remove the specified directory from the sync roots, disabling the service
	 * when necessary.
	 * 
	 * @param syncRoot
	 *            directory to remove
	 */
	public void removeSyncRoot(final File syncRoot) throws IllegalStateException {
		final Configuration configuration = getConfiguration();
		final Dictionary<String, Object> properties = getProperties(configuration);

		final Set<String> syncRoots = getSyncRoots(properties);

		syncRoots.remove(syncRoot.getAbsolutePath());
		properties.put(SYNCROOTS_PROPERTY, syncRoots.toArray(new String[syncRoots.size()]));

		if (syncRoots.size() == 0) {
			disableSync(properties);
		}

		update(configuration);
	}

	private void enableSync(final Dictionary<String, Object> properties) {
		properties.put(ENABLED_PROPERTY, Boolean.TRUE);
	}

	private void disableSync(final Dictionary<String, Object> properties) {
		properties.put(ENABLED_PROPERTY, Boolean.FALSE);
	}

	private Configuration getConfiguration() throws IllegalStateException {
		try {
			return this.configAdmin.getConfiguration(SERVICE_PID);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private Dictionary<String, Object> getProperties(final Configuration configuration) {
		return configuration.getProperties();
	}

	private Set<String> getSyncRoots(final Dictionary<String, Object> properties) {
		final Set<String> syncRoots = new LinkedHashSet<String>();
		String[] syncRootArray = PropertiesUtil.toStringArray(properties.get(SYNCROOTS_PROPERTY));
		if (syncRootArray != null) {
			syncRoots.addAll(Arrays.asList(syncRootArray));
			syncRootArray = null;
		}
		return syncRoots;
	}

	private void update(final Configuration configuration) throws IllegalStateException {
		try {
			configuration.update();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
