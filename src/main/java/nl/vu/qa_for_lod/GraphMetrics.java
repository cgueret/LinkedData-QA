/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	private final Map<String, Map<String, Double>> stats;

	/**
	 * @param graph
	 * @param seedFile
	 */
	public GraphMetrics(Graph graph, SeedFile seedFile) {
		this.graph = graph;
		this.seedFile = seedFile;
		this.stats = new HashMap<String, Map<String, Double>>();
	}

	/**
	 * 
	 */
	public void process() {
		// Initialise the statistics table
		for (Resource resource : seedFile.getSeedResources())
			stats.put(resource.getURI(), new HashMap<String, Double>());

		// Run the metrics and save the results
		Map<String, Double> nodesCentrality = graph.getNodesCentrality();
		saveResults(nodesCentrality, "c1");

		// Add the new triples
		for (Statement statement : seedFile.getStatements())
			graph.addStatement(statement);

		// Re-run the metrics and save the results
		Map<String, Double> nodesCentrality2 = graph.getNodesCentrality();
		saveResults(nodesCentrality2, "c2");

		// Compare
		for (Entry<String, Map<String, Double>> entry : stats.entrySet()) {
			Double d = entry.getValue().get("c2") - entry.getValue().get("c1");
			if (d > 20)
				System.out.println(d + " " + entry.getKey());
		}
	}

	/**
	 * @param results
	 * @param key
	 */
	private void saveResults(Map<String, Double> results, String key) {
		for (Entry<String, Double> result : results.entrySet())
			if (stats.keySet().contains(result.getKey()))
				stats.get(result.getKey()).put(key, result.getValue());
	}

}
