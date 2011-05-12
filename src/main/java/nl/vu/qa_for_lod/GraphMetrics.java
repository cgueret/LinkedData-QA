/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class GraphMetrics {
	private final Graph graph;
	private final SeedFile seedFile;

	/**
	 * @param graph
	 * @param seedFile
	 */
	public GraphMetrics(Graph graph, SeedFile seedFile) {
		this.graph = graph;
		this.seedFile = seedFile;
	}

	/**
	 * 
	 */
	public void process() {
		// Initialise structures for storing results
		Statistics statistics = new Statistics(seedFile.getSeedResources());
		Distribution distributions = new Distribution();

		// Run all the metrics and save the results in the provided maps
		runMetrics(statistics, distributions);

		// Compare the results before and after
		compareResults(statistics, "centrality");
		compareResults(statistics, "degree");

		// Save the distribution table
		distributions.write("/tmp/distributions.dat");
	}

	/**
	 * @param stats
	 * @param distributions
	 */
	private void compareResults(Statistics statistics, String attribute) {
		// Compare
		Map<String, Double> diffs = new HashMap<String, Double>();
		for (Entry<String, Map<String, Double>> entry : statistics.entrySet()) {
			Double difference = Math.abs(entry.getValue().get(attribute + "_before")
					- entry.getValue().get(attribute + "_after"));
			diffs.put(entry.getKey(), difference);
		}

		// Print the top 10
		List<String> output = new ArrayList<String>();
		Set<Double> keys = new TreeSet<Double>();
		keys.addAll(diffs.values());
		for (Double key : keys)
			for (Entry<String, Double> entry : diffs.entrySet())
				if (entry.getValue().equals(key))
					output.add(entry.getValue() + " " + entry.getKey());
		for (String out : output.subList(output.size() - 10, output.size()))
			System.out.println(out);

	}

	/**
	 * @param stats
	 * @param distributions
	 */
	private void runMetrics(Statistics statistics, Distribution distributions) {
		// Run the metrics and save the results
		Map<String, Double> nodesCentrality = graph.getNodesCentrality();
		statistics.saveResults(nodesCentrality, "centrality_before");
		distributions.saveResults(nodesCentrality, "centrality_before");
		Map<String, Double> nodesDegree = graph.getNodesDegree();
		statistics.saveResults(nodesDegree, "degree_before");
		distributions.saveResults(nodesDegree, "degree_before");

		// Add the new triples
		for (Statement statement : seedFile.getStatements())
			graph.addStatement(statement);

		// Re-run the metrics and save the results
		nodesCentrality = graph.getNodesCentrality();
		statistics.saveResults(nodesCentrality, "centrality_after");
		distributions.saveResults(nodesCentrality, "centrality_after");
		nodesDegree = graph.getNodesDegree();
		statistics.saveResults(nodesDegree, "degree_after");
		distributions.saveResults(nodesDegree, "degree_after");
	}

}
