package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*-
 * Vault Sync Service doesn't behave very well if it is restarted during
 * the first sync (sync once):
 * 
 * "Error during sync javax.jcr.RepositoryException: This session has been closed.
 * at
 * org.apache.jackrabbit.oak.jcr.delegate.SessionDelegate.checkAlive(SessionDelegate.java:290)"
 * 
 * Which results in an incomplete synchronization. That's why we try to
 * sleep a few moments, before changing its configuration and restarting
 * it indirectly.
 * 
 * However, this won't cover all the possibilities. The service can be in the middle of a
 * regular sync (which is undetectable) when we change its configuration.
 * Or .vlt-sync.properties can be manually changed (another uncovered situation).
 * 
 */

/**
 * Responsible for processing (asynchronously) the requests for change on VTL
 * Sync Service settings.
 * 
 * @author Daniel Henrique Alves Lima
 *
 */
@Component
@Service(value = { JobConsumer.class })
public class ServiceSettingsConsumerImpl implements JobConsumer {

	/** The Queue being observed by this consumer. */
	protected static final String TOPIC_NAME = "com/techdm/aem/vltsync/impl/ServiceSettingsQueue";

	@Property(value = TOPIC_NAME)
	private static final String PROP_TOPICS = JobConsumer.PROPERTY_TOPICS;

	/**
	 * Action (sync root added or sync root removed) to process.
	 */
	protected static final String KEY_ACTION = "action";

	/**
	 * Sync root being added or removed.
	 */
	protected static final String KEY_SYNC_ROOT = "syncRoot";

	/**
	 * Time (in milliseconds) that this consumer should wait before executing
	 * the next operation.
	 */
	protected static final String KEY_EXPECTED_SYNC_TIME = "expectedSyncTime";

	/**
	 * Action of adding a sync root.
	 */
	protected static final String ACTION_ADD = "addSyncRoot";

	/**
	 * Action of removing a sync root.
	 */
	protected static final String ACTION_REMOVE = "removeSyncRoot";

	/** VLT Sync Service Persistent ID. */
	protected static final String SERVICE_PID = "org.apache.jackrabbit.vault.sync.impl.VaultSyncServiceImpl";

	/** VLT Sync Service Property: is service enabled? */
	protected static final String PROP_ENABLED = "vault.sync.enabled";

	/** VLT Sync Service Property: root folders. */
	protected static final String PROP_SYNCROOTS = "vault.sync.syncroots";

	/* Logger instance. */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/* The service to get OSGi configs */
	@Reference
	private ConfigurationAdmin configAdmin;

	private long sleepTimeBeforeNextUpdate = 0;

	private long lastUpdateTime = System.currentTimeMillis() - sleepTimeBeforeNextUpdate;

	/**
	 * Process a configuration update of the VLT Sync Service.
	 * 
	 */
	public JobResult process(Job job) {
		try {
			final String action = job.getProperty(KEY_ACTION, String.class);
			final File syncRoot = job.getProperty(KEY_SYNC_ROOT, File.class);

			if (ACTION_ADD.equals(action)) {
				final Long expectedSyncTime = job.getProperty(KEY_EXPECTED_SYNC_TIME, Long.class);
				addSyncRoot(syncRoot, expectedSyncTime);
			} else if (ACTION_REMOVE.equals(action)) {
				removeSyncRoot(syncRoot);
			} else {
				throw new IllegalArgumentException("process(): Unknown action " + action);
			}

			return JobResult.OK;
		} catch (IllegalStateException e) {
			logger.error("process(): recoverable error", e);
			return JobResult.FAILED;
		} catch (RuntimeException e) {
			logger.error("process(): UNRECOVERABLE error", e);
			return JobResult.CANCEL;
		}
	}

	/**
	 * Add the specified directory to the sync roots, enabling the service when
	 * necessary.
	 * 
	 * @param syncRoot
	 *            directory to add
	 * @param expectedSyncTime
	 *            the expected sync time as result of adding this directory
	 */
	protected void addSyncRoot(final File syncRoot, final Long expectedSyncTime) throws IllegalStateException {
		logger.debug("addSyncRoot(): syncRoot = {}, expectedSyncTime = {}", syncRoot, expectedSyncTime);
		final Configuration configuration = getConfiguration();
		final Dictionary<String, Object> properties = getProperties(configuration);

		final Set<String> syncRoots = getSyncRoots(properties);

		syncRoots.add(syncRoot.getAbsolutePath());
		if (syncRoots.size() == 1) {
			enableSync(properties);
		}

		properties.put(PROP_SYNCROOTS, syncRoots.toArray(new String[syncRoots.size()]));
		update(configuration, properties, expectedSyncTime);
	}

	/**
	 * Remove the specified directory from the sync roots, disabling the service
	 * when necessary.
	 * 
	 * @param syncRoot
	 *            directory to remove
	 */
	protected void removeSyncRoot(final File syncRoot) throws IllegalStateException {
		logger.debug("removeSyncRoot(): syncRoot = {}", syncRoot);
		final Configuration configuration = getConfiguration();
		final Dictionary<String, Object> properties = getProperties(configuration);

		final Set<String> syncRoots = getSyncRoots(properties);

		syncRoots.remove(syncRoot.getAbsolutePath());
		properties.put(PROP_SYNCROOTS, syncRoots.toArray(new String[syncRoots.size()]));

		if (syncRoots.size() == 0) {
			disableSync(properties);
		}

		update(configuration, properties, null);
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

	private void update(final Configuration configuration, final Dictionary<String, Object> properties,
			final Long timeToWaitBeforeNextUpdate) throws IllegalStateException {
		logger.debug("update(): configuration = {}, properties = {}", configuration, properties);

		waitBeforeUpdate(timeToWaitBeforeNextUpdate);

		/*
		 * Now, change its configuration!
		 */

		try {
			configuration.update(properties);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void waitBeforeUpdate(final Long timeToWaitBeforeNextUpdate) {
		final long timeBetweenCalls = System.currentTimeMillis() - this.lastUpdateTime;
		final long remainingSleepTime = this.sleepTimeBeforeNextUpdate - timeBetweenCalls;

		logger.debug("waitBeforeUpdate(): lastUpdateTime = {}, now = {}", this.lastUpdateTime,
				System.currentTimeMillis());
		logger.debug("waitBeforeUpdate(): sleepTimeBeforeNextUpdate = {}, remainingSleepTime = {}",
				this.sleepTimeBeforeNextUpdate, remainingSleepTime);

		if (remainingSleepTime > 0) {
			logger.debug("waitBeforeUpdate(): sleeping for {} (ms)", remainingSleepTime);
			Thread.yield();
			try {
				Thread.sleep(remainingSleepTime);
			} catch (InterruptedException e) {
			} finally {
				logger.debug("waitBeforeUpdate(): waking up!", remainingSleepTime);
			}
		}
		this.sleepTimeBeforeNextUpdate = timeToWaitBeforeNextUpdate != null ? timeToWaitBeforeNextUpdate : 0;
		this.lastUpdateTime = System.currentTimeMillis();
	}

}
