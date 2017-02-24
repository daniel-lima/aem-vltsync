package com.techdm.aem.vltsync.impl;

import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.ACTION_ADD;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.ACTION_REMOVE;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.KEY_ACTION;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.KEY_EXPECTED_SYNC_TIME;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.KEY_SYNC_ROOT;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.TOPIC_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.event.jobs.JobManager;
import org.junit.Before;
import org.junit.Test;

import junitx.util.PrivateAccessor;

public class ServiceSettingsImplTest {

	private ServiceSettingsImpl serviceSettings = new ServiceSettingsImpl();

	private JobManager jobManager;

	private Map<String, Object> props;

	@Before
	public void setUp() throws NoSuchFieldException {
		this.jobManager = mock(JobManager.class);
		this.props = new LinkedHashMap<String, Object>();

		PrivateAccessor.setField(this.serviceSettings, "jobManager", this.jobManager);
	}

	@Test
	public void testAddSyncRoot() {
		/* Prepare data. */
		this.props.put(KEY_ACTION, ACTION_ADD);
		this.props.put(KEY_SYNC_ROOT, new File("/virtual/root"));
		this.props.put(KEY_EXPECTED_SYNC_TIME, 4000l);

		/* Invoke method. */
		this.serviceSettings.addSyncRoot(new File("/virtual/root"), 4000l);

		/* Check its results. */
		verify(this.jobManager, times(1)).addJob(TOPIC_NAME, this.props);
	}

	@Test
	public void testAddSyncRootNoSyncOnce() {
		/* Prepare data. */
		this.props.put(KEY_ACTION, ACTION_ADD);
		this.props.put(KEY_SYNC_ROOT, new File("/virtual/root"));
		this.props.put(KEY_EXPECTED_SYNC_TIME, null);

		/* Invoke method. */
		this.serviceSettings.addSyncRoot(new File("/virtual/root"), null);

		/* Check its results. */
		verify(this.jobManager, times(1)).addJob(TOPIC_NAME, this.props);
	}

	@Test
	public void testRemoveSyncRoot() {
		/* Prepare data. */
		this.props.put(KEY_ACTION, ACTION_REMOVE);
		this.props.put(KEY_SYNC_ROOT, new File("/virtual/root"));

		/* Invoke method. */
		this.serviceSettings.removeSyncRoot(new File("/virtual/root"));

		/* Check its results. */
		verify(this.jobManager, times(1)).addJob(TOPIC_NAME, this.props);
	}

}
