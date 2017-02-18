package com.techdm.aem.vltsync;

import java.util.Arrays;
import java.util.Map;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;

import org.apache.sling.commons.osgi.PropertiesUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with VTL sync integration when necessary.
 */
@Component(policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = true, immediate = true)
public class RegisterImpl {

    @Property(label = "Path")
    private static final String PROP_PATH = "path";
    
    @Property(label = "Root Dir")
    private static final String PROP_ROOT_DIR = "root.dir";

    /* Logger instance. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate(final Map <String, Object> props) {
	logger.debug("activate(): props = {}", props);       

	final String path = PropertiesUtil.toString(props.get(PROP_PATH), null);
	final String rootDirValue = PropertiesUtil.toString(props.get(PROP_ROOT_DIR), null);

	File rootDir = new File(rootDirValue);
	run(rootDir, path);
    }	
    
    private void run(final File rootDir, final String path) throws IllegalStateException {
	logger.debug("run()");

	try {
	    rootDir.mkdirs();
	    
	    FileFilter fileFilter = new NotFileFilter(new RegexFileFilter("^.vlt-sync.+$"));
	    File [] rootDirContents = rootDir.listFiles(fileFilter);
	    
	    if (rootDirContents == null || rootDirContents.length == 0) {
		logger.debug("run(): Generating vlt sync config files at {}", rootDir);
		generateVltSyncConfigFiles(rootDir, path);
	    } else {
		if (logger.isDebugEnabled()) {
		    logger.debug("run(): {} not empty! It contains {}", rootDir,
				 Arrays.asList(rootDirContents));
		}
	    }
	} catch (IOException e) {
	    logger.error("run()", e);
	}
    }

    private void generateVltSyncConfigFiles(final File rootDir, final String path) throws IOException {
	generateWorkspaceFilterFile(rootDir, path);
	generateConfigPropertyFile(rootDir, path);
    }

    private void generateConfigPropertyFile(final File rootDir, final String path) throws IOException {
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
