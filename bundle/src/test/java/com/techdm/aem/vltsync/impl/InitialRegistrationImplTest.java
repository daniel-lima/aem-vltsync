package com.techdm.aem.vltsync.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.ServiceException;

import junitx.framework.FileAssert;
import junitx.util.PrivateAccessor;

public class InitialRegistrationImplTest {

	private final String SYNC_CONFIG_FN = ".vlt-sync-config.properties";
	private final String SYNC_FILTER_FN = ".vlt-sync-filter.xml";
	private final String DEFAULT_FILTER_FN = "../META-INF/vault/filter.xml";

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private ServiceSettingsImpl serviceSettings = null;

	private Map<String, Object> props;

	private File baseDir;

	private File generatedConfigFile;

	private File generatedFilterFile;

	private InitialRegistrationImpl initialRegistration = new InitialRegistrationImpl();

	@Before
	public void setUp() throws NoSuchFieldException, IOException {
		this.baseDir = File.createTempFile(getClass().getName(), "_tmp");
		this.baseDir.delete();
		this.baseDir.mkdir();
		this.baseDir.deleteOnExit();
		new File(this.baseDir, DEFAULT_FILTER_FN).delete();

		this.generatedConfigFile = new File(this.baseDir, SYNC_CONFIG_FN);
		this.generatedFilterFile = new File(this.baseDir, SYNC_FILTER_FN);

		this.serviceSettings = mock(ServiceSettingsImpl.class);

		this.props = new LinkedHashMap<String, Object>();
		this.props.put(InitialRegistrationImpl.PROP_LOCAL_PATH, this.baseDir.getAbsolutePath());
		this.props.put(InitialRegistrationImpl.PROP_FILTER_ROOTS,
				new String[] { "/content/my-app", "/etc/designs/my-app" });

		PrivateAccessor.setField(this.initialRegistration, "serviceSettings", this.serviceSettings);
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(this.baseDir);
		new File(this.baseDir, DEFAULT_FILTER_FN).delete();
	}

	@Test
	public void testActivateMissingPropertiesFailure() {
		expectedEx.expect(ServiceException.class);
		expectedEx.expectMessage(" is mandatory!");

		this.initialRegistration.activate(new LinkedHashMap<String, Object>());
	}

	@Test
	public void testActivateEmptyDirSuccess() throws URISyntaxException {
		/* Prepare data. */
		this.baseDir.delete();
		assertEquals(false, this.baseDir.exists());

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivateDirWithIgnorableContentSuccess() throws URISyntaxException, IOException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(".vlt-sync.log");

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivateDirWithIgnorableContentOverwriteSuccess() throws URISyntaxException, IOException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(".vlt-sync.log");
		this.props.put(InitialRegistrationImpl.PROP_OVERWRITE_CONFIG_FILES, true);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivateDirWithContentsSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles("readme.txt", "LICENSE");

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data2-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivatePropertyFileExistsSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(SYNC_CONFIG_FN);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("empty-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, null);
	}

	@Test
	public void testActivatePropertyFileExistsNoSyncOnceSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		FileUtils.copyFile(getResource("data3-config.properties"), this.generatedConfigFile);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data3-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, null);
	}
	
	@Test
	public void testActivatePropertyFileExistsOverwriteSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(SYNC_CONFIG_FN);
		this.props.put(InitialRegistrationImpl.PROP_OVERWRITE_CONFIG_FILES, true);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivatePropertyFileExistsDirWithContentsOverwriteSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles("README.md", SYNC_CONFIG_FN);
		this.props.put(InitialRegistrationImpl.PROP_OVERWRITE_CONFIG_FILES, true);
		this.props.put(InitialRegistrationImpl.PROP_EXPECTED_SYNC_ONCE_TIME, 3001l);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data2-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3001l);
	}

	@Test
	public void testActivateFilterFileExistsSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(SYNC_FILTER_FN);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("empty-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivateFilterFileExistsOverwriteSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(SYNC_FILTER_FN);
		this.props.put(InitialRegistrationImpl.PROP_OVERWRITE_CONFIG_FILES, true);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		FileAssert.assertEquals(getResource("data1-filter.xml"), this.generatedFilterFile);
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivateDefaultFilterFileExistsSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(DEFAULT_FILTER_FN);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		assertEquals(false, this.generatedFilterFile.exists());
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testActivateDefaultFilterFileExistsOverwriteSuccess() throws IOException, URISyntaxException {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);
		createTempFiles(DEFAULT_FILTER_FN);
		this.props.put(InitialRegistrationImpl.PROP_OVERWRITE_CONFIG_FILES, true);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);

		/* Check its results. */
		FileAssert.assertEquals(getResource("data1-config.properties"), this.generatedConfigFile);
		assertEquals(false, this.generatedFilterFile.exists());
		verify(this.serviceSettings, times(1)).addSyncRoot(this.baseDir, 3000l);
	}

	@Test
	public void testDeactivate() {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);
		this.initialRegistration.deactivate();

		/* Check its results. */
		verify(this.serviceSettings, times(1)).removeSyncRoot(this.baseDir);
	}

	@Test
	public void testDeactivateTwice() {
		/* Prepare data. */
		assertEquals(0, this.baseDir.list().length);

		/* Invoke method. */
		this.initialRegistration.activate(this.props);
		this.initialRegistration.deactivate();
		this.initialRegistration.deactivate();

		/* Check its results. */
		verify(this.serviceSettings, times(1)).removeSyncRoot(this.baseDir);
	}

	private void createTempFiles(final String... relativePaths) throws IOException {
		for (String relativePath : relativePaths) {
			File file = new File(this.baseDir, relativePath);

			File dir = file.getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
				dir.deleteOnExit();
			}

			file.createNewFile();
			file.deleteOnExit();
		}
	}

	private File getResource(final String resourcePath) throws URISyntaxException {
		URL url = getClass().getResource(resourcePath);
		File file = new File(url.toURI());
		return file;
	}

}
