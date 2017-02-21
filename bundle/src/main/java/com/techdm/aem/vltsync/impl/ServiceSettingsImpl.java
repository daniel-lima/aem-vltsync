package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles (reads and writes)
 * org.apache.jackrabbit.vault.sync.impl.VaultSyncServiceImpl settings.
 * 
 * @author Daniel Henrique Alves Lima
 *
 */
@Service(ServiceSettingsImpl.class)
@Component
public class ServiceSettingsImpl {

	protected static final String SERVICE_PID = "org.apache.jackrabbit.vault.sync.impl.VaultSyncServiceImpl";

	protected static final String PROP_ENABLED = "vault.sync.enabled";

	protected static final String PROP_SYNCROOTS = "vault.sync.syncroots";

	/* Logger instance. */
	private final Logger logger = LoggerFactory.getLogger(getClass());

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
		logger.debug("addSyncRoot(): syncRoot = {}", syncRoot);
		final Configuration configuration = getConfiguration();
		final Dictionary<String, Object> properties = getProperties(configuration);

		final Set<String> syncRoots = getSyncRoots(properties);

		syncRoots.add(syncRoot.getAbsolutePath());
		if (syncRoots.size() == 1) {
			enableSync(properties);
		}

		properties.put(PROP_SYNCROOTS, syncRoots.toArray(new String[syncRoots.size()]));
		update(configuration, properties);
	}

	/**
	 * Remove the specified directory from the sync roots, disabling the service
	 * when necessary.
	 * 
	 * @param syncRoot
	 *            directory to remove
	 */
	public void removeSyncRoot(final File syncRoot) throws IllegalStateException {
		logger.debug("removeSyncRoot(): syncRoot = {}", syncRoot);
		final Configuration configuration = getConfiguration();
		final Dictionary<String, Object> properties = getProperties(configuration);

		final Set<String> syncRoots = getSyncRoots(properties);

		syncRoots.remove(syncRoot.getAbsolutePath());
		properties.put(PROP_SYNCROOTS, syncRoots.toArray(new String[syncRoots.size()]));

		if (syncRoots.size() == 0) {
			disableSync(properties);
		}

		update(configuration, properties);
	}

	private void enableSync(final Dictionary<String, Object> properties) {
		logger.debug("enableSync(): properties = {}", properties);
		properties.put(PROP_ENABLED, Boolean.TRUE);
	}

	private void disableSync(final Dictionary<String, Object> properties) {
		logger.debug("disableSync(): properties = {}", properties);
		properties.put(PROP_ENABLED, Boolean.FALSE);
	}

	private Configuration getConfiguration() throws IllegalStateException {
		try {
			return this.configAdmin.getConfiguration(SERVICE_PID);
		} catch (IOException e) {
			logger.error("getConfiguration()", e);
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private Dictionary<String, Object> getProperties(final Configuration configuration) {
		Dictionary<String, Object> properties = configuration.getProperties();
		return properties;
	}

	private Set<String> getSyncRoots(final Dictionary<String, Object> properties) {
		final Set<String> syncRoots = new LinkedHashSet<String>();
		String[] syncRootArray = PropertiesUtil.toStringArray(properties.get(PROP_SYNCROOTS));
		if (syncRootArray != null) {
			syncRoots.addAll(Arrays.asList(syncRootArray));
			syncRootArray = null;
		}

		return syncRoots;
	}

	private void update(final Configuration configuration, final Dictionary<String, Object> properties)
			throws IllegalStateException {
		logger.debug("update(): configuration = {}, properties = {}", configuration, properties);
		try {
			configuration.update(properties);
		} catch (IOException e) {
			logger.error("update()", e);
			throw new IllegalStateException(e);
		}
	}

}
