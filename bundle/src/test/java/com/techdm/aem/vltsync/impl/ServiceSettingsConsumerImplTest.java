package com.techdm.aem.vltsync.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import junitx.util.PrivateAccessor;

public class ServiceSettingsConsumerImplTest {

	private ServiceSettingsConsumerImpl serviceSettingsConsumer = new ServiceSettingsConsumerImpl();

	private ConfigurationAdmin configurationAdmin = null;

	private Configuration configuration = null;

	private Hashtable<String, Object> dictionary = null;

	@Before
	public void setUp() throws IOException, NoSuchFieldException {
		this.configurationAdmin = mock(ConfigurationAdmin.class);
		this.configuration = mock(Configuration.class);
		this.dictionary = new Hashtable<String, Object>();

		when(this.configurationAdmin.getConfiguration(ServiceSettingsConsumerImpl.SERVICE_PID))
				.thenReturn(this.configuration);
		when(this.configuration.getProperties()).thenReturn(this.dictionary);

		PrivateAccessor.setField(this.serviceSettingsConsumer, "configAdmin", this.configurationAdmin);
	}

	@Test
	public void testAddSyncRootToEmptySuccess() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		this.dictionary.put(ServiceSettingsConsumerImpl.PROP_SYNCROOTS, new String[] {});

		/* Invoke method. */
		this.serviceSettingsConsumer.addSyncRoot(new File("/virtual/root"), null);

		/* Check its results. */
		assertEquals(true, this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root" },
				(String[]) this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testAddSyncRootToNonEmptySuccess() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		this.dictionary.put(ServiceSettingsConsumerImpl.PROP_SYNCROOTS, new String[] { "/virtual/old" });

		/* Invoke method. */
		this.serviceSettingsConsumer.addSyncRoot(new File("/virtual/new"), null);

		/* Check its results. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/old", "/virtual/new" },
				(String[]) this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testRemoveSyncRootLast() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		this.dictionary.put(ServiceSettingsConsumerImpl.PROP_SYNCROOTS, new String[] { "/virtual/old" });

		/* Invoke method. */
		this.serviceSettingsConsumer.removeSyncRoot(new File("/virtual/old"));

		/* Check its results. */
		assertEquals(false, this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertArrayEquals(new String[] {}, (String[]) this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testRemoveSyncRoot() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		this.dictionary.put(ServiceSettingsConsumerImpl.PROP_SYNCROOTS,
				new String[] { "/virtual/old1", "/virtual/old2" });

		/* Invoke method. */
		this.serviceSettingsConsumer.removeSyncRoot(new File("/virtual/old1"));

		/* Check its results. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/old2" },
				(String[]) this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testAddSyncRootNullPropertiesSuccess() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		/* Invoke method. */
		this.serviceSettingsConsumer.addSyncRoot(new File("/virtual/root"), null);

		/* Check its results. */
		assertEquals(true, this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertArrayEquals(new String[] { "/virtual/root" },
				(String[]) this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

	@Test
	public void testRemoveSyncRootNullProperties() throws IOException {
		/* Prepare data. */
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertNull(this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		/* Invoke method. */
		this.serviceSettingsConsumer.removeSyncRoot(new File("/virtual/old1"));

		/* Check its results. */
		assertEquals(false, this.dictionary.get(ServiceSettingsConsumerImpl.PROP_ENABLED));
		assertArrayEquals(new String[] {}, (String[]) this.dictionary.get(ServiceSettingsConsumerImpl.PROP_SYNCROOTS));

		verify(this.configuration, times(1)).update(this.dictionary);
	}

}
