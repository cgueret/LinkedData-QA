package nl.vu.qa_for_lod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import nl.vu.qa_for_lod.graph.impl.FileDataProvider;
import nl.vu.qa_for_lod.graph.impl.WoDDataProvider;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricState;
import nl.vu.qa_for_lod.metrics.impl.Centrality;
import nl.vu.qa_for_lod.metrics.impl.ClusteringCoefficient;
import nl.vu.qa_for_lod.metrics.impl.Degree;
import nl.vu.qa_for_lod.metrics.impl.Richness;
import nl.vu.qa_for_lod.metrics.impl.SameAsChains;
import nl.vu.qa_for_lod.report.DistributionsTable;
import nl.vu.qa_for_lod.report.HTMLReport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
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
public class App {
	static Logger logger = LoggerFactory.getLogger(App.class);
	private final FileDataProvider extraTriples;
	private final MetricsExecutor metrics;
	private final WoDDataProvider dataFetcher;

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		// Build the options
		Options options = new Options();
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
		options.addOption("h", false, "print help message");
		options.addOption("nogui", false, "disable the progress bar");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		// Print help message
		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(App.class.getName(), options);
			System.exit(0);
		}

		// Get the name of the data file
		String triplesFilePath = cmd.getOptionValue("triples");
		File triplesFile = new File(triplesFilePath);
		if (triplesFilePath == null || !triplesFile.exists()) {
			System.err.println("You must provide a valid set of triples");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(App.class.getName(), options);
			System.exit(-1);
		}
		String triplesFileName = triplesFile.getName();

		// Get resources file name
		String resourcesFileName = cmd.getOptionValue("resources");
		File resourcesFile = (resourcesFileName == null ? null : new File(resourcesFileName));
		if (resourcesFileName != null && !resourcesFile.exists()) {
			System.err.println("Invalid resources file");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(App.class.getName(), options);
			System.exit(-1);
		}

		// Get resources file name
		String endpointsFileName = cmd.getOptionValue("endpoints");
		File endpointsFile = (endpointsFileName == null ? null : new File(endpointsFileName));
		if (endpointsFileName != null && !endpointsFile.exists()) {
			System.err.println("Invalid endpoints file");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(App.class.getName(), options);
			System.exit(-1);
		}

		// Setup the output directory
		String outDirName = cmd.getOptionValue("report");
		if (outDirName == null)
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

		// Create, init and run
		App app = new App(triplesFile, resourcesFile, endpointsFile, cacheDirectory);
		app.process(outputDirectory, withGUI);
		app.close();
		System.exit(0);
	}

	/**
	 * @param triplesFile
	 * @param resourcesFile
	 * @param endpointsFile
	 * @param cacheDirectory
	 * @throws Exception
	 */
	public App(File triplesFile, File resourcesFile, File endpointsFile, File cacheDirectory) throws Exception {
		// Load the graph file
		extraTriples = new FileDataProvider(triplesFile.getPath());

		// Create a data fetcher to get data for the resources
		dataFetcher = new WoDDataProvider(cacheDirectory.getPath());

		// Add the end points if they are provided
		if (endpointsFile != null) {
			BufferedReader reader = new BufferedReader(new FileReader(endpointsFile));
			for (String line = reader.readLine(); line != null; line = reader.readLine())
				if (!line.startsWith("#"))
					dataFetcher.addEndPoint(line);
			reader.close();
		}

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
	 * @param outputDirectory
	 * @param withGUI
	 * @throws Exception
	 */
	private void process(File outputDirectory, boolean withGUI) throws Exception {
		// Run all the metrics
		metrics.processQueue(withGUI);

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
