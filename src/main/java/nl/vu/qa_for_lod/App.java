package nl.vu.qa_for_lod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
	 static Logger logger = LoggerFactory.getLogger(App.class);
	
	public static void main(String[] args) throws Exception {
		// Create a graph
		Graph graph = new Graph();
		
		// Create a loader
		GraphLoader loader = new GraphLoader(graph);
		
		// Load the seed
		loader.loadSeed("data/links-cut.nt");
		logger.info("Initial graph => " + graph.getStats());

		// Expand all resources of the seed
		loader.expandGraph();
		
		graph.dump("/tmp/graph");
	}
}
