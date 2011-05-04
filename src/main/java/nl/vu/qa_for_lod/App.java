package nl.vu.qa_for_lod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		// Create a graph and a loader
		Graph graph = new Graph();

		// Load the seed file
		SeedFile seedFile = new SeedFile("data/links-cut.nt");

		// Load the graph around the seed Resources
		graph.loadGraphFromSeeds(seedFile.getSeedResources());
		logger.info("Graph => " + graph.getStats());

		// Dump the graph into external files
		graph.dump("/tmp/graph");

		// Run the analysis
		GraphMetrics metrics = new GraphMetrics(graph, seedFile);
		metrics.process();
	}
}
