/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.vu.qa_for_lod.data.Distributions;
import nl.vu.qa_for_lod.data.HallOfFame;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.Metric.MetricState;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class GraphMetrics {
	private final Graph graph;
	private final SeedFile seedFile;
	private final List<Metric> metrics = new ArrayList<Metric>();

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
		// Run the metrics, add the new triples and re-run the metrics
		for (Metric metric : metrics)
			metric.processGraph(graph, MetricState.BEFORE);
		graph.addStatements(seedFile.getStatements());
		for (Metric metric : metrics)
			metric.processGraph(graph, MetricState.AFTER);

		// Compute a ranking of the most suspicious nodes
		HallOfFame haf = new HallOfFame(10);
		for (Metric metric : metrics)
			haf.insert(metric.getSuspiciousNodes(haf.getSize(), seedFile.getSeedResources()));
		haf.print();
		
		// Save the distribution table
		Distributions distributions = new Distributions();
		for (Metric metric : metrics) {
			distributions.insert(metric.getDistribution(MetricState.BEFORE), metric.getName() + "_1");
			distributions.insert(metric.getDistribution(MetricState.AFTER), metric.getName() + "_2");
		}
		distributions.write("/tmp/distributions.dat");
	}

	/**
	 * @param metric
	 */
	public void addMetric(Metric metric) {
		metrics.add(metric);
	}

}
