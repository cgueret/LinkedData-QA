package nl.vu.qa_for_lod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nl.vu.qa_for_lod.graph.DataAcquisitionTaskFactory;
import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.EndPoint;
import nl.vu.qa_for_lod.graph.impl.DataAcquisitionConfig;
import nl.vu.qa_for_lod.graph.impl.DataAcquisitionTaskFactoryImpl;
import nl.vu.qa_for_lod.graph.impl.DataProviderSubtract;
import nl.vu.qa_for_lod.graph.impl.DereferencerAny23;
import nl.vu.qa_for_lod.graph.impl.FileDataProvider;
import nl.vu.qa_for_lod.graph.impl.WoDDataProvider2;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricState;
import nl.vu.qa_for_lod.metrics.impl.Centrality;
import nl.vu.qa_for_lod.metrics.impl.ClusteringCoefficient;
import nl.vu.qa_for_lod.metrics.impl.Degree;
import nl.vu.qa_for_lod.metrics.impl.Richness;
import nl.vu.qa_for_lod.metrics.impl.SameAsChains;
import nl.vu.qa_for_lod.report.DistributionsTable;
import nl.vu.qa_for_lod.report.HTMLReport;

import org.aksw.commons.sparql.api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreEx;
import org.aksw.commons.sparql.api.cache.extra.CacheCoreH2;
import org.aksw.commons.sparql.api.cache.extra.CacheEx;
import org.aksw.commons.sparql.api.cache.extra.CacheExImpl;
import org.aksw.commons.sparql.api.core.QueryExecutionFactory;
import org.aksw.commons.sparql.api.dereference.Dereferencer;
import org.aksw.commons.sparql.api.dereference.QueryExecutionFactoryDereference;
import org.aksw.commons.sparql.api.http.QueryExecutionFactoryHttp;
import org.aksw.commons.sparql.api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.mortbay.jetty.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;



/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO add a metric to detect when most mappings are 1-1 and some 1-M (then
// they are suspicious)
public class App2 {
	static Logger logger = LoggerFactory.getLogger(App2.class);
	private final FileDataProvider extraTriples;
	private final MetricsExecutor metrics;
	private final DataProvider dataFetcher;
	private static final Options options = new Options();

	
	
	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(App2.class.getName(), options);
		System.exit(exitCode);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		try {
			init(args);
		} catch(Exception e) {
			logger.error("An error occurred", e);
			System.exit(-1);
		}
		
		System.exit(0);
	}
	
	@SuppressWarnings("static-access")
	private static void init(String[] args) throws Exception {
		
		// Build the options
		Option outDirOption = OptionBuilder.withArgName("directory").hasArg()
				.withDescription("output directory for the results").create("out");
		options.addOption(outDirOption);
		Option triplesFileOption = OptionBuilder.withArgName("triples.nt").hasArg()
				.withDescription("use the given file for the extra triples").create("triples");
		options.addOption(triplesFileOption);
		Option resourcesFileOption = OptionBuilder.withArgName("resources.txt").hasArg()
				.withDescription("use the given file for the resources").create("resources");
		options.addOption(resourcesFileOption);
		Option endpointsFileOption = OptionBuilder.withArgName("endpoints.txt").hasArg()
				.withDescription("use the given file for the end points").create("endpoints");
		options.addOption(endpointsFileOption);
		Option maximumTriplesOption = OptionBuilder.withArgName("number").hasArg()
				.withDescription("maximum amount of triples to consider (0 for unlimited)").create("max");
		options.addOption(maximumTriplesOption);
		options.addOption("h", false, "print help message");
		options.addOption("nogui", false, "disable the progress bar");
		options.addOption("onlyout", false, "force to use only outgoing triples");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		// Print help message
		if (cmd.hasOption("h")) {
			printHelpAndExit(0);
		}

		// Get the name of the data file
		String triplesFilePath = cmd.getOptionValue("triples");
		if (triplesFilePath == null) {
			System.err.println("You must provide a valid set of triples");
			printHelpAndExit(-1);
		}

		File triplesFile = new File(triplesFilePath);
		if (!triplesFile.exists()) {
			System.err.println("The specified file '" + triplesFile.getAbsolutePath() + "' does not exist.");
			printHelpAndExit(-1);
		}
		String triplesFileName = triplesFile.getName();

		// Get resources file name
		String resourcesFileName = cmd.getOptionValue("resources");
		File resourcesFile = (resourcesFileName == null ? null : new File(resourcesFileName));
		if (resourcesFileName != null && !resourcesFile.exists()) {
			System.err.println("Invalid resources file");
			printHelpAndExit(-1);
		}

		// Get resources file name
		String endpointsFileName = cmd.getOptionValue("endpoints");
		File endpointsFile = (endpointsFileName == null ? null : new File(endpointsFileName));
		if (endpointsFileName != null && !endpointsFile.exists()) {
			System.err.println("Invalid endpoints file");
			printHelpAndExit(-1);
		}

		// Setup the output directory
		String outDirName = cmd.getOptionValue("out");
		if (outDirName == null || outDirName.trim().isEmpty())
			outDirName = "reports/" + triplesFileName;
		File outputDirectory = new File(outDirName);
		if (!outputDirectory.exists())
			outputDirectory.mkdirs();

		// Prepare caching dir
		File cacheDirectory = new File("cache/" + triplesFileName);
		if (!cacheDirectory.exists())
			cacheDirectory.mkdirs();

		// Use a GUI ?
		boolean withGUI = !(cmd.hasOption("nogui"));

		// By default, use both in and out triples
		Direction direction = Direction.BOTH;
		if (cmd.hasOption("onlyout"))
			direction = Direction.OUT;

		// Maximum amount of triples
		int max = 150;
		if (cmd.hasOption("max")) {
			try {
				max = Integer.parseInt(cmd.getOptionValue("max"));
			} catch (Exception e) {
				System.err.println("Invalid maximum amount of triples");
				printHelpAndExit(-1);
			}
		}
		
		logger.info("DIRECTION: " + direction);
		
		// Start a server that lets us check the status (by default http://localhost:7531/status)
		Server server = StatusServer.startServer();
		
		try {
			// Create, init and run
			App2 app = new App2(triplesFile, resourcesFile, endpointsFile, cacheDirectory, max, direction);
			app.process(outputDirectory, withGUI, direction);
			app.close();
		} finally {
			server.stop();
			server.destroy();
		}
	}

	
	
	
	/**
	 * @param triplesFile
	 * @param resourcesFile
	 * @param endpointsFile
	 * @param cacheDirectory
	 * @param max
	 * @throws Exception
	 */
	public App2(File triplesFile, File resourcesFile, File endpointsFile, File cacheDirectory, int max, Direction direction) throws Exception {
		// Load the graph file
		extraTriples = new FileDataProvider(triplesFile.getPath(), max);

		
		
		// Set up a cache using a H2 database called 'cache'
		CacheCoreEx cacheBackend = CacheCoreH2.create("cache", 15000000, true);
		CacheEx cacheFrontend = new CacheExImpl(cacheBackend);


		/* Code for debugging - can be removed
		//CacheCoreExBZip2 cc = (CacheCoreExBZip2)cacheBackend;
		//((CacheCoreH2)cc.getDecoratee()).writeContents(System.out);
		CacheEntry entry = cacheBackend.lookup("WebOfData", CannedQueryUtils.describe(Node.createURI("http://www.w3.org/2002/07/owl#Thing")).toString());
		if(entry != null) {
			InputStream in = entry.getInputStreamProvider().open();
			System.out.println("Content: " + StreamUtils.toString(in));
			in.close();
			entry.getInputStreamProvider().close();
		}
		*/
		
		
		List<QueryExecutionFactory<?>> factories = new ArrayList<QueryExecutionFactory<?>>();
		
		
		// Add the end points if they are provided
		if (endpointsFile != null) {
			BufferedReader reader = new BufferedReader(new FileReader(endpointsFile));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (!line.startsWith("#")) {
					String[] parts = line.split(" ");
					if (parts.length > 0) {
						EndPoint endPoint = new EndPoint(parts[0], (parts.length > 1 ? parts[1] : null));
						
						logger.info("Configuring Endpoint " + endPoint);
						
						Collection<String> defaultGraphs = (endPoint.getGraph() == null)
								? Collections.<String>emptySet()
								: Collections.singleton(endPoint.getGraph());

						QueryExecutionFactory<?> factory = new QueryExecutionFactoryHttp(endPoint.getURI(), defaultGraphs);
						factory = new QueryExecutionFactoryCacheEx(factory, cacheFrontend);
						factory = new QueryExecutionFactoryPaginated(factory, 10000);

						// The pagination makes use of the cache
						
						factories.add(factory);
					}
				}
			}
			reader.close();
		}
		
		
		// The fallback is to dereference data against the WoD itself
		// FIXME Make user-agent configurable
		Dereferencer dereferencer = DereferencerAny23.create("LATC QA tool <cstadler@informatik.uni-leipzig.de>");		
		QueryExecutionFactory<?> factory = new QueryExecutionFactoryDereference(dereferencer);
		factory = new QueryExecutionFactoryCacheEx(factory, cacheFrontend);

		factories.add(factory);
		
		
		DataAcquisitionConfig config = new DataAcquisitionConfig(factories, direction);
		
		DataAcquisitionTaskFactory taskFactory = new DataAcquisitionTaskFactoryImpl(config);
			
		// Create a data fetcher to get data for the resources
		DataProvider dataProvider = new WoDDataProvider2(taskFactory);		
		
		dataFetcher = new DataProviderSubtract(dataProvider, extraTriples.getModel());

		// Create the metrics pipeline
		metrics = new MetricsExecutor(dataFetcher, extraTriples);
		metrics.addMetric(new Degree());
		metrics.addMetric(new ClusteringCoefficient());
		metrics.addMetric(new SameAsChains());
		metrics.addMetric(new Centrality());
		metrics.addMetric(new Richness());

		// Initialise the resource queue
		if (resourcesFile != null) {
			BufferedReader reader = new BufferedReader(new FileReader(resourcesFile));
			for (String line = reader.readLine(); line != null; line = reader.readLine())
				if (!line.startsWith("#"))
					metrics.addToResourcesQueue(ResourceFactory.createResource(line));
			reader.close();
		} else {
			for (Resource resource : extraTriples.getResources())
				metrics.addToResourcesQueue(resource);
		}
	}

	/**
	 * 
	 */
	private void close() {
		// Close the data fetcher
		dataFetcher.close();
	}

	/**
	 * TODO Basically this method should "publish" or "archive" all involed artefacts.
	 * 
	 * @param outputDirectory
	 * @param withGUI
	 * @param direction
	 * @throws Exception
	 */
	private void process(File outputDirectory, boolean withGUI, Direction direction) throws Exception {
		
		
		// Run all the metrics
		metrics.processQueue(withGUI, direction);
		
		// Generate the analysis report
		logger.info("Save execution report in " + outputDirectory + "/report.html");
		HTMLReport report = HTMLReport.createReport("dataset", metrics, extraTriples);
		report.writeTo(outputDirectory + "/report.html");

		// Save the distributions
		for (Metric metric : metrics.getMetrics()) {
			String fileName = outputDirectory + "/" + metric.getName().replace(" ", "_") + ".dat";
			logger.info(String.format("Save distribution for \"%s\" in %s", metric.getName(), fileName));
			DistributionsTable table = new DistributionsTable();
			table.insert(metrics.getMetricData(metric).getDistribution(MetricState.BEFORE), "before");
			table.insert(metrics.getMetricData(metric).getDistribution(MetricState.AFTER), "after");
			table.write(fileName);
		}
	}
}
