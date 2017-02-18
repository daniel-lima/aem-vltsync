package com.techdm.aem.vltsync.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
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
 * Component to register some folder as a sync folder.
 * 
 * @author Daniel Henrique Alves Lima
 *
 */
@Component(policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = true, immediate = true)
public class RegistrationImpl {

	@Property(label = "Path")
	private static final String PATH_PROPERTY = "path";

	@Property(label = "Root Dir")
	private static final String ROOT_DIR_PROPERTY = "root.dir";

	/* Logger instance. */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Reference
	private ServiceConfigurationImpl serviceConfiguration;

	private String path = null;

	private File rootDir = null;

	@Activate
	protected void activate(final Map<String, Object> props) {
		logger.debug("activate(): props = {}", props);

		this.path = PropertiesUtil.toString(props.get(PATH_PROPERTY), null);
		final String rootDirValue = PropertiesUtil.toString(props.get(ROOT_DIR_PROPERTY), null);

		this.rootDir = new File(rootDirValue);
		generateFiles();

		this.serviceConfiguration.addSyncRoot(this.rootDir);
	}

	@Deactivate
	protected void deactivate() {
		if (this.rootDir != null) {
			this.serviceConfiguration.removeSyncRoot(this.rootDir);
		}

		this.path = null;
		this.rootDir = null;
	}

	private void generateFiles() throws IllegalStateException {
		logger.debug("run()");

		try {
			rootDir.mkdirs();

			FileFilter fileFilter = new NotFileFilter(new RegexFileFilter("^.vlt-sync.+$"));
			File[] rootDirContents = rootDir.listFiles(fileFilter);

			if (rootDirContents == null || rootDirContents.length == 0) {
				logger.debug("run(): Generating vlt sync config files at {}", rootDir);
				generateVltSyncConfigFiles();
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("run(): {} not empty! It contains {}", rootDir, Arrays.asList(rootDirContents));
				}
			}

		} catch (IOException e) {
			logger.error("run()", e);
			throw new IllegalStateException(e);
		}
	}

	private void generateVltSyncConfigFiles() throws IOException {
		generateWorkspaceFilterFile(rootDir, path);
		generateConfigPropertyFile();
	}

	private void generateConfigPropertyFile() throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(rootDir, ".vlt-sync-config.properties")));

			writer.println("disabled=false");
			writer.println("sync-once=JCR2FS");
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void generateWorkspaceFilterFile(final File rootDir, final String path) throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(rootDir, ".vlt-sync-filter.xml")));

			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<workspaceFilter version=\"1.0\">");
			writer.println("<filter root=\"" + path + "\"/>");
			writer.println("</workspaceFilter>");
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

}
