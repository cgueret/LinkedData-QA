package nl.vu.qa_for_lod;

public class App {
	public static void main(String[] args) throws Exception {
		System.out.println("Loading graph...");

		// Create a graph
		Graph graph = new Graph();
		
		// Create a loader
		GraphLoader loader = new GraphLoader(graph);
		
		// Load the seed
		loader.loadSeed("data/links-cut.nt");
		
		// Print stats
		graph.printStats();

		// Expand all resources of the seed
		System.out.println("Expanding graph...");
		loader.expandGraph();
		
		// Print stats
		graph.printStats();
	}
}
