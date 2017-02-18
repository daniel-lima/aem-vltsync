package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
 * Component to register a local folder as sync folder.
 * 
 * @author Daniel Henrique Alves Lima
 *
 */
@Component(policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = true, immediate = true)
public class RegistrationImpl {

	@Property(label = "Paths")
	private static final String PATHS_PROPERTY = "paths";

	@Property(label = "Root Dir")
	private static final String ROOT_DIR_PROPERTY = "root.dir";

	/* Logger instance. */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Reference
	private ServiceSettingsImpl serviceSettings;

	private String[] paths = null;

	private File rootDir = null;

	@Activate
	protected void activate(final Map<String, Object> props) {
		logger.debug("activate(): props = {}", props);

		this.paths = PropertiesUtil.toStringArray(props.get(PATHS_PROPERTY), new String[0]);
		final String rootDirValue = PropertiesUtil.toString(props.get(ROOT_DIR_PROPERTY), null);

		this.rootDir = new File(rootDirValue);
		generateFiles();

		this.serviceSettings.addSyncRoot(this.rootDir);
	}

	@Deactivate
	protected void deactivate() {
		if (this.rootDir != null) {
			this.serviceSettings.removeSyncRoot(this.rootDir);
		}

		this.paths = null;
		this.rootDir = null;
	}

	private void generateFiles() throws IllegalStateException {
		logger.debug("run()");

		try {
			rootDir.mkdirs();

			/*
			 * FileFilter fileFilter = new NotFileFilter(new
			 * RegexFileFilter("^.vlt-sync.+$")); File[] rootDirContents =
			 * rootDir.listFiles(fileFilter);
			 * 
			 * if (rootDirContents == null || rootDirContents.length == 0) {
			 */
			logger.debug("run(): Generating vlt sync config files at {}", rootDir);
			generateVltSyncConfigFiles();
			/*
			 * } else { if (logger.isDebugEnabled()) {
			 * logger.debug("run(): {} not empty! It contains {}", rootDir,
			 * Arrays.asList(rootDirContents)); } }
			 */

		} catch (IOException e) {
			logger.error("run()", e);
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

		if (foundPaths.isEmpty()) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new FileWriter(new File(this.rootDir, configPropertyFilename)));

				writer.println("disabled=false");
				writer.println("sync-once=JCR2FS");
			} finally {
				IOUtils.closeQuietly(writer);
			}
		} else {
			/*
			 * Don't overwrite the file if it already exists. Allowing the user
			 * to create or change .vlt-sync-config.properties guarantees a
			 * finer control over VTL sync service behavior.
			 */
			if (logger.isDebugEnabled()) {
				logger.debug("generateConfigPropertyFile(): {} already contains {}!", this.rootDir, foundPaths);
			}
		}
	}

	private void generateWorkspaceFilterFile() throws IOException {
		final String workspaceFilterFilename = ".vlt-sync-filter.xml";
		final String defaultWorkspaceFilterFilename = "META-INF/vault/filter.xml";

		final List<String> foundPaths = findRelativePaths(workspaceFilterFilename, defaultWorkspaceFilterFilename,
				"../" + defaultWorkspaceFilterFilename);

		if (foundPaths.isEmpty()) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new FileWriter(new File(rootDir, workspaceFilterFilename)));

				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				writer.println("<workspaceFilter version=\"1.0\">");
				for (String path : this.paths) {
					writer.println("   <filter root=\"" + path + "\"/>");
				}
				writer.println("</workspaceFilter>");
			} finally {
				IOUtils.closeQuietly(writer);
			}
		} else {
			/*
			 * To make this component compatible with other VLT commands, such
			 * as checkout and checkin, we should not create a sync filter if a
			 * default filter already exists!
			 * 
			 * To guarantee a finer control to the user, don't overwrite an
			 * already created/configured file!
			 * 
			 */
			if (logger.isDebugEnabled()) {
				logger.debug("generateWorkspaceFilterFile(): {} already contains {}!", this.rootDir, foundPaths);
			}
		}
	}

	private List<String> findRelativePaths(final String... paths) {
		List<String> foundPaths = new ArrayList<String>();

		for (String path : paths) {
			final File file = new File(this.rootDir, path);
			if (file.exists()) {
				foundPaths.add(path);
			}
		}

		return foundPaths;
	}

}
