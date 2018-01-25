package org.svearike.serviceworker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;

/**
 * This Mojo creates an offline cache service worker in javascript.
 *
 * @goal generate-service-worker
 * @phase prepare-package
 */
public class ServiceWorkerMojo extends AbstractMojo
{
	/**
	 * Location of the generated list of source files.
	 *
	 * @parameter expression="${project.build.directory}/classes/webapp/generated-sw.js"
	 */
	private File serviceWorkerFile;

	/**
	 * Location of the service worker template file.
	 *
	 * @parameter expression="${project.build.directory}/classes/webapp/sw.js"
	 */
	private File serviceWorkerTemplateFile;

	/**
	 * Project the plugin is called from.
	 *
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	/**
	 * Location of the files to cache.
	 *
	 * @parameter expression="${project.build.directory}/classes/webapp"
	 */
	private File sourceDir;

	/**
	 * Defines files in the webapp to include.
	 *
	 * @parameter
	 */
	private String[] includes = {"**/*"};

	/**
	 * Defines which of the included files in the source directories to exclude (non by default).
	 *
	 * @parameter
	 */
	private String[] excludes;

	private enum CacheType
	{
		STATIC
	}

	private static class Resource
	{
		public String path;
		public String hash;

		public Resource(String path, String hash)
		{
			this.path = path;
			this.hash = hash;
		}
	}

	private Map<Resource, CacheType> cachedPaths = new HashMap<Resource, CacheType>();

	/**
	 * Main method executed by maven for this mojo.
	 *
	 * @throws MojoExecutionException propagated.
	 */
	@Override
	public void execute() throws MojoExecutionException
	{
		final Log log = getLog();

		log.info("creating service worker '" + serviceWorkerFile.getAbsolutePath() + "'");

		BufferedWriter writer = null;
		try {
			serviceWorkerFile.getParentFile().mkdirs();
			writer = new BufferedWriter(new FileWriter(serviceWorkerFile));
		} catch (IOException e) {
			throw new MojoExecutionException("could not open source list file '" + serviceWorkerFile + "'", e);
		}
		log.info("resources = " + project.getResources());
		scan(sourceDir, writer);

		try {
			BufferedReader br = new BufferedReader(new FileReader(serviceWorkerTemplateFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("$STATIC_FILES"))
				{
					StringBuilder output = new StringBuilder();
					output.append("{");
					boolean first = true;
					for(Entry<Resource, CacheType> e : cachedPaths.entrySet())
					{
						if (e.getValue() != CacheType.STATIC)
							continue;

						if (!first)
							output.append(",");
						first = false;
						if (e.getKey().path.equals("index.html"))
							output.append("\"/\" : \"" + e.getKey().hash + "\",");
						output.append("\"/" + e.getKey().path + "\" : \"" + e.getKey().hash + "\"");
					}
					output.append("}");
					writer.write(line.replace("$STATIC_FILES", output) + "\n");
				}
				else
					writer.write(line + "\n");
			}
		} catch(Exception e) {
			throw new MojoExecutionException("Could not open service worker template file '" + serviceWorkerTemplateFile + "'", e);
		}

		IOUtil.close(writer);
	}

	/**
	 * Scans a set of directories.
	 *
	 * @param roots Directories to scan
	 * @param writer Where to write the source list
	 * @throws MojoExecutionException propagated.
	 */
	private void scan(List<String> roots, BufferedWriter writer) throws MojoExecutionException {
		for (String root : roots) {
			scan(new File(root), writer);
		}       
	}

	/**
	 * Scans a single directory.
	 *
	 * @param root Directory to scan
	 * @param writer Where to write the source list
	 * @throws MojoExecutionException in case of IO errors
	 */
	private void scan(File root, BufferedWriter writer) throws MojoExecutionException {
		final Log log = getLog();

		if (!root.exists()) {
			return;
		}

		log.info("scanning source file directory '" + root + "'");

		final DirectoryScanner directoryScanner = new DirectoryScanner();
		directoryScanner.setIncludes(includes);
		directoryScanner.setExcludes(excludes);
		directoryScanner.setBasedir(root);
		directoryScanner.scan();

		try {
			for (String fileName : directoryScanner.getIncludedFiles()) {
				cachedPaths.put(new Resource(fileName, calculateHash(new File(root, fileName))), CacheType.STATIC);
			}
		} catch(IOException e) {
			throw new MojoExecutionException("Could not scan files", e);
		}
	}

	private String calculateHash(File file) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(fis);
		fis.close();
		return sha1;
	}
}
