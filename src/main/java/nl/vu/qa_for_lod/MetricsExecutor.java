/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.NotFoundException;

import nl.vu.qa_for_lod.metrics.Distribution;
import nl.vu.qa_for_lod.metrics.Metric;
import nl.vu.qa_for_lod.metrics.MetricData;
import nl.vu.qa_for_lod.report.MetricState;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://wiki.gephi.org/index.php/Toolkit_portal
public class MetricsExecutor {

	protected static Logger logger = LoggerFactory.getLogger(MetricsExecutor.class);
	private final Graph graph = new Graph();
	private final Map<Metric, MetricData> metricsData = new HashMap<Metric, MetricData>();
	private final List<Resource> resourceQueue = new ArrayList<Resource>();
	private final ExtraLinks seedFile;

	/**
	 * @param seedFile
	 */
	public MetricsExecutor(ExtraLinks seedFile) {
		this.seedFile = seedFile;
	}

	/**
	 * @param metric
	 */
	public void addMetric(Metric metric) {
		metricsData.put(metric, new MetricData());
	}

	/**
	 * @param resource
	 */
	public void addToResourcesQueue(Resource resource) {
		resourceQueue.add(resource);
	}

	/**
	 * @throws Exception
	 * 
	 */
	// TODO Parallelise this
	public void processQueue() throws Exception {
		logger.info("Process " + resourceQueue.size() + " resources in queue");

		// Clear all previous results
		for (Metric metric : metricsData.keySet())
			metricsData.get(metric).clear();

		// Get executable metrics
		List<Metric> executableMetrics = new ArrayList<Metric>();
		for (Metric metric : metricsData.keySet())
			if (metric.isApplicableFor(graph, resourceQueue))
				executableMetrics.add(metric);

		// Execute the metrics
		for (Resource resource : resourceQueue) {
			try {
				// Create the initial graph and run the metrics
				graph.clear();
				graph.loadFromResource(resource);
				for (Metric metric : executableMetrics)
					metricsData.get(metric).setResult(MetricState.BEFORE, resource, metric.getResult(graph, resource));

				// Create the graph with the new links and re-run the metrics
				graph.clear();
				graph.addStatements(seedFile.getStatements(resource));
				graph.loadFromResource(resource);
				for (Metric metric : executableMetrics)
					metricsData.get(metric).setResult(MetricState.AFTER, resource, metric.getResult(graph, resource));
			} catch (NotFoundException e) {
				// Just skip resources that don't work
			}
		}

		// Close the graph
		graph.close();

		// Do the post processing
		for (Metric metric : executableMetrics) {
			MetricData data = metricsData.get(metric);
			for (MetricState state : MetricState.values()) {
				// Get the distributions
				Distribution observedDistribution = data.getDistribution(state);
				Distribution idealDistribution = metric.getIdealDistribution(observedDistribution);

				// Ask the metric the distance to the ideal value
				data.setDistanceToIdeal(state, observedDistribution.distanceTo(idealDistribution));
			}
		}
	}

	/**
	 * @return
	 */
	public Set<Entry<Metric, MetricData>> metricsData() {
		return metricsData.entrySet();
	}
}
