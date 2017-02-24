package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.jobs.JobManager;
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

	/* Logger instance. */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Reference
	private JobManager jobManager;

	/**
	 * Add the specified directory to the sync roots, enabling the service when
	 * necessary.
	 * 
	 * @param syncRoot
	 *            directory to add
	 * @param expectedSyncTime
	 *            the expected sync time as result of adding this directory
	 */
	public void addSyncRoot(final File syncRoot, Long expectedSyncTime) throws IllegalStateException {
		logger.debug("addSyncRoot(): syncRoot = {}", syncRoot);

		final Map<String, Object> props = new LinkedHashMap<String, Object>();
		props.put(ServiceSettingsConsumerImpl.KEY_ACTION, ServiceSettingsConsumerImpl.ACTION_ADD);
		props.put(ServiceSettingsConsumerImpl.KEY_SYNC_ROOT, syncRoot);
		props.put(ServiceSettingsConsumerImpl.KEY_EXPECTED_SYNC_TIME, expectedSyncTime);

		jobManager.addJob(ServiceSettingsConsumerImpl.TOPIC_NAME, props);
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

		final Map<String, Object> props = new LinkedHashMap<String, Object>();
		props.put(ServiceSettingsConsumerImpl.KEY_ACTION, ServiceSettingsConsumerImpl.ACTION_REMOVE);
		props.put(ServiceSettingsConsumerImpl.KEY_SYNC_ROOT, syncRoot);

		jobManager.addJob(ServiceSettingsConsumerImpl.TOPIC_NAME, props);
	}

}
