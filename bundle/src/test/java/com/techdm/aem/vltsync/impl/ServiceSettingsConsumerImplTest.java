package com.techdm.aem.vltsync.impl;

import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.ACTION_ADD;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.ACTION_REMOVE;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.KEY_ACTION;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.KEY_EXPECTED_SYNC_TIME;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.KEY_SYNC_ROOT;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.PROP_ENABLED;
import static com.techdm.aem.vltsync.impl.ServiceSettingsConsumerImpl.PROP_SYNCROOTS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import junitx.util.PrivateAccessor;

public class ServiceSettingsConsumerImplTest {

	private ServiceSettingsConsumerImpl serviceSettingsConsumer = null;

	private ConfigurationAdmin configurationAdmin = null;

	private Configuration configuration = null;

	private Hashtable<String, Object> dictionary = null;
	
	private Job job = null;
	

	@Before
	public void setUp() throws IOException, NoSuchFieldException {
		this.serviceSettingsConsumer = new ServiceSettingsConsumerImpl();
		
		this.configurationAdmin = mock(ConfigurationAdmin.class);
		this.configuration = mock(Configuration.class);
		this.dictionary = new Hashtable<String, Object>();
		this.job = mock(Job.class);

		when(this.configurationAdmin.getConfiguration(ServiceSettingsConsumerImpl.SERVICE_PID))
				.thenReturn(this.configuration);
		when(this.configuration.getProperties()).thenReturn(this.dictionary);

		PrivateAccessor.setField(this.serviceSettingsConsumer, "configAdmin", this.configurationAdmin);
	}

	@Test
	public void testAddSyncRootToEmptySuccess() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] {});

		/* Invoke method. */
		this.serviceSettingsConsumer.addSyncRoot(new File("/virtual/root"), null);

		/* Check its results. */
		assertEquals(true, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root" },
				(String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testAddSyncRootToNonEmptySuccess() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] { "/virtual/old" });

		/* Invoke method. */
		this.serviceSettingsConsumer.addSyncRoot(new File("/virtual/new"), null);

		/* Check its results. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/old", "/virtual/new" },
				(String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testRemoveSyncRootLast() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] { "/virtual/old" });

		/* Invoke method. */
		this.serviceSettingsConsumer.removeSyncRoot(new File("/virtual/old"));

		/* Check its results. */
		assertEquals(false, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] {}, (String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testRemoveSyncRoot() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS,
				new String[] { "/virtual/old1", "/virtual/old2" });

		/* Invoke method. */
		this.serviceSettingsConsumer.removeSyncRoot(new File("/virtual/old1"));

		/* Check its results. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/old2" },
				(String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testAddSyncRootNullPropertiesSuccess() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		assertNull(this.dictionary.get(PROP_SYNCROOTS));

		/* Invoke method. */
		this.serviceSettingsConsumer.addSyncRoot(new File("/virtual/root"), null);

		/* Check its results. */
		assertEquals(true, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root" },
				(String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testRemoveSyncRootNullProperties() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		assertNull(this.dictionary.get(PROP_SYNCROOTS));

		/* Invoke method. */
		this.serviceSettingsConsumer.removeSyncRoot(new File("/virtual/old1"));

		/* Check its results. */
		assertEquals(false, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] {}, (String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}
	
	@Test
	public void testProcessAddSyncRoot() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] {});

		when(this.job.getProperty(KEY_ACTION, String.class)).thenReturn(ACTION_ADD);
		when(this.job.getProperty(KEY_SYNC_ROOT, File.class)).thenReturn(new File("/virtual/root"));
		when(this.job.getProperty(KEY_EXPECTED_SYNC_TIME, Long.class)).thenReturn(15l);

		/* Invoke method. */
		JobResult result = this.serviceSettingsConsumer.process(this.job);

		/* Check its results. */
		assertEquals(JobResult.OK, result);

		assertEquals(true, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root" }, (String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}
	
	
	@Test
	public void testProcessAddSyncRootNoSyncOnce() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] {});

		when(this.job.getProperty(KEY_ACTION, String.class)).thenReturn(ACTION_ADD);
		when(this.job.getProperty(KEY_SYNC_ROOT, File.class)).thenReturn(new File("/virtual/root"));

		/* Invoke method. */
		JobResult result = this.serviceSettingsConsumer.process(this.job);

		/* Check its results. */
		assertEquals(JobResult.OK, result);

		assertEquals(true, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root" }, (String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}
		

	@Test
	public void testProcessRemoveSyncRoot() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] { "/virtual/old1", "/virtual/old2" });

		when(this.job.getProperty(KEY_ACTION, String.class)).thenReturn(ACTION_REMOVE);
		when(this.job.getProperty(KEY_SYNC_ROOT, File.class)).thenReturn(new File("/virtual/old1"));

		/* Invoke method. */
		JobResult result = this.serviceSettingsConsumer.process(this.job);

		/* Check its results. */
		assertEquals(JobResult.OK, result);

		assertNull(this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/old2" }, (String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}
	
	
	@Test
	public void testProcessNoAction() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] { "/virtual/old1", "/virtual/old2" });

		when(this.job.getProperty(KEY_SYNC_ROOT, File.class)).thenReturn(new File("/virtual/old1"));

		/* Invoke method. */
		JobResult result = this.serviceSettingsConsumer.process(this.job);

		/* Check its results. */
		assertEquals(JobResult.CANCEL, result);

		assertNull(this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/old1", "/virtual/old2" }, (String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(0)).update(this.dictionary);
	}
	
	
	
	@Test
	public void testProcessWaitTime() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(PROP_ENABLED));
		this.dictionary.put(PROP_SYNCROOTS, new String[] {});

		when(this.job.getProperty(KEY_ACTION, String.class)).thenReturn(ACTION_ADD);
		when(this.job.getProperty(KEY_SYNC_ROOT, File.class)).thenReturn(new File("/virtual/root1"));
		when(this.job.getProperty(KEY_EXPECTED_SYNC_TIME, Long.class)).thenReturn(1000l);

		/* Invoke method. */
		long start = System.currentTimeMillis();		
		JobResult result = this.serviceSettingsConsumer.process(this.job);
		long end = System.currentTimeMillis();

		/* Check its results. */
		assertEquals(JobResult.OK, result);
		assertTrue((end - start) < 300l);

		/* Prepare data. */
		when(this.job.getProperty(KEY_SYNC_ROOT, File.class)).thenReturn(new File("/virtual/root2"));
		when(this.job.getProperty(KEY_EXPECTED_SYNC_TIME, Long.class)).thenReturn(null);

		/* Invoke method. */
		result = this.serviceSettingsConsumer.process(this.job);
		end = System.currentTimeMillis();

		/* Check its results. */
		assertEquals(JobResult.OK, result);
		assertTrue((end - start) >= 1000l);

		assertEquals(true, this.dictionary.get(PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root1", "/virtual/root2" },
				(String[]) this.dictionary.get(PROP_SYNCROOTS));

		verify(this.configuration, times(2)).update(this.dictionary);
	}
	

}
