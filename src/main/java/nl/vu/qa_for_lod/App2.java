package nl.vu.qa_for_lod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import nl.vu.qa_for_lod.graph.DataAcquisitionTaskFactory;
import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.EndPoint;
import nl.vu.qa_for_lod.graph.impl.DataAcquisitionConfig;
import nl.vu.qa_for_lod.graph.impl.DataAcquisitionTaskFactoryImpl;
import nl.vu.qa_for_lod.graph.impl.DataProviderMap;
import nl.vu.qa_for_lod.graph.impl.DataProviderSubtract;
import nl.vu.qa_for_lod.graph.impl.DataProviderUtils;
import nl.vu.qa_for_lod.graph.impl.DereferencerAny23;
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

import org.aksw.commons.jena.ModelSetView;
import org.aksw.commons.jena.ModelUtils;
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

import com.google.common.io.Files;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

class SampleFile
{
	private File file;
	private Integer sampleSize;
	private String color;
	
	/**
	 * 
	 * 
	 * @param file
	 * @param sampleSize null if not specified
	 */
	public SampleFile(File file, Integer sampleSize, String color) {
		this.file = file;
		this.sampleSize = sampleSize;
		this.color = color;
	}

	public File getFile() {
		return file;
	}

	public Integer getSampleSize() {
		return sampleSize;
	}

	public String getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return "SampleFile [file=" + file + ", sampleSize=" + sampleSize + "]";
	}
}



/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO add a metric to detect when most mappings are 1-1 and some 1-M (then
// they are suspicious)
public class App2 {
	static Logger logger = LoggerFactory.getLogger(App2.class);
	private final DataProviderMap extraTriples;
	private final MetricsExecutor metrics;
	private final DataProvider dataFetcher;
	private static final Options options = new Options();

	
	public static void printHelpAndExit(int exitCode, String errorMessage) {
		if(errorMessage != null) {
			System.err.println(errorMessage);
		}
		
		printHelpAndExit(exitCode);
	}	
	
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
			//initTest(args);
		} catch(Exception e) {
			logger.error("An error occurred", e);
			System.exit(-1);
		}
		
		System.exit(0);
	}
	
	
	/**
	 * Parsing works as follows:
	 * 
	 * Usually we expect pairs of (key, value) corresponding to (sampleSize, fileName).
	 * if interpreting the key as a file points to an existing file, sampleSize is treated as
	 * not specified, and the value of that pair is skipped.
	 *  
	 * color? size? file
	 * #ff0000 150 myfile.nt
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private static List<SampleFile> parseSamples(String[] values) throws Exception
	{		
		List<SampleFile> result = new ArrayList<SampleFile>();
		
		int i = 0;
		String defaultColor = null;
		while(i < values.length) {
			String key = values[i];

			String color = defaultColor;
			// Check if the key is color (starts with #)
			if(key.startsWith("#")) {
				color = key;
				
				if(i++ > values.length) {
					throw new RuntimeException("Missing argument");
				}
				key = values[i];
			}
			
			// Check if the key is size
			Integer sampleSize = null;
			try {
				sampleSize = Integer.parseInt(key);
				if(i++ > values.length) {
					throw new RuntimeException("Missing argument");
				}
				key = values[i];
				
			} catch (Exception e) {
				
			}

			File file = new File(key);
			if(!file.exists()) {
				throw new FileNotFoundException(key);
			}

			result.add(new SampleFile(file, sampleSize, color));
			++i;
		}

		return result;
	}

	/*
	public static Map<String, Model>  prepareSample(List<SampleFile> sampleFiles, Random random)
			throws Exception
	{
		return prepareSample(sampleFiles, ModelFactory.createDefaultModel(), random);
	}*/

	public static <T> void retainRandomSample(List<T> inout, int sampleSize, Random random)
    {
        Collections.shuffle(inout, random);

        inout.subList(Math.min(sampleSize, inout.size()), inout.size()).clear();
    }

	public static Map<String, Model> prepareSample(List<SampleFile> sampleFiles, Model result, Integer max, Random random)
			throws Exception
	{
		Map<String, Model> coloredModels = new HashMap<String, Model>();

		
		for(SampleFile sampleFile : sampleFiles) {
			Model tmp = ModelUtils.read(sampleFile.getFile());

			Model coloredModel = null;
			if(sampleFile.getColor() != null) {
				coloredModel = coloredModels.get(sampleFile.getColor());
				if(coloredModel == null) {
					coloredModel = ModelFactory.createDefaultModel();
					coloredModels.put(sampleFile.getColor(), coloredModel);
				}
			}

			if(sampleFile.getSampleSize() == null) {
				
				result.add(tmp);
				
				if(coloredModel != null) {
					coloredModel.add(tmp);
				}
				
				logger.info("Loaded " + sampleFile.getFile().getAbsolutePath() + " for " + tmp.size() + " triples"); 
			}
			else {
				List<Statement> sample = new ArrayList<Statement>(new ModelSetView(tmp));
				retainRandomSample(sample, sampleFile.getSampleSize(), random);

				logger.info("Sampled " + sampleFile.getFile().getAbsolutePath() + " for " + sample.size() + " triples"); 
				result.add(sample);
				
				if(coloredModel != null) {
					coloredModel.add(sample);
				}
			}
		}
		
		
		if(max != null) {
			List<Statement> stmts = result.listStatements().toList();
			retainRandomSample(stmts, max, random);
			
			// Overwrite our previous result
			//result = ModelFactory.createDefaultModel();
			result.removeAll();
			result.add(stmts);
			
			// Remove unnecessary statements from colored models
			for(Entry<String, Model> entry : coloredModels.entrySet()) {
				StmtIterator it = entry.getValue().listStatements();
				
				while(it.hasNext()) {
					if(!result.contains(it.next())) {
						it.remove();
					}
				}
				it.close();
			}
		}
	
		return coloredModels;
	}
	
	
	
	/**
	 * For the evaluation:
	 * 
	 * Set the random seed
	 * Specify a set of files combined with the sample size
	 * -seed 123456 -f 150 positive.nt -f 150 negative.nt  
	 * 
	 * 
	 * 
	 * @param args
	 * @throws Exception
	 */
	
	@SuppressWarnings("static-access")
	private static void init(String[] args) throws Exception {
		
		// Build the options
		
		
		Option randomSeedOption = OptionBuilder.withArgName("0").hasArg()
				.withDescription("Random seed. Default is 0.").create("seed");
		options.addOption(randomSeedOption);

		Option outDirOption = OptionBuilder.withArgName("directory").hasArg()
				.withDescription("output directory for the results").create("out");
		options.addOption(outDirOption);
		
		Option triplesFileOption = OptionBuilder.withArgName("triples.nt").hasArgs()
				.withDescription("use the given file for the extra triples").create("triples");
		
		options.addOption(triplesFileOption);
		Option resourcesFileOption = OptionBuilder.withArgName("resources.txt").hasArg()
				.withDescription("use the given file for the resources").create("resources");
		options.addOption(resourcesFileOption);
		Option endpointsFileOption = OptionBuilder.withArgName("endpoints.txt").hasArgs()
				.withDescription("use the given file for the end points").create("endpoints");
		options.addOption(endpointsFileOption);
		

		// Maximum can be used to trim a sample created from multiple -triples arguments
		Option maximumTriplesOption = OptionBuilder.withArgName("number").hasArg()
				.withDescription("maximum amount of triples to consider (0 for unlimited)").create("max");
		options.addOption(maximumTriplesOption);

		
		options.addOption("h", false, "print help message");
		options.addOption("nogui", false, "disable the progress bar");
		options.addOption("onlyout", false, "force to use only outgoing triples");

		options.addOption("permissive", false, "ignore unreachable endpoints");
		
		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		// Print help message
		if (cmd.hasOption("h")) {
			printHelpAndExit(0);
		}
		
		// Get the random seed
		long randomSeed = 0;
		String randomSeedStr = cmd.getOptionValue("seed");
		if(randomSeedStr != null) {
			try {
				randomSeed = Long.parseLong(randomSeedStr);
			} catch(Exception e) {
				printHelpAndExit(-1, "Could not parse random seed");
			}
		}

		
		logger.info("Using random seed " + randomSeed);
		
		Random random = new Random(randomSeed);
		

		// Get the name of the data file
		String[] sampleFileStrs = cmd.getOptionValues("triples");
		if (sampleFileStrs == null) {
			System.err.println("You must provide a valid set of triples");
			printHelpAndExit(-1);
		}
		
		List<SampleFile> sampleFiles = parseSamples(sampleFileStrs);
		
		if(sampleFiles.isEmpty()) {
			printHelpAndExit(-1, "No input samples provided");
		}
		
		
/*
		File triplesFile = new File(triplesFilePath);
		if (!triplesFile.exists()) {
			System.err.println("The specified file '" + triplesFile.getAbsolutePath() + "' does not exist.");
			printHelpAndExit(-1);
		}
		String triplesFileName = triplesFile.getName();
*/

		// Get resources file name
		String resourcesFileName = cmd.getOptionValue("resources");
		File resourcesFile = (resourcesFileName == null ? null : new File(resourcesFileName));
		if (resourcesFileName != null && !resourcesFile.exists()) {
			System.err.println("Invalid resources file");
			printHelpAndExit(-1);
		}

		// Get resources file name
		String[] endpointsFileNames = cmd.getOptionValues("endpoints");
		List<File> endpointsFiles = new ArrayList<File>();
		
		if(endpointsFileNames != null) {
			for(String fileName : endpointsFileNames) {
				File file = new File(fileName);
				if(!file.exists()) {
					System.err.println("Invalid endpoints file: " + file.getAbsolutePath());
					printHelpAndExit(-1);					
				}
		
				endpointsFiles.add(file);
			}
		}
		
		
		// Setup the output directory
		String outDirName = cmd.getOptionValue("out");
		if (outDirName == null || outDirName.trim().isEmpty()) {
			//outDirName = "reports/" + triplesFileName;
			throw new RuntimeException("No output directory specified");
		}
		File outputDirectory = new File(outDirName);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		
		

		// Maximum amount of triples
		Integer max = null;
		if (cmd.hasOption("max")) {
			try {
				max = Integer.parseInt(cmd.getOptionValue("max"));
			} catch (Exception e) {
				System.err.println("Invalid maximum amount of triples");
				printHelpAndExit(-1);
			}
		}
		

		// Prepare the sample
		Model sample = ModelFactory.createDefaultModel();
		Map<String, Model> coloredModels = prepareSample(sampleFiles, sample, max, random);
		
		
			
		logger.info("Total sample size: " + sample.size() + " triples remaining");
		
		// For the record: Write the sample into the output dir
		File sampleFile = new File(outDirName + "/sample.nt");
		if(sampleFile.exists()) {
			logger.warn(sampleFile.getAbsolutePath() + " exists. Backing up.");
			
			File backup = new File(sampleFile.getAbsoluteFile() + ".bak");
	
			boolean overwrite = backup.exists();
			Files.copy(sampleFile, backup);

			if(overwrite) {
				logger.warn("Overwrote " + backup.getAbsolutePath());
			}

			//throw new RuntimeException(sampleFile.getAbsolutePath() + " exists. ");
		}
		
		
		boolean permissive = cmd.hasOption("permissive");
		
		FileOutputStream fos = new FileOutputStream(sampleFile);
		
		sample.write(fos, "N-TRIPLES");
		fos.flush();
		fos.close();		
		
		
		// Prepare caching dir
		/*
		File cacheDirectory = new File("cache/" + triplesFileName);
		if (!cacheDirectory.exists())
			cacheDirectory.mkdirs();
		*/

		// Use a GUI ?
		boolean withGUI = !(cmd.hasOption("nogui"));

		// By default, use both in and out triples
		Direction direction = Direction.BOTH;
		if (cmd.hasOption("onlyout"))
			direction = Direction.OUT;

		
		String cacheDirName = "cache";
		
		//direction = Direction.IN; 
		logger.info("DIRECTION: " + direction);
		
		// Start a server that lets us check the status (by default http://localhost:7531/status)
		//Server server = StatusServer.startServer();
		Server server = null;
		
		try {
			// Create, init and run
			App2 app = new App2(sample, resourcesFile, endpointsFiles, cacheDirName, direction, permissive);
			app.process(outputDirectory, withGUI, direction, coloredModels);
			app.close();
		} finally {
			if(server != null) {
				server.stop();
				server.destroy();
			}
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
	public App2(Model sample, File resourcesFile, List<File> endpointsFiles, String cacheDirName, Direction direction, boolean permissive) throws Exception {

		extraTriples = DataProviderUtils.create(sample);


		
		
		long lifespan = 4l * 7l * 24l * 60l * 60l * 1000l; 
		// Set up a cache using a H2 database called 'cache'
		CacheCoreEx cacheBackend = CacheCoreH2.create(cacheDirName, lifespan, true);
		CacheEx cacheFrontend = new CacheExImpl(cacheBackend);


		/* Code for debugging */
		/*
		CacheCoreExCompressor cc = (CacheCoreExCompressor)cacheBackend;
		
		CacheCoreH2 c = ((CacheCoreH2)cc.getDecoratee());
		Iterator<CacheEntryH2> it = c.iterator();
		while(it.hasNext()) {
			CacheEntryH2 ce = (CacheEntryH2)it.next();
			CacheEntry d = cc.wrap(ce);
			
			System.out.println(ce.getQueryHash());
			System.out.println(ce.getQueryString());
			InputStream in = d.getInputStreamProvider().open();
			StreamUtils.copy(in, System.out);
		}
		
		if(true) {
			System.exit(0);
		}
		*/
		
		//System.out.println(StringUtils.md5Hash("http://aksw.org/ontology/WebOfData" + CannedQueryUtils.describe(Node.createURI("http://www.w3.org/2002/07/owl#Thing")).toString()));
		
		/*
		CacheEntry entry = cacheBackend.lookup("http://aksw.org/ontology/WebOfData", CannedQueryUtils.describe(Node.createURI("http://www.w3.org/2002/07/owl#Thing")).toString());
		if(entry != null) {
			InputStream in = entry.getInputStreamProvider().open();
			System.out.println("Content: " + StreamUtils.toString(in));
			in.close();
			entry.getInputStreamProvider().close();
		}
		*/
		
		
		
		List<QueryExecutionFactory<?>> factories = new ArrayList<QueryExecutionFactory<?>>();
		
		
		// Add the end points if they are provided
		//if (endpointsFile != null) {
		for(File endpointsFile : endpointsFiles) {
			BufferedReader reader = new BufferedReader(new FileReader(endpointsFile));
			
			Set<EndPoint> seenEndpoints = new HashSet<EndPoint>(); 
			
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (!line.startsWith("#")) {
					String[] parts = line.split(" ");
					if (parts.length > 0) {
						EndPoint endPoint = new EndPoint(parts[0], (parts.length > 1 ? parts[1] : null));
						
						if(seenEndpoints.contains(endPoint)) {
							logger.info("Skipping because already configued: " + endPoint);
							continue;
						}
						seenEndpoints.add(endPoint);
						
						logger.info("Configuring Endpoint " + endPoint);
						
						Collection<String> defaultGraphs = (endPoint.getGraph() == null)
								? Collections.<String>emptySet()
								: Collections.singleton(endPoint.getGraph());

						try {
							QueryExecutionFactory<?> factory = new QueryExecutionFactoryHttp(endPoint.getURI(), defaultGraphs);
							factory = new QueryExecutionFactoryCacheEx(factory, cacheFrontend);
							factory = new QueryExecutionFactoryPaginated(factory, 10000);

							// The pagination makes use of the cache
							factories.add(factory);
						} catch(Exception e) {
							if(permissive) {
								logger.warn("Skipping endpoint: " + endPoint, e);
							} else {
								throw e;
							}							
						}
						
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
		
		// FIXME Not sure if the DataAcquisitionTask should know about the direction
		// As the dataProvider has an argument for this, maybe he should do the filtering
		DataAcquisitionConfig config = new DataAcquisitionConfig(factories, direction);
		
		DataAcquisitionTaskFactory taskFactory = new DataAcquisitionTaskFactoryImpl(config);
			
		// Create a data fetcher to get data for the resources
		DataProvider dataProvider = new WoDDataProvider2(taskFactory);		
		
		dataFetcher = new DataProviderSubtract(dataProvider, sample);

		/*
		Set<Statement> stmts = dataFetcher.get(ResourceFactory.createResource("http://www.w3.org/2002/07/owl#Thing"), direction);
		System.out.println("Fetched data:");
		for(Statement stmt : stmts) {
			System.out.println(stmt);
		}
		if(true) {
			System.exit(0);
		}*/

		
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
	private void process(File outputDirectory, boolean withGUI, Direction direction, Map<String, Model> coloredModels) throws Exception {
		
		
		// Run all the metrics
		metrics.processQueue(withGUI, direction);
		
		// Generate the analysis report
		logger.info("Save execution report in " + outputDirectory.getAbsolutePath() + "/report.html");
		HTMLReport report = HTMLReport.createReport("dataset", metrics, extraTriples, coloredModels);
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
