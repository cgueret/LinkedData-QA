/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	static Logger logger = LoggerFactory.getLogger(GraphMetrics.class);

	private final Graph graph;
	private final SeedFile seedFile;
	private final List<Metric> metrics = new ArrayList<Metric>();
	private final HallOfFame haf = new HallOfFame(10);

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
		for (Metric metric : metrics) {
			logger.info("Execute metric \"" + metric.getName() + "\"");
			metric.processGraph(graph, MetricState.BEFORE);
		}
		logger.info("Insert new statements");
		graph.addStatements(seedFile.getStatements());
		for (Metric metric : metrics) {
			metric.processGraph(graph, MetricState.AFTER);
			logger.info("Execute metric \"" + metric.getName() + "\"");
		}

		// Compute a ranking of the most suspicious nodes
		for (Metric metric : metrics)
			haf.insert(metric.getSuspiciousNodes(haf.getSize(), seedFile.getSeedResources()));

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

	/**
	 * 
	 */
	public void printReport() {
		System.out.println("Metric statuses");
		System.out.println("---------------");
		for (Metric metric : metrics) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(metric.getName()).append(" : ");
			buffer.append(metric.isGreen() ? "green" : "red");
			System.out.println(buffer.toString());
		}
		System.out.println("");
		System.out.println("Top suspicious nodes");
		System.out.println("--------------------");
		haf.print();
	}

}
