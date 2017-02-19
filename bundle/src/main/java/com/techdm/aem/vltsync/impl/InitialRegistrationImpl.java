package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Deactivate;

/**
 * Provides capability similar to 'vlt sync register' command, associating a filesystem path to one or more
 * paths in the JCR.
 * 
 * @author Daniel Henrique Alves Lima
 *
 */
@Component(policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = true, immediate = true)
public class InitialRegistrationImpl {

	@Property(label = "Paths")
	private static final String FILTER_ROOTS_PROPERTY = "filter.roots";

	@Property(label = "Root Dir")
	private static final String LOCAL_PATH_PROPERTY = "local.path";
	
	@Property(label = "Overwrite Config Files")
	private static final String OVERWRITE_CONFIG_FILES_PROPERTY = "overwrite.config.files";

	/* Logger instance. */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Reference
	private ServiceSettingsImpl serviceSettings;

	private String[] filterRoots = null;

	private File localDir = null;
	
	private Boolean overwriteConfigFiles = null;

	@Activate
	protected void activate(final Map<String, Object> props) {
		logger.debug("activate(): props = {}", props);

		this.filterRoots = PropertiesUtil.toStringArray(props.get(FILTER_ROOTS_PROPERTY), new String[0]);
		final String localDirValue = PropertiesUtil.toString(props.get(LOCAL_PATH_PROPERTY), null);

		this.localDir = new File(localDirValue);
		this.overwriteConfigFiles = PropertiesUtil.toBoolean(OVERWRITE_CONFIG_FILES_PROPERTY, false);
		
		generateFiles();

		this.serviceSettings.addSyncRoot(this.localDir);
	}

	@Deactivate
	protected void deactivate() {
		if (this.localDir != null) {
			this.serviceSettings.removeSyncRoot(this.localDir);
		}

		this.filterRoots = null;
		this.localDir = null;
		this.overwriteConfigFiles = null;
	}

	private void generateFiles() throws IllegalStateException {
		logger.debug("generateFiles()");

		try {
			this.localDir.mkdirs();

			/*
			 * FileFilter fileFilter = new NotFileFilter(new
			 * RegexFileFilter("^.vlt-sync.+$")); File[] rootDirContents =
			 * rootDir.listFiles(fileFilter);
			 * 
			 * if (rootDirContents == null || rootDirContents.length == 0) {
			 */
			logger.debug("generateFiles(): Generating vlt sync config files at {}", this.localDir);
			generateVltSyncConfigFiles();
			/*
			 * } else { if (logger.isDebugEnabled()) {
			 * logger.debug("run(): {} not empty! It contains {}", rootDir,
			 * Arrays.asList(rootDirContents)); } }
			 */

		} catch (IOException e) {
			logger.error("generateFiles()", e);
			throw new IllegalStateException(e);
		}
	}

	private void generateVltSyncConfigFiles() throws IOException {
		generateWorkspaceFilterFile();
		generateConfigPropertyFile();
	}

	private void generateConfigPropertyFile() throws IOException {
		final String configPropertyFilename = ".vlt-sync-config.properties";
		final List<String> foundPaths = findRelativePaths(configPropertyFilename);
		final String syncOnce = localDirHasContents()? "FS2JCR" : "JCR2FS";		
		
		/*
		 * Don't overwrite the file if it already exists. Allowing the user
		 * to create or change .vlt-sync-config.properties guarantees a
		 * finer control over VTL sync service behavior.
		 */
		if (foundPaths.isEmpty() || this.overwriteConfigFiles) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new FileWriter(new File(this.localDir, configPropertyFilename)));

				writer.println("disabled=false");
				writer.println("sync-once=" + syncOnce);
			} finally {
				IOUtils.closeQuietly(writer);
			}
		} else {			
			if (logger.isDebugEnabled()) {
				logger.debug("generateConfigPropertyFile(): {} already contains {}!", this.localDir, foundPaths);
			}
		}
	}

	private boolean localDirHasContents() {
		FileFilter fileFilter = new NotFileFilter(new RegexFileFilter("^.vlt-sync.+$"));
		File[] rootDirContents = this.localDir.listFiles(fileFilter);

		return rootDirContents != null && rootDirContents.length > 0;
	}

	private void generateWorkspaceFilterFile() throws IOException {
		final String workspaceFilterFilename = ".vlt-sync-filter.xml";
		final String defaultWorkspaceFilterFilename = "META-INF/vault/filter.xml";

		final List<String> foundPaths = findRelativePaths(workspaceFilterFilename);
		final List<String> filterPaths = findRelativePaths(defaultWorkspaceFilterFilename,
				"../" + defaultWorkspaceFilterFilename);
		
		/*-
		 * To make this component compatible with other VLT commands, such
		 * as checkout and commit, we should not create a sync filter if a
		 * default filter already exists! 
		 * Reference: http://jackrabbit.apache.org/filevault/usage.html#a.vlt-sync-filter.xml
		 * 
		 * To guarantee a finer control to the user, don't overwrite an
		 * already created/configured file!
		 * 
		 */
		if (filterPaths.isEmpty() && (foundPaths.isEmpty() || this.overwriteConfigFiles)) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new FileWriter(new File(localDir, workspaceFilterFilename)));

				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				writer.println("<workspaceFilter version=\"1.0\">");
				for (String path : this.filterRoots) {
					writer.println("   <filter root=\"" + path + "\"/>");
				}
				writer.println("</workspaceFilter>");
			} finally {
				IOUtils.closeQuietly(writer);
			}
		} else {			
			if (logger.isDebugEnabled()) {
				logger.debug("generateWorkspaceFilterFile(): {} already contains {}!", this.localDir, foundPaths);
			}
		}
	}

	private List<String> findRelativePaths(final String... paths) {
		List<String> foundPaths = new ArrayList<String>();

		for (String path : paths) {
			final File file = new File(this.localDir, path);
			if (file.exists()) {
				foundPaths.add(path);
			}
		}

		return foundPaths;
	}

}
