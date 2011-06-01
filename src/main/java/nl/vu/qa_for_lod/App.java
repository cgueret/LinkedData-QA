package nl.vu.qa_for_lod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import nl.vu.qa_for_lod.graph.impl.FileDataProvider;
import nl.vu.qa_for_lod.metrics.impl.ClusteringCoefficient;
import nl.vu.qa_for_lod.metrics.impl.Degree;
import nl.vu.qa_for_lod.metrics.impl.SameAsChains;
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

	/**
	 * @param dataFileName
	 * @param reportFileName
	 * @throws Exception
	 */
	public App(String dataFileName) throws Exception {
		// Check if the file exists
		File file = new File(dataFileName);
		if (!file.exists())
			throw new FileNotFoundException(dataFileName + " can not be found");

		// Load the graph file
		extraTriples = new FileDataProvider(dataFileName);
		logger.info(String.format("Loaded %d triples from %s", extraTriples.size(), dataFileName));

		// Initialise the metrics pipeline
		metrics = new MetricsExecutor(extraTriples);
		metrics.addMetric(new Degree());
		metrics.addMetric(new ClusteringCoefficient());
		metrics.addMetric(new SameAsChains());
	}

	/**
	 * @param reportFileName
	 * @throws Exception
	 */
	private void process(String reportFileName, boolean withGUI) throws Exception {
		// Run all the metrics
		metrics.processQueue(withGUI);

		// Generate the analysis report
		logger.info("Save execution report in " + reportFileName);
		HTMLReport report = HTMLReport.createReport(reportFileName, metrics, extraTriples);
		report.writeTo(reportFileName);
	}

	/**
	 * 
	 */
	private void loadDefaultResourcesQueue() {
		for (Resource resource : extraTriples.getResources())
			metrics.addToResourcesQueue(resource);
	}

	/**
	 * @param resourcesFileName
	 * @throws IOException
	 */
	private void loadResourcesQueue(String resourcesFileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(resourcesFileName));
		for (String line = reader.readLine(); line != null; line = reader.readLine())
			if (!line.startsWith("#"))
				metrics.addToResourcesQueue(ResourceFactory.createResource(line));
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		// Build the options
		Options options = new Options();
		Option reportFile = OptionBuilder.withArgName("report.html").hasArg()
				.withDescription("use the given file for the HTML report").create("report");
		options.addOption(reportFile);
		Option dataFile = OptionBuilder.withArgName("triples.nt").hasArg()
				.withDescription("use the given file for the extra triples").create("triples");
		options.addOption(dataFile);
		Option resourcesFile = OptionBuilder.withArgName("resources.txt").hasArg()
				.withDescription("use the given file for the resources").create("resources");
		options.addOption(resourcesFile);
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

		// Get report file name
		String dataFileName = cmd.getOptionValue("triples");
		if (dataFileName == null) {
			System.err.println("You must provide a set of triples");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(App.class.getName(), options);
			System.exit(-1);
		}

		// Get report name
		String reportFileName = cmd.getOptionValue("report");
		if (reportFileName == null)
			reportFileName = "report.html";

		// Get resources file name
		String resourcesFileName = cmd.getOptionValue("resources");

		// Use a GUI ?
		boolean withGUI = !(cmd.hasOption("nogui"));
		
		// Create, init and run the app
		App app = new App(dataFileName);
		if (resourcesFileName != null)
			app.loadResourcesQueue(resourcesFileName);
		else
			app.loadDefaultResourcesQueue();

		app.process(reportFileName, withGUI);
	}
}
