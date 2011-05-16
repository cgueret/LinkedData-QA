package nl.vu.qa_for_lod;

import nl.vu.qa_for_lod.metrics.Centrality;
import nl.vu.qa_for_lod.metrics.Degree;
import nl.vu.qa_for_lod.metrics.Popularity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		// Create a graph and a loader
		Graph graph = new Graph();

		// Load the seed file
		SeedFile seedFile = new SeedFile("data/links-cut.nt");
		logger.info("Number of seeds = " + seedFile.getSeedResources().size());

		// Load the graph around the seed Resources
		graph.loadGraphFromSeeds(seedFile.getSeedResources());
		logger.info("Graph => " + graph.getStats());

		// Dump the graph into external files
		// graph.dump("/tmp/graph");

		// Run the analysis
		GraphMetrics metrics = new GraphMetrics(graph, seedFile);
		metrics.addMetric(new Popularity());
		metrics.addMetric(new Degree());
		metrics.addMetric(new Centrality());
		// TODO add a metric to detect when most mappings are 1-1 and some 1-M
		// (then they are suspicious)

		// Run all the metrics
		metrics.process();
		
		// Print the execution report
		metrics.printReport();
	}
}
