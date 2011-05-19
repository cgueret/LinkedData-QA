/**
 * 
 */
package nl.vu.qa_for_lod.report;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vu.qa_for_lod.Graph;
import nl.vu.qa_for_lod.SeedFile;
import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class MetricsContainer {
	private final static DecimalFormat df = new DecimalFormat("###.##");
	protected static Logger logger = LoggerFactory.getLogger(MetricsContainer.class);
	private final Graph graph;
	private final Map<Metric, MetricData> metricsData = new HashMap<Metric, MetricData>();
	private final SeedFile seedFile;

	/**
	 * @param graph
	 * @param seedFile
	 */
	public MetricsContainer(Graph graph, SeedFile seedFile) {
		this.graph = graph;
		this.seedFile = seedFile;
	}

	/**
	 * @param metric
	 */
	public void addMetric(Metric metric) {
		metricsData.put(metric, new MetricData());
	}

	/**
	 * 
	 */
	public void printReport() {
		// Get the list of metrics to execute
		Set<Metric> metrics = metricsData.keySet();

		System.out.println("\n");
		System.out.println("Metric statuses");
		System.out.println("---------------");
		for (Entry<Metric, MetricData> entry : metricsData.entrySet()) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(entry.getKey().getName()).append(" => ");
			buffer.append(entry.getValue().isGreen() ? "green " : "red ").append("(");
			buffer.append("new distance = ");
			buffer.append(df.format(entry.getValue().getRatioDistanceChange()));
			buffer.append(" % of previous)");
			System.out.println(buffer.toString());
		}
		System.out.println("");

		// Compute a ranking of the most suspicious nodes
		System.out.println("Top suspicious nodes");
		System.out.println("--------------------");
		HallOfFame haf = new HallOfFame(10);
		for (Metric metric : metrics)
			haf.insert(metricsData.get(metric).getSuspiciousNodes(haf.getSize(), seedFile.getSeedResources()));
		haf.print();

		// Save the distribution table
		/*
		 * Distributions distributions = new Distributions(); for (Metric metric
		 * : metrics) {
		 * distributions.insert(metric.getDistribution(MetricState.BEFORE),
		 * metric.getName() + "_1");
		 * distributions.insert(metric.getDistribution(MetricState.AFTER),
		 * metric.getName() + "_2"); }
		 * distributions.write("/tmp/distributions.dat");
		 * 
		 * for (Metric metric : metrics)
		 * metric.getDistribution().writeToFile("/tmp/distribution-" +
		 * metric.getName() + ".dat");
		 */

	}

	/**
	 * 
	 */
	public void process() {
		// Get the list of metrics to execute
		Set<Metric> metrics = metricsData.keySet();

		// Clear all possible previous results
		for (Metric metric : metrics)
			metricsData.get(metric).clear();

		// Run the metrics, add the new triples and re-run the metrics
		for (Metric metric : metrics)
			processMetric(graph, metric, MetricState.BEFORE);
		logger.info("Insert new statements");
		graph.addStatements(seedFile.getStatements());
		for (Metric metric : metrics)
			processMetric(graph, metric, MetricState.AFTER);
	}

	/**
	 * @param state
	 */
	public void processMetric(Graph graph, Metric metric, MetricState state) {
		logger.info("Execute metric \"" + metric.getName() + "\"");
		MetricData data = metricsData.get(metric);
		data.setResults(state, metric.getResults(graph));
		Distribution distribution = data.getDistribution(state);
		data.setDistanceToIdeal(state, metric.getDistanceToIdealDistribution(distribution));
	}
}
