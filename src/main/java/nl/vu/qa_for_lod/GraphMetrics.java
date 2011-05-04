/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.HashMap;
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
/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 *
 */
/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
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
		// Initialise the statistics table
		Map<String, Map<String, Double>> stats = new HashMap<String, Map<String, Double>>();
		for (Resource resource : seedFile.getSeedResources())
			stats.put(resource.getURI(), new HashMap<String, Double>());

		// Initialise the distribution table
		Map<Integer, Map<String, Integer>> distribs = new HashMap<Integer, Map<String, Integer>>();
		for (int key = 0; key < 101; key++)
			distribs.put(key, new HashMap<String, Integer>());

		// Run the metrics and save the results
		Map<String, Double> nodesCentrality = graph.getNodesCentrality();
		saveResults(nodesCentrality, stats, "c1");
		saveDistribution(nodesCentrality, distribs, "closeness_centrality_before");

		// Add the new triples
		for (Statement statement : seedFile.getStatements())
			graph.addStatement(statement);

		// Re-run the metrics and save the results
		Map<String, Double> nodesCentrality2 = graph.getNodesCentrality();
		saveResults(nodesCentrality2, stats, "c2");
		saveDistribution(nodesCentrality2, distribs, "closeness_centrality_after");

		// Compare
		for (Entry<String, Map<String, Double>> entry : stats.entrySet()) {
			Double d = entry.getValue().get("c2") - entry.getValue().get("c1");
			if (d > 0.1)
				System.out.println(d + " " + entry.getKey());
		}

		printDistribution(distribs);
	}

	/**
	 * @param results
	 * @param stats
	 * @param key
	 */
	private void saveResults(Map<String, Double> results, Map<String, Map<String, Double>> stats, String key) {
		for (Entry<String, Double> result : results.entrySet())
			if (stats.keySet().contains(result.getKey()))
				stats.get(result.getKey()).put(key, result.getValue());
	}

	/**
	 * @param results
	 * @param key
	 * @param distribs
	 */
	private void saveDistribution(Map<String, Double> results, Map<Integer, Map<String, Integer>> distribs, String name) {
		// Initialise results
		for (Entry<Integer, Map<String, Integer>> entry : distribs.entrySet())
			entry.getValue().put(name, 0);

		// Find the highest value
		Double max = Double.MIN_VALUE;
		for (Entry<String, Double> result : results.entrySet())
			if (result.getValue() > max)
				max = result.getValue();

		// Fill the distribution table
		for (Entry<String, Double> result : results.entrySet()) {
			int key = (int) (100 * (result.getValue() / max));
			Map<String, Integer> row = distribs.get(key);
			row.put(name, row.get(name) + 1);
		}
	}

	/**
	 * @param distribs
	 * 
	 */
	private void printDistribution(Map<Integer, Map<String, Integer>> distribs) {
		// Get the keys for the rows and columns
		Set<Integer> keys = new TreeSet<Integer>();
		keys.addAll(distribs.keySet());
		Set<String> names = new TreeSet<String>();
		names.addAll(distribs.get(keys.toArray()[0]).keySet());

		// Print the headers
		StringBuffer header = new StringBuffer();
		header.append("# Percent");
		for (String name : names)
			header.append(" ").append(name);
		System.out.println(header.toString());

		// Print the content of the table
		for (Integer key : keys) {
			StringBuffer row = new StringBuffer();
			row.append(key);
			for (String name : names)
				row.append(" ").append(distribs.get(key).get(name));
			System.out.println(row.toString());
		}

	}
}
